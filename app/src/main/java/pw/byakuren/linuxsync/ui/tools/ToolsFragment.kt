package pw.byakuren.linuxsync.ui.tools

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import pw.byakuren.linuxsync.R

class ToolsFragment : Fragment() {

    private lateinit var toolsViewModel: ToolsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        toolsViewModel =
            ViewModelProviders.of(this).get(ToolsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_tools, container, false)

        val settings = root.context.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)

        setSwitchStates(root, settings)

        return root
    }

    private fun setSwitchStates(view: View, settings: SharedPreferences) {
        val battSwitch: Switch = view.findViewById(R.id.battery_update_switch)
        battSwitch.isChecked = settings.getBoolean(getString(R.string.setting_battery_updates), true)

        val encSwitch: Switch = view.findViewById(R.id.rsa_switch)
        encSwitch.isChecked = settings.getBoolean(getString(R.string.setting_use_rsa), true)
    }
}