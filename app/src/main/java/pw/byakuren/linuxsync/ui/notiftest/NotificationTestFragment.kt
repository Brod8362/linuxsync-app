package pw.byakuren.linuxsync.ui.notiftest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import pw.byakuren.linuxsync.R

class NotificationTestFragment : Fragment() {

    private lateinit var notificationTestViewModel: NotificationTestViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notificationTestViewModel =
            ViewModelProviders.of(this).get(NotificationTestViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_test, container, false)
        val textView: TextView = root.findViewById(R.id.text_notiftest)
        notificationTestViewModel.text.observe(this, Observer {
            textView.text = it
        })
        val button: Button = root.findViewById(R.id.button)
        button.setOnClickListener { sendTestNotification(button) }
        return root
    }

    fun sendTestNotification(view: View) {
        val nbuilder = NotificationCompat.Builder(view.context, "sync_test_channel")
            .setSmallIcon(R.drawable.ic_menu_send)
            .setContentTitle("Test notification!")
            .setContentText("content")
            .setPriority(NotificationCompat.PRIORITY_LOW)
        with(NotificationManagerCompat.from(view.context)) {
            notify(0, nbuilder.build())
        }
    }
}