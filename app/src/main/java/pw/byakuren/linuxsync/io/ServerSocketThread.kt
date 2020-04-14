package pw.byakuren.linuxsync.io

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class ServerSocketThread(val context: Context, port: Int) : Thread() {

    private var serverSocket: ServerSocket = ServerSocket(port)
    private var connectedSocket: Socket? = null
    private var readThread: SocketReadThread? = null
    private var connectCallback: ((ServerSocketThread)->Unit)? = null
    private var disconnectCallback: (()->Unit)? = null

    private val TAG = "BYAKUREN_SOCKET"

    override fun run() {
        Log.d(TAG, "Waiting for socket connections...")
        connectedSocket = serverSocket.accept()
        Log.d(TAG, "Accepted socket connection from ${connectedSocket?.inetAddress.toString()}")
        connectCallback?.invoke(this)
        connectedSocket?.getOutputStream()?.write(connect_packet())
        readThread = SocketReadThread(DataInputStream(
            BufferedInputStream(connectedSocket?.getInputStream() as InputStream)))
        readThread?.start()
    }

    fun write(data: ByteArray) {
        try {
            WriteSocket().execute(Pair(data, connectedSocket?.getOutputStream() as OutputStream))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not send data", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Could not send data", e)
        }
    }

    private fun connect_packet(): ByteArray {
        val data = arrayListOf<Byte>()
        data.add(0x3C)
        data.add(0x01)
        data.add(SegmentType.Title.header)
        val str = "Connection established"
        data.add(str.length.toByte())
        data.addAll(str.toByteArray().toList())
        data.add(SegmentType.Body.header)
        data.add(str.length.toByte())
        data.addAll(str.toByteArray().toList())
        data.add(SegmentType.End.header)
        return data.toByteArray()
    }

    override fun interrupt() {
        super.interrupt()
        Log.d(TAG, "Shutdown socket.")
        connectedSocket?.close()
        serverSocket.close()
        readThread?.interrupt()
    }

    fun setConnectCallback(callback: (ServerSocketThread)->Unit) {
        connectCallback=callback
    }

    fun setDisconnectCallback(callback: ()->Unit) {
        disconnectCallback=callback
    }

}

internal class WriteSocket : AsyncTask<Pair<ByteArray, OutputStream>, Void, Unit>() {
    override fun doInBackground(vararg params: Pair<ByteArray, OutputStream>?) {
        params[0]?.second?.write(params[0]?.first as ByteArray)
    }
}


