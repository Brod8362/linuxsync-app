package pw.byakuren.linuxsync.ui.home

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import pw.byakuren.linuxsync.MainActivity
import pw.byakuren.linuxsync.R

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        populateConnectionDetails(root)

        val h = Handler()
        val delay: Long = 250
        h.postDelayed(object : Runnable {
            override fun run() {
                populateConnectionDetails(root)
                h.postDelayed(this, delay)
            }
        }, delay)

        return root
    }

    private fun populateConnectionDetails(view: View) {
        val table: TableLayout = view.findViewById(R.id.connected_info_table)
        val connected_textview: TextView = table.findViewById(R.id.text_is_connected)
        val hostname_textview: TextView = table.findViewById(R.id.text_connected_hostname)
        val time_textview: TextView = table.findViewById(R.id.text_connected_time_elapsed)
        val socketThread = MainActivity.socketThread
        if (socketThread == null) {
            connected_textview.text = getString(R.string.disconnected)
            hostname_textview.text = getString(R.string.connected_hostname, "-")
            time_textview.text = getString(R.string.connected_time_elapsed, "-")
        } else {
            if (socketThread.isConnected()) {
                connected_textview.text = getString(R.string.connected)
                hostname_textview.text =
                    getString(R.string.connected_hostname, socketThread.connectedHostname())
                val time = socketThread.connectedElapsedTime()
                val time_string =
                    String.format("%d:%02d:%02d", time / 3600, (time % 3600) / 60, time % 60)
                time_textview.text = getString(R.string.connected_time_elapsed, time_string)

            } else {
                connected_textview.text = getString(R.string.searching)
                hostname_textview.text = getString(R.string.connected_hostname, "-")
                time_textview.text = getString(R.string.connected_time_elapsed, "-")
            }
        }
        val autoconnect: TextView = view.findViewById(R.id.autoconnect_enabled_text)
        val auto_enabled =
            view.context.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
                .getBoolean(getString(R.string.setting_automatic_connections), false)
        autoconnect.text = getString(
            R.string.autoconnect_enabled_text,
            if (auto_enabled) "enabled" else "disabled"
        )
    }
}