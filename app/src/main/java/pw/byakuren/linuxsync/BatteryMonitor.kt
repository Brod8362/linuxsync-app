package pw.byakuren.linuxsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BatteryMonitor: BroadcastReceiver() {

    val TAG = "BYAKUREN_BATMON"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "batmon recv")
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        var extra = "Discharging"
        if (usbCharge) {
            extra = "Charging, USB"
        } else if (acCharge) {
            extra = "Charging, AC"
        }
        val s = "Battery ${batteryPct?.toInt()}% ($extra)"
        Log.d(TAG, s)
        if (context != null) {
            val nbuilder = NotificationCompat.Builder(context, "sync_test_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("LinuxSync")
                .setContentText(s)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(context)) {
                notify(0, nbuilder.build())
            }
        }
    }
}