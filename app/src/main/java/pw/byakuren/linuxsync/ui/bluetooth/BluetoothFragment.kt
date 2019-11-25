package pw.byakuren.linuxsync.ui.bluetooth

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import pw.byakuren.linuxsync.R

class BluetoothFragment : Fragment() {

    private lateinit var bluetoothViewModel: BluetoothViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bluetoothViewModel =
            ViewModelProviders.of(this).get(BluetoothViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_bluetooth, container, false)
        val textView: TextView = root.findViewById(R.id.text_slideshow)
        bluetoothViewModel.text.observe(this, Observer {
            textView.text = it
        })

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            displayNoBluetoothError()
        }

        return root
    }

    private fun displayNoBluetoothError() {
        val builder: AlertDialog.Builder? = activity?.let {
            AlertDialog.Builder(it)
        }
        builder?.apply {
            setPositiveButton(R.string.ok
            ) { _, _ ->
                System.exit(1);
            }
        }

        builder?.setMessage(R.string.bluetooth_unsupported_message)
            ?.setTitle(R.string.device_unsupported)
        builder?.create()?.show()
    }
}