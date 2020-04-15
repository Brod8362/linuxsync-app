package pw.byakuren.linuxsync.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import pw.byakuren.linuxsync.R
import java.lang.IllegalStateException
import java.net.InetAddress

class ConnectionAcceptDialog(val addrString: String, val hostname: String) : DialogFragment() {

    var res = false
    var completed = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val msg = getString(R.string.accept_connection_message, addrString, hostname)
        return activity?.let {
            val builder = AlertDialog.Builder(it)
                .setMessage(msg)
                .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
                    res = true
                    completed = true
                })
                .setNegativeButton("No", DialogInterface.OnClickListener { dialog, id ->
                    completed = true
                })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}