package pw.byakuren.linuxsync.ui.connection

import android.content.Context.MODE_PRIVATE
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import pw.byakuren.linuxsync.MainActivity
import pw.byakuren.linuxsync.R


class ConnectionFragment : Fragment() {

    private lateinit var connectionViewModel: ConnectionViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        connectionViewModel =
            ViewModelProviders.of(this).get(ConnectionViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_connection, container, false)
        val switch: Switch = root.findViewById(R.id.master_toggle)
        val ipView: TextView = root.findViewById(R.id.ip_text)
        ipView.text = getString(R.string.ip_address_text, getIpAddressString())
        if (!affiliatedWithNetwork()) {
            switch.isEnabled = false
            switch.isChecked = false
        } else {
            switch.isChecked = MainActivity.socketThread!=null
        }
        val prefs = root.context.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        val autoSwitch: Switch = root.findViewById(R.id.autoconnect_switch)
        autoSwitch.isChecked = prefs.getBoolean(getString(R.string.setting_automatic_connections), false)
        val protoSwitch: Switch = root.findViewById(R.id.protobuf_switch)
        protoSwitch.isChecked = prefs.getBoolean(getString(R.string.setting_use_protobuf), true)
        return root
    }

    private fun getIpAddressString(): String {
        val wifiManager = context?.applicationContext?.getSystemService(WIFI_SERVICE) as WifiManager
        if (wifiManager.dhcpInfo.ipAddress == 0) return "Not affiliated"
        return Formatter.formatIpAddress(wifiManager.dhcpInfo.ipAddress)
    }

    private fun affiliatedWithNetwork(): Boolean =
        (context?.applicationContext?.getSystemService(WIFI_SERVICE) as WifiManager)
            .dhcpInfo.ipAddress != 0



}