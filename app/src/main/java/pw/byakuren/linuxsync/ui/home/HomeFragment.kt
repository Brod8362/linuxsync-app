package pw.byakuren.linuxsync.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import pw.byakuren.linuxsync.MainActivity
import pw.byakuren.linuxsync.R
import java.text.SimpleDateFormat
import java.time.Duration
import kotlin.time.hours
import kotlin.time.minutes
import kotlin.time.seconds

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
        return root
    }

    private fun populateConnectionDetails(view: View) {
        val table: TableLayout =  view.findViewById(R.id.connected_info_table)
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
                hostname_textview.text = getString(R.string.connected_hostname, socketThread.connectedHostname())
                val time = socketThread.connectedElapsedTime()
                val time_string = String.format("%d:%02d:%02d", time/3600, (time%3600)/60, time%60)
                time_textview.text = getString(R.string.connected_time_elapsed, time_string)

            } else {
                connected_textview.text = getString(R.string.searching)
                hostname_textview.text = getString(R.string.connected_hostname, "-")
                time_textview.text = getString(R.string.connected_time_elapsed, "-")
            }
        }
    }
}