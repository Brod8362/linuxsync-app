package pw.byakuren.linuxsync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pw.byakuren.linuxsync.io.SegmentType
import pw.byakuren.linuxsync.io.ServerSocketThread

class NotificationListener : NotificationListenerService() {

    val NOTIFICATION_ID: Int = 85094321

    var socket: ServerSocketThread? = null
    var TAG = "BYAKUREN_NLISTENER"


    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected")
        createNotificationChannel()

        val notification = Notification.Builder(this, getString(R.string.persistent_channel_id))
            .setContentTitle("LinuxSync")
            .setContentText("LinuxSync is running")
            .setSmallIcon(R.drawable.ic_menu_send)
            .setTicker("LinuxSync is running")
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onNotificationPosted(notif: StatusBarNotification?) {
        if (socket == null) {
            if (MainActivity.socketThread != null) {
                this.socket = MainActivity.socketThread
            } else {
                return
            }
        }
        Log.d(TAG, "Caught notification")
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

        Log.d(TAG, "Crafted notification packet")

        socket?.write(data.toByteArray())
        if (socket == null) {
            Log.d(TAG, "Socket does not exist.")
        }
        Log.d(TAG,"Sent buffer size "+data.size+" over socket")
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.persistent_channel_name)
        val descriptionText = getString(R.string.persistent_channel_name)
        val importance = NotificationManager.IMPORTANCE_LOW
        val id = getString(R.string.persistent_channel_id)
        val channel = NotificationChannel(id, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}