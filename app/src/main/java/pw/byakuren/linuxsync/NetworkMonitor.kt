package pw.byakuren.linuxsync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NetworkMonitor(val onEnable: ()->Unit, val onDisable: ()->Unit): BroadcastReceiver() {

    private val TAG = "BYAKUREN_NETMON"

    override fun onReceive(context: Context?, intent: Intent?) {
        val conn = intent?.getBooleanExtra("connected", false)
        if (conn!!) {
            Log.d(TAG, "network connection established")
            onEnable.invoke()
        } else {
            Log.d(TAG, "network connection lost")
            onDisable.invoke()
        }
    }
}