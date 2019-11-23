package pw.byakuren.linuxsync

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Toast.makeText(this, "Listener connected", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationPosted(notif: StatusBarNotification?) {
        val nbuilder = NotificationCompat.Builder(this, "sync_test_channel")
            .setSmallIcon(R.drawable.ic_menu_send)
            .setContentTitle("Notification listener is working!")
            .setContentText("content")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(this)) {
            notify(0, nbuilder.build())
        }
    }
}