package pw.byakuren.linuxsync.io

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.google.protobuf.InvalidProtocolBufferException
import pw.byakuren.linuxsync.encryption.Authentication
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ServerSocketThread(
    private val context: Context, port: Int, private val sharedPreferences: SharedPreferences,
    private val dialogCallback: ((InetAddress) -> Boolean), private val notificationCallback:((String) -> Unit)
) : Thread() {

    private var serverSocket: ServerSocket = ServerSocket(port)
    private var connectedSocket: Socket? = null
    private var readThread: SocketReadThread? = null
    private var heartbeat: HeartbeatThread? = null
    private var connectCallback: ((ServerSocketThread) -> Unit)? = null
    private var disconnectCallback: (() -> Unit)? = null

    private var connectedHostname: String? = null
    private var connectedTime: LocalDateTime? = null

    private val TAG = "BYAKUREN_SOCKET"

    @ExperimentalStdlibApi
    @SuppressLint("ApplySharedPref") //this ignores the warning on "editor.commit()"
    override fun run() {
        Log.d(TAG, "Waiting for socket connections...")
        val tempSocket = try {
            notificationCallback("Waiting for connection")
            serverSocket.accept()
        } catch (e: SocketException) {
            Log.e(TAG, "socket closed while accept")
            notificationCallback("Socket closed")
            null
        } ?: return //this returns if the obj is null

        val addrString = tempSocket.inetAddress.toString()
        val dataBuffer = ByteArray(1024)
        var bytesRead = 0
        while (bytesRead == 0) {
            bytesRead = tempSocket.getInputStream().read(dataBuffer)
        }
        val authInfo = try {
            Authentication.ClientDetails.parseFrom(dataBuffer.sliceArray(IntRange(0,bytesRead-1)))
        } catch (e: InvalidProtocolBufferException) {
            Log.e(TAG, "invalid protobuf data", e)
            close()
            return
        }

        val hostname = authInfo.hostname
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
            Log.d(TAG, "Accepted socket connection from $addrString ($hostname)")
            write(byteArrayOf(0xAC.toByte()))
            notificationCallback("Connected to $hostname")

            connectedTime = LocalDateTime.now()
            connectedHostname = hostname

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
            Log.d(TAG, "Declined connection from $addrString ($hostname)")
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
            WriteSocket {
                disconnectCallback?.invoke()
            }.execute(Pair(data, connectedSocket?.getOutputStream() as OutputStream))
            Log.v(TAG, "Sent buffer size " + data.size + " over socket")
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
        close()
        sleep(2000)
        connectedSocket?.close()
        try {
            serverSocket.reuseAddress = true
            serverSocket.close()
        } catch (e: Exception) {
            Log.d(TAG,"socket is closed")
        }
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
        return connectedHostname
    }

    fun connectedTime(): LocalDateTime? {
        return connectedTime
    }

    fun connectedElapsedTime(): Long {
        return if (connectedTime == null) {
            0
        } else {
            ChronoUnit.SECONDS.between(connectedTime, LocalDateTime.now())
        }
    }

}

internal class WriteSocket(private val failCallback: () -> Unit) :
    AsyncTask<Pair<ByteArray, OutputStream>, Void, Unit>() {
    override fun doInBackground(vararg params: Pair<ByteArray, OutputStream>?) {
        try {
            params[0]?.second?.write(params[0]?.first as ByteArray)
        } catch (e: Exception) {
            failCallback.invoke()
        }
    }
}


