package pw.byakuren.linuxsync.io

class HeartbeatThread(private val socketThread: ServerSocketThread): Thread() {

    override fun run() {
        while (!socketThread.isClosed()) {
            socketThread.write("_".toByteArray()) //heartbeat, literally just garbage data to refresh the timeout
            sleep(15000)
        }
    }
}