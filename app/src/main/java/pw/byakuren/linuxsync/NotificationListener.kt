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
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import pw.byakuren.linuxsync.io.NotificationProto
import pw.byakuren.linuxsync.io.NotificationProto.NotificationData
import pw.byakuren.linuxsync.io.NotificationProto.NotificationData.newBuilder
import pw.byakuren.linuxsync.io.SegmentType
import pw.byakuren.linuxsync.io.ServerSocketThread
import pw.byakuren.linuxsync.ui.ConnectionAcceptDialog
import java.net.BindException
import java.net.SocketException
import javax.crypto.Cipher

class NotificationListener : NotificationListenerService() {

    private val NOTIFICATION_ID: Int = 85094321

    var TAG = "BYAKUREN_NLISTENER"

    private val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding")

    override fun onListenerConnected() {
        super.onListenerConnected()
        MainActivity.notificationListener = this
        Log.d(TAG, "Listener connected")
        createNotificationChannel()

        //send peristent notif
        startForeground(NOTIFICATION_ID, createLinuxSyncNotification("LinuxSync is running"))

        //set up broadcast receivers
        val intentFilter = IntentFilter("android.net.wifi.supplicant.CONNECTION_CHANGE")
        this.registerReceiver(
            NetworkMonitor(
                { startAutoListen() },
                { stopListen("Network Unavailable") }), intentFilter
        )

        val batteryIntentFilter = IntentFilter("android.intent.action.ACTION_BATTERY_LOW")
        batteryIntentFilter.addAction("android.intent.action.ACTION_BATTERY_CHANGED")
        batteryIntentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED")
        batteryIntentFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
        this.registerReceiver(BatteryMonitor(), batteryIntentFilter)

        startAutoListen()
    }

    override fun onNotificationPosted(notif: StatusBarNotification?) {
        if (MainActivity.socketThread == null) {
            Log.d(TAG, "socket doesn't exist, ignoring notification")
            return
        }
        if (notif == null) {
            Log.d(TAG, "notfication is null");
            return
        }
        if (notif.id == NOTIFICATION_ID) {
            Log.v(TAG, "ignoring own persistent notification")
            return
        }
        val protobuf =
            getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE).getBoolean(
                getString(R.string.setting_use_protobuf), true)

        val shouldEncrypt: Boolean = MainActivity.socketThread?.shouldEncrypt()!!

        val data = if (protobuf && !shouldEncrypt)
            byteArrayOf(0x3D) + formatNotificationToProtobufBytes(notif)
        else if (protobuf && shouldEncrypt) {
            cipher.init(Cipher.ENCRYPT_MODE, MainActivity.socketThread!!.getPublicKey()!!)
            byteArrayOf(0x3E) + cipher.doFinal(formatNotificationToProtobufBytes(notif))
        } else
            formatNotificationToPacketBytes(notif)
        try {
            MainActivity.socketThread?.write(data)
        } catch (e: SocketException) {
            Log.e(TAG, "Socket closed when trying to send data")
        } catch (e: Exception) {
            Log.e(TAG, "failed to sent data over socket", e)
        }
    }

    fun formatNotificationToProtobufBytes(notif: StatusBarNotification): ByteArray {
        val bundle = notif.notification?.extras
        var title: String = bundle?.get("android.title").toString()
        var extra: String = bundle?.get("android.text").toString()
        if (title.isEmpty()) {
            title = "<no title>"
        }
        if (extra.isEmpty()) {
            extra = "<no body>"
        }
        val appinfo: ApplicationInfo = bundle?.get("android.appInfo") as ApplicationInfo
        val proto = newBuilder()
            .setTitle(title)
            .setBody(extra)
            .setAppPackage(appinfo.packageName)
            .setId(notif.id)

        if (notif.notification.actions != null) {
            for ((actionIndex, action) in notif.notification.actions.withIndex()) {
                proto.addActions(
                    NotificationData.Action.newBuilder()
                        .setTitle(action.title.toString())
                        .setIndex(actionIndex)
                )
            }
        }
        return proto.build().toByteArray()
    }

    fun formatNotificationToPacketBytes(notif: StatusBarNotification): ByteArray {
        val map = mutableMapOf<SegmentType, String>()
        val data = arrayListOf<Byte>()

        val bundle = notif.notification?.extras
        val title: String = bundle?.get("android.title").toString()
        val extra: String = bundle?.get("android.text").toString()
        val subtext: String = bundle?.get("android.subtext").toString()
        val appinfo: ApplicationInfo = bundle?.get("android.appInfo") as ApplicationInfo
        map.put(SegmentType.Title, title)
        map.put(SegmentType.Body, extra)
        map.put(SegmentType.AppName, appinfo.packageName)
        map.put(SegmentType.NotificationId, notif.id.toString())

        //first byte must be 0x3C, to distinguish garbage
        data.add(0x3C)

        //now all the actions. the action segment has its own format
        var actions = 0
        if (notif.notification.actions != null) {
            actions = notif.notification.actions.size
        }

        //next byte is the number of segments in the packet
        data.add((map.size + actions).toByte())

        //then, fill in all the segments. first the segment header, then length, then data.
        for ((type, str) in map) {
            val bytes = str.toByteArray().toList()
            data.add(type.header)
            data.add(bytes.size.toByte())
            data.addAll(str.toByteArray().toList().subList(0, 127.coerceAtMost(bytes.size)))
        }

        if (notif.notification.actions != null) {
            for ((actionIndex, action) in notif.notification.actions.withIndex()) {
                //put action header
                data.add(SegmentType.Action.header)
                val actionTitle = action.title.toString()
                //next, length of the title. this does not include the last extra byte
                data.add((actionTitle.length).toByte())
                Log.d(TAG, actionTitle.length.toByte().toString())

                //then add the title
                data.addAll(actionTitle.toByteArray().toList())

                //write the index of the action (used when processing it on reply from client)
                data.add(actionIndex.toByte())
            }
        }

        //last byte must be 0x7F
        data.add(0x7F)
        return data.toByteArray()
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
        if (!wifiIsConnected()) {
            Log.d(TAG, "can't bind because network isn't connected")
        }
        try {
            Log.d(TAG, "binding socket")
            MainActivity.socketThread = ServerSocketThread(
                this,
                5000,
                this.baseContext.getSharedPreferences(
                    getString(R.string.prefs_trusted_devices),
                    Context.MODE_PRIVATE
                ),
                { addr -> showAcceptDialog(addr.toString(), addr.hostName) },
                { reason -> updateNotification(reason) }
            )
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
        stopListen("Connections closed")
    }

    fun stopListen(reason: String) {
        updateNotification(reason)
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
        return network != null
    }

    /**
     * Start listening only if auto connect is enabled.
     */
    fun startAutoListen() {
        val settings = getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        if (settings.getBoolean(getString(R.string.setting_automatic_connections), false)) {
            Log.d(TAG, "starting auto listen")
            Thread.sleep(3000) //sleep for a little bit to ensure the network is connected
            startListen()
        }
    }

    private fun createLinuxSyncNotification(content: String): Notification {
        //create intent to open main activity
        val intent = Intent(this.baseContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            this.baseContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, getString(R.string.persistent_channel_id))
            .setContentTitle("LinuxSync")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_menu_send)
            .setTicker("LinuxSync is running")
            .setContentIntent(notifyPendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val notifCompat = NotificationManagerCompat.from(this.baseContext)
        notifCompat.notify(NOTIFICATION_ID, createLinuxSyncNotification(content))
    }
}