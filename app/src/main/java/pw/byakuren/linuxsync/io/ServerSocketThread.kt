package pw.byakuren.linuxsync.io

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import androidx.navigation.Navigation.findNavController
import kotlinx.android.synthetic.main.content_main.*
import pw.byakuren.linuxsync.R
import pw.byakuren.linuxsync.ui.ConnectionAcceptDialog
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class ServerSocketThread(val context: Context, port: Int, val dialogCallback: ((InetAddress) -> Boolean)) : Thread() {

    private var serverSocket: ServerSocket = ServerSocket(port)
    private var connectedSocket: Socket? = null
    private var readThread: SocketReadThread? = null
    private var connectCallback: ((ServerSocketThread)->Unit)? = null
    private var disconnectCallback: (()->Unit)? = null


    private val TAG = "BYAKUREN_SOCKET"

    override fun run() {
        Log.d(TAG, "Waiting for socket connections...")
        val tempSocket = serverSocket.accept()
        if (dialogCallback.invoke(tempSocket.inetAddress)) {
            connectedSocket = tempSocket
            Log.d(TAG, "Accepted socket connection from ${connectedSocket?.inetAddress.toString()}")
            connectCallback?.invoke(this)
            readThread = SocketReadThread(DataInputStream(
                BufferedInputStream(connectedSocket?.getInputStream() as InputStream)))
            readThread?.start()
        } else {
            //declined connection
            Log.d(TAG, "Declined connection from ${tempSocket?.inetAddress.toString()}")
            connectedSocket?.shutdownInput()
            connectedSocket?.shutdownOutput()
            connectedSocket?.close()
            connectedSocket = null
        }
    }

    fun write(data: ByteArray) {
        try {
            WriteSocket().execute(Pair(data, connectedSocket?.getOutputStream() as OutputStream))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not send data", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Could not send data", e)
        }
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


