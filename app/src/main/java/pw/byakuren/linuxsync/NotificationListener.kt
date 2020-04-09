package pw.byakuren.linuxsync

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pw.byakuren.linuxsync.io.SegmentType
import pw.byakuren.linuxsync.io.ServerSocketThread

class NotificationListener : NotificationListenerService() {

    var socket: ServerSocketThread? = null
    var TAG = "BYAKUREN_NLISTENER"

    override fun onListenerConnected() {
        super.onListenerConnected()
        Toast.makeText(this, "Listener connected", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationPosted(notif: StatusBarNotification?) {
        val map = mutableMapOf<SegmentType, String>()
        val data = arrayListOf<Byte>()

        var bundle = notif?.notification?.extras
        var title: String = bundle?.get("android.title").toString()
        var extra: String = bundle?.get("android.text").toString()
        map.put(SegmentType.Title, title)
        map.put(SegmentType.Body, extra)

        //first byte must be 0x3C, to distinguish garbage
        data.add(0x3C)

        //next byte is the number of segments in the packet
        data.add(map.size.toByte())

        //then, fill in all the segments. first the segment header, then length, then data.
        for ((type, str) in map) {
            val bytes = str.toByteArray().toList()
            data.add(type.header)
            data.add(bytes.size.toByte())
            data.addAll(str.toByteArray().toList().subList(0, 127.coerceAtMost(bytes.size)))
        }

        //last byte must be 0x7F
        data.add(0x7F)

        socket?.write(data.toByteArray())
    }
}