package pw.byakuren.linuxsync.io

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ServerSocketThread(
    val context: Context, port: Int, val sharedPreferences: SharedPreferences,
    val dialogCallback: ((InetAddress) -> Boolean)
) : Thread() {

    private var serverSocket: ServerSocket = ServerSocket(port)
    private var connectedSocket: Socket? = null
    private var readThread: SocketReadThread? = null
    private var heartbeat: HeartbeatThread? = null
    private var connectCallback: ((ServerSocketThread) -> Unit)? = null
    private var disconnectCallback: (() -> Unit)? = null
    private var connectedTime: LocalDateTime? = null


    private val TAG = "BYAKUREN_SOCKET"

    @SuppressLint("ApplySharedPref") //this ignores the warning on "editor.commit()"
    override fun run() {
        Log.d(TAG, "Waiting for socket connections...")
        val tempSocket = try {
            serverSocket.accept()
        } catch (e: SocketException) {
            Log.e(TAG, "socket closed while accept")
            null
        } ?: return //this returns if the obj is null

        val addrString = tempSocket.inetAddress.toString()
        if (sharedPreferences.contains(addrString) ||
            dialogCallback.invoke(tempSocket.inetAddress)
        ) {
            if (!sharedPreferences.contains(addrString)) {
                val editor = sharedPreferences.edit()
                editor.putString(addrString, LocalDateTime.now().toString())
                editor.commit()
                Log.d(TAG, "Added " + tempSocket.inetAddress.toString() + " to trusted addresses")
            }

            connectedSocket = tempSocket
            Log.d(TAG, "Accepted socket connection from ${addrString}")
            connectedTime = LocalDateTime.now()
            connectCallback?.invoke(this)
            connectedSocket?.soTimeout = 20000
            readThread = SocketReadThread(
                DataInputStream(
                    BufferedInputStream(connectedSocket?.getInputStream() as InputStream)
                )
            )
            readThread?.start()
            heartbeat = HeartbeatThread(this)
            heartbeat?.start()

        } else {
            //declined connection
            Log.d(TAG, "Declined connection from ${addrString}")
            connectedSocket = tempSocket
            close()
            sleep(3000)
            connectedSocket?.close()
            connectedSocket = null
            heartbeat = null
        }
    }

    fun write(data: ByteArray) {
        try {
            WriteSocket().execute(Pair(data, connectedSocket?.getOutputStream() as OutputStream))
        } catch (e: TypeCastException) {
            Log.e(TAG, "failed to send notification, socket is probably not connected")
        } catch (e: Exception) {
            Toast.makeText(context, "Could not send data", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Could not send data", e)
        }
    }


    fun close() {
        write(byteArrayOf(0x7F, 0x7F))
    }

    override fun interrupt() {
        super.interrupt()
        Log.d(TAG, "Shutdown socket.")
        connectedSocket?.close()
        serverSocket.reuseAddress = true
        serverSocket.close()
        readThread?.interrupt()
    }

    fun setConnectCallback(callback: (ServerSocketThread) -> Unit) {
        connectCallback = callback
    }

    fun setDisconnectCallback(callback: () -> Unit) {
        disconnectCallback = callback
    }

    fun isClosed(): Boolean {
        return serverSocket.isClosed
    }

    fun isConnected(): Boolean {
        return connectedSocket != null && connectedSocket!!.isConnected
    }

    fun connectedHostname(): String? {
        if (connectedSocket == null) {
            return null;
        } else {
            return connectedSocket!!.inetAddress.canonicalHostName
        }
    }

    fun connectedTime(): LocalDateTime? {
        return connectedTime
    }

    fun connectedElapsedTime(): Int {
        if (connectedTime == null) {
            return 0;
        } else {
            return ChronoUnit.SECONDS.between(LocalDateTime.now(), connectedTime).toInt()
        }
    }

}

internal class WriteSocket : AsyncTask<Pair<ByteArray, OutputStream>, Void, Unit>() {
    override fun doInBackground(vararg params: Pair<ByteArray, OutputStream>?) {
        params[0]?.second?.write(params[0]?.first as ByteArray)
    }
}


