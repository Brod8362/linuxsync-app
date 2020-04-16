package pw.byakuren.linuxsync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pw.byakuren.linuxsync.io.SegmentType
import pw.byakuren.linuxsync.io.ServerSocketThread
import pw.byakuren.linuxsync.ui.ConnectionAcceptDialog
import java.net.BindException
import java.net.SocketException

class NotificationListener : NotificationListenerService() {

    val NOTIFICATION_ID: Int = 85094321

    var TAG = "BYAKUREN_NLISTENER"


    override fun onListenerConnected() {
        super.onListenerConnected()
        MainActivity.notificationListener = this
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
        if (MainActivity.socketThread == null) {
            return
        }
        Log.d(TAG, "Caught notification")
        val map = mutableMapOf<SegmentType, String>()
        val data = arrayListOf<Byte>()

        val bundle = notif?.notification?.extras
        val title: String = bundle?.get("android.title").toString()
        val extra: String = bundle?.get("android.text").toString()
        val subtext: String = bundle?.get("android.subtext").toString()
        val appinfo: ApplicationInfo = bundle?.get("android.appInfo") as ApplicationInfo
        map.put(SegmentType.Title, title)
        map.put(SegmentType.Body, extra)
        map.put(SegmentType.AppName, appinfo.packageName)

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
        try {
            MainActivity.socketThread?.write(data.toByteArray())
            Log.d(TAG, "Sent buffer size " + data.size + " over socket")
        } catch (e: SocketException) {
            Log.e(TAG, "Socket closed when trying to send data")
        }


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

    fun startListen() {
        try {
            MainActivity.socketThread = ServerSocketThread(
                this, 5000, this.baseContext.getSharedPreferences("trusted_devices", Context.MODE_PRIVATE)
            ) { addr -> showAcceptDialog(addr.toString(), addr.hostName) }
            MainActivity.socketThread!!.setDisconnectCallback { stopListen(); startListen() }
        } catch (e: BindException) {
            Toast.makeText(this, "Could not make server: is it already running?", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "BindException", e)
            return
        }
        MainActivity.socketThread?.start()
    }

    fun stopListen() {
        MainActivity.socketThread?.interrupt() //TODO: temporary way to stop socket thread
        Thread.sleep(5000) //wait to ensure the socket is fully closed and done
        MainActivity.socketThread = null
    }

    fun showAcceptDialog(addr: String, hostname: String): Boolean {
        val dialog = ConnectionAcceptDialog(addr, hostname)
        MainActivity.fragmentManager?.let { dialog.show(it, "BYAKUREN_DIALOG") }
        while (!dialog.completed) { /* just waiting for it to complete */ }
        return dialog.res
    }

}