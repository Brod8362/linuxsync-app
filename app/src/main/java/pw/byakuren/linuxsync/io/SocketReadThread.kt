package pw.byakuren.linuxsync.io

import android.util.Log
import android.widget.Toast
import java.io.DataInputStream
import java.net.SocketException
import java.net.SocketTimeoutException

class SocketReadThread(private val stream: DataInputStream) : Thread() {

    private val TAG = "BYAKUREN_READ_THREAD"

    private val buffer: ByteArray = ByteArray(128)

    @ExperimentalStdlibApi
    override fun run() {
        var len: Int
        do {
            len = try {
                stream.read(buffer)
            } catch (e: SocketException){
                Log.d(TAG, "failed on read buffer, most likely socket closed")
                0
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "socket timed out")
                0
            }
        } while (len > 0)
    }

    override fun interrupt() {
        stream.close()
        super.interrupt()
    }
}