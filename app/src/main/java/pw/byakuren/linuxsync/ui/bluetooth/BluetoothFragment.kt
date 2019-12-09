package pw.byakuren.linuxsync.ui.bluetooth

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import pw.byakuren.linuxsync.R
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

class BluetoothFragment : Fragment() {

    private val TRUSTED_ADDRESS = "5C:E0:C5:48:31:8A"

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
        /* Retrieve bluetooth adapter */
        val manAny: Any? = this.context?.getSystemService(Context.BLUETOOTH_SERVICE)
        val bluetoothManager: BluetoothManager?
        if (manAny is BluetoothManager) {
            bluetoothManager = manAny
        } else {
            displayNoBluetoothError()
            return root
        }
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        /* Verify device supports bluetooth, if not display error and exit */
        if (bluetoothAdapter == null) {
            displayNoBluetoothError()
        }

        /* Ensure bluetooth is enabled */
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val REQUEST_ENABLE_BT = 1 // i have no idea what this does
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        /* Retrieve already paired devices. */
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        initBluetooth(bluetoothAdapter, pairedDevices)

        return root
    }

    private fun displayNoBluetoothError() {
        val builder: AlertDialog.Builder? = activity?.let {
            AlertDialog.Builder(it)
        }
        builder?.apply {
            setPositiveButton(R.string.ok
            ) { _, _ ->
                exitProcess(1)
            }
        }

        builder?.setMessage(R.string.bluetooth_unsupported_message)
            ?.setTitle(R.string.device_unsupported)
        builder?.create()?.show()
    }

    private fun getTrustedBluetoothDevice(paired: Set<BluetoothDevice>?): BluetoothDevice? {
        paired?.forEach { d -> if (d.address == TRUSTED_ADDRESS) return d }
        return null
    }

    private fun initBluetooth(bt: BluetoothAdapter?, paired: Set<BluetoothDevice>?): BluetoothSocket? {
        bt?.cancelDiscovery()
        val device = getTrustedBluetoothDevice(paired)
        val socket = device?.createL2capChannel(0x1001)
        try {
            socket?.connect()
        } catch (e: IOException) {
            Toast.makeText(this.context, "Failed to connect to bluetooth socket", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
        Toast.makeText(this.context, "Connected to bluetooth socket", Toast.LENGTH_LONG).show()
        return socket
        // consider using code from https://developer.android.com/guide/topics/connectivity/bluetooth#ConnectDevices here
    }
}