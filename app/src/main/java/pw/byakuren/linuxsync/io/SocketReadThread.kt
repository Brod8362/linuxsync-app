package pw.byakuren.linuxsync.io

import android.util.Log
import java.io.DataInputStream

class SocketReadThread(val stream: DataInputStream) : Thread() {

    private val TAG = "BYAKUREN_READ_THREAD"

    private val buffer: ByteArray = ByteArray(1024)

    @ExperimentalStdlibApi
    override fun run() {

        var run = true
        while (run && stream.read(buffer) != 0 ) {
            Log.d(TAG, buffer.decodeToString().trim())
        }

    }

    override fun interrupt() {
        stream.close()
        super.interrupt()
    }
}