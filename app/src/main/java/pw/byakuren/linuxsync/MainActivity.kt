package pw.byakuren.linuxsync

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
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
        var notificationListener: NotificationListener? = null
        var fragmentManager: FragmentManager? = null
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    private val TAG = "BYAKUREN_MAIN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        createNotificationChannel()
        MainActivity.fragmentManager = this.supportFragmentManager


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
                R.id.nav_home, R.id.nav_connection,
                R.id.nav_tools, R.id.nav_share, R.id.nav_send
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (!Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners")
                .contains(applicationContext.packageName)
        ) {
            //check if has permission to use notification listener
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("You need to give the application notification permission.")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    //pass in a random view because, bad code but w/e
                   openNotificationSettings(navView)
                }
            val alert: AlertDialog = builder.create()
            alert.show()
        }

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

    fun masterSwitchToggle(view: View) {
        val switch = view as Switch
        if (switch.isChecked) {
            startListen()
        } else {
            stopListen()
        }
    }

    fun automaticSwitchToggle(view: View) {
        val switch = view as Switch
        val prefs = getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(getString(R.string.setting_automatic_connections), switch.isChecked)
        editor.apply()
    }

    fun protobufSwitchToggle(view: View) {
        val switch = view as Switch
        val prefs = getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(getString(R.string.setting_use_protobuf), switch.isChecked)
        editor.apply()
    }

    fun batteryStatusSwitchToggle(view: View) {
        val switch = view as Switch
        val prefs = getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(getString(R.string.setting_battery_updates), switch.isChecked)
        editor.apply()
    }

    fun startListen() {
        Log.d(TAG, "Enable client connections")
        if (notificationListener != null) {
            notificationListener?.startListen()
        } else {
            Toast.makeText(this, "You need to give the notification permission.", Toast.LENGTH_LONG).show()
        }
    }

    fun stopListen() {
        Log.d(TAG, "Disable client connections")
        notificationListener?.stopListen()
    }

    fun openNotificationSettings(view: View) {
        applicationContext.startActivity(
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    fun updateConnectedView() {
//        val view: TextView = findViewById(R.id.connected_counter)
//        view.text = getString(R.string.connected_counter, connectedDevices)
    }

    fun showAcceptDialog(addr: String, hostname: String): Boolean {
        val dialog = ConnectionAcceptDialog(addr, hostname)
        dialog.show(this.supportFragmentManager, "BYAKUREN_DIALOG")
        while (!dialog.completed) {
        }
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

    fun sendTestNotificationWithActions(view: View) {
        val actionA = NotificationCompat.Action(0, "Action A", null);
        val actionB = NotificationCompat.Action(0, "Action B", null);
        val nbuilder = NotificationCompat.Builder(view.context, "sync_test_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Actions")
            .setContentText("Content and some actions")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(actionA)
            .addAction(actionB)
        with(NotificationManagerCompat.from(view.context)) {
            notify(0, nbuilder.build())
        }
    }
}
