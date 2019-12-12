package pw.byakuren.linuxsync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import pw.byakuren.linuxsync.io.ServerSocketThread
import java.net.BindException

class MainActivity : AppCompatActivity() {

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
        val listener = NotificationListener()

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    fun startListen(view: View) {
        Toast.makeText(this, "start", Toast.LENGTH_SHORT).show()
        try {
            socketThread = ServerSocketThread(this, 5000)
        } catch (e: BindException) {
            Toast.makeText(this, "Could not make server: is it already running?", Toast.LENGTH_LONG).show()
            Log.e(TAG, "BindException", e)
            return
        }
        socketThread?.setConnectCallback { connectedDevices++; updateConnectedView() }
        socketThread?.setDisconnectCallback { connectedDevices--; updateConnectedView() }
        socketThread?.start()
    }

    fun stopListen(view: View) {
        Toast.makeText(this, "stop", Toast.LENGTH_SHORT).show()
        socketThread?.interrupt() //TODO: temporary way to stop socket thread
    }

    fun writeToSocket(view: View) {
        val text: EditText = findViewById(R.id.send_buffer)
        socketThread?.write((text.text.toString()+"\n").toByteArray())
    }

    fun updateConnectedView() {
//        val view: TextView = findViewById(R.id.connected_counter)
//        view.text = getString(R.string.connected_counter, connectedDevices)
    }


}
