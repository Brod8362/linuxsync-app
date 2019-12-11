package pw.byakuren.linuxsync.ui.connection

import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
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
        val ipView: TextView = root.findViewById(R.id.ipview)
        val ipText = getIpAddressString()
        ipView.text = getString(R.string.ip_address_text, ipText)


        return root
    }

    private fun setIpView() {

    }

    private fun getIpAddressString(): String {
        val wifiManager = context?.applicationContext?.getSystemService(WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wifiManager.dhcpInfo.ipAddress)
    }



}