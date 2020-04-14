package pw.byakuren.linuxsync.io

import java.io.DataInputStream

class SocketReadThread(val stream: DataInputStream) : Thread() {

    private val TAG = "BYAKUREN_READ_THREAD"

    private val buffer: ByteArray = ByteArray(128)

    @ExperimentalStdlibApi
    override fun run() {
        var len: Int
        do {
            len = stream.read(buffer)
        } while (len > 0)
    }

    override fun interrupt() {
        stream.close()
        super.interrupt()
    }
}