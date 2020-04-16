package pw.byakuren.linuxsync.ui.tools

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import pw.byakuren.linuxsync.R
import java.time.LocalDateTime

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

        val input_field: EditText = root.findViewById(R.id.trusted_device_field)
        val button: Button = root.findViewById(R.id.add_button)
        button.setOnClickListener {
            val prefs = root.context.getSharedPreferences(getString(R.string.prefs_trusted_devices), MODE_PRIVATE)
            val edit = prefs.edit()
            edit.putString(input_field.editableText.toString(), LocalDateTime.now().toString())
            edit.apply()
            Toast.makeText(this.context,"Added new trusted device", Toast.LENGTH_SHORT).show()
        }
        return root
    }
}