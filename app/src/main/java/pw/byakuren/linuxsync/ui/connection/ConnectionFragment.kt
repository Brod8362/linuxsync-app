package pw.byakuren.linuxsync.ui.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
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
        val textView: TextView = root.findViewById(R.id.text_slideshow)
        connectionViewModel.text.observe(this, Observer {
            textView.text = it
        })


        return root
    }
}