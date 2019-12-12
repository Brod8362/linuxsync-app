package pw.byakuren.linuxsync.io

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket

class ServerSocketThread(context: Context, port: Int) : Thread() {

    private var serverSocket: ServerSocket = ServerSocket(port)
    private var connectedSocket: Socket? = null
    private var readThread: SocketReadThread? = null
    private var connectCallback: (()->Unit)? = null
    private var disconnectCallback: (()->Unit)? = null

    private val TAG = "BYAKUREN_SOCKET"

    override fun run() {
        Log.d(TAG, "Waiting for socket connections...")
        connectedSocket = serverSocket.accept()
        Log.d(TAG, "Accepted socket connection from ${connectedSocket?.inetAddress.toString()}")
        connectCallback?.invoke()
        connectedSocket?.getOutputStream()?.write("Hello!".toByteArray())
        readThread = SocketReadThread(DataInputStream(
            BufferedInputStream(connectedSocket?.getInputStream() as InputStream)))
        readThread?.start()
    }

    override fun interrupt() {
        super.interrupt()
        Log.d(TAG, "Shutdown socket.")
        connectedSocket?.close()
        serverSocket.close()
        readThread?.interrupt()
    }

    fun setConnectCallback(callback: ()->Unit) {
        connectCallback=callback
    }

    fun setDisconnectCallback(callback: ()->Unit) {
        disconnectCallback=callback
    }

}