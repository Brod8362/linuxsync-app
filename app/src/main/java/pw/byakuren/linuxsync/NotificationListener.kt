package pw.byakuren.linuxsync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
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

    private val NOTIFICATION_ID: Int = 85094321

    var TAG = "BYAKUREN_NLISTENER"


    override fun onListenerConnected() {
        super.onListenerConnected()
        MainActivity.notificationListener = this
        Log.d(TAG, "Listener connected")
        createNotificationChannel()

        //create intent to open main activity
        val intent = Intent(this.baseContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(this.baseContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        //send peristent notif
        val notification = Notification.Builder(this, getString(R.string.persistent_channel_id))
            .setContentTitle("LinuxSync")
            .setContentText("LinuxSync is running")
            .setSmallIcon(R.drawable.ic_menu_send)
            .setTicker("LinuxSync is running")
            .setContentIntent(notifyPendingIntent)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        //set up broadcast receivers
        val intentFilter = IntentFilter("android.net.wifi.supplicant.CONNECTION_CHANGE")
        this.registerReceiver(NetworkMonitor({startAutoListen()}, {stopListen()}), intentFilter)

        val batteryIntentFilter = IntentFilter("android.intent.action.ACTION_BATTERY_LOW")
        batteryIntentFilter.addAction("android.intent.action.ACTION_BATTERY_CHANGED")
        batteryIntentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED")
        batteryIntentFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
        this.registerReceiver(BatteryMonitor(), batteryIntentFilter)

        startAutoListen()
    }

    override fun onNotificationPosted(notif: StatusBarNotification?) {
        if (MainActivity.socketThread == null) {
            Log.d(TAG,"network not connected, won't start socket")
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
        if (!wifiIsConnected()) return;
        try {
            MainActivity.socketThread = ServerSocketThread(
                this, 5000, this.baseContext.getSharedPreferences(
                    getString(R.string.prefs_trusted_devices),
                    Context.MODE_PRIVATE
                )
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
        MainActivity.socketThread = null
    }

    fun showAcceptDialog(addr: String, hostname: String): Boolean {
        val dialog = ConnectionAcceptDialog(addr, hostname)
        MainActivity.fragmentManager?.let { dialog.show(it, "BYAKUREN_DIALOG") }
        while (!dialog.completed) { /* just waiting for it to complete */
        }
        return dialog.res
    }

    fun wifiIsConnected(): Boolean {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connManager.activeNetwork
        return network!=null
    }

    /**
     * Start listening only if auto connect is enabled.
     */
    fun startAutoListen() {
        val settings = getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        if (settings.getBoolean(getString(R.string.setting_automatic_connections), false)) {
            startListen()
        }
    }
}