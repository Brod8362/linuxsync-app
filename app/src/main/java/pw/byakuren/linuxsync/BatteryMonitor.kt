package pw.byakuren.linuxsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BatteryMonitor : BroadcastReceiver() {

    val TAG = "BYAKUREN_BATMON"
    val NOTIFICATION_ID = 90348510

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "batmon recv")
        if (context == null) return;
        val prefs =
            context.getSharedPreferences(context.getString(R.string.prefs_settings), MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(context.getString(R.string.setting_battery_updates), true)
        if (!isEnabled) return

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        val wirelessCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        val extra = when {
            usbCharge -> {
                "Charging, USB"
            }
            acCharge -> {
                "Charging, AC"
            }
            wirelessCharge -> {
                "Charging, Wireless"
            }
            else -> {
                "Discharging"
            }
        }
        val s = "Battery ${batteryPct?.toInt()}% ($extra)"

        val nbuilder = NotificationCompat.Builder(context, "sync_test_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("LinuxSync")
            .setContentText(s)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, nbuilder.build())
        }
        //send a notification and then immediately cancel it so the user doesn't see the annoying popup
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}