package pw.byakuren.linuxsync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import pw.byakuren.linuxsync.io.ServerSocketThread
import pw.byakuren.linuxsync.ui.ConnectionAcceptDialog
import java.net.BindException

class MainActivity : AppCompatActivity() {

    companion object {
        var socketThread: ServerSocketThread? = null
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    private var socketThread: ServerSocketThread? = null

    private var connectedDevices = 0

    private val TAG = "BYAKUREN_MAIN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        createNotificationChannel()


        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            sendTestNotification(view)
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_test, R.id.nav_connection,
                R.id.nav_tools, R.id.nav_share, R.id.nav_send
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("sync_test_channel", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun startListen(view: View) {
        try {
            socketThread = ServerSocketThread(this, 5000, getPreferences(Context.MODE_PRIVATE)
            ) { addr -> showAcceptDialog(addr.toString(), addr.hostName)}
        } catch (e: BindException) {
            Toast.makeText(this, "Could not make server: is it already running?", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "BindException", e)
            return
        }
        MainActivity.socketThread = this.socketThread
        socketThread?.setConnectCallback { connectedDevices++; updateConnectedView() }
        socketThread?.setDisconnectCallback { connectedDevices--; updateConnectedView() }
        socketThread?.start()
    }

    fun stopListen(view: View) {
        socketThread?.interrupt() //TODO: temporary way to stop socket thread
        socketThread = null
        MainActivity.socketThread=null
    }

    fun writeToSocket(view: View) {
        val text: EditText = findViewById(R.id.send_buffer)
        socketThread?.write((text.text.toString() + "\n").toByteArray())
    }

    fun updateConnectedView() {
//        val view: TextView = findViewById(R.id.connected_counter)
//        view.text = getString(R.string.connected_counter, connectedDevices)
    }

    fun showAcceptDialog(addr: String, hostname: String): Boolean {
        val dialog = ConnectionAcceptDialog(addr, hostname)
        dialog.show(this.supportFragmentManager, "BYAKUREN_DIALOG")
        while (!dialog.completed) {}
        return dialog.res
    }

    fun sendTestNotification(view: View) {
        val nbuilder = NotificationCompat.Builder(view.context, "sync_test_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Title")
            .setContentText("Content")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(view.context)) {
            notify(0, nbuilder.build())
        }
    }
}
