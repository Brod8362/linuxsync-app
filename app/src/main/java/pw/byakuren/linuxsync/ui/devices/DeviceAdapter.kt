package pw.byakuren.linuxsync.ui.devices

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pw.byakuren.linuxsync.R

class DeviceAdapter(private var ips: Array<String>, private var dates: Array<String>, private val sharedPreferences: SharedPreferences) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    class ViewHolder(val layout: LinearLayout):RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_list_row, parent, false) as LinearLayout
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ip: TextView = holder.layout.findViewById(R.id.ipText)
        val date: TextView = holder.layout.findViewById(R.id.dateText)
        val button: Button = holder.layout.findViewById(R.id.removeButton)
        ip.text = ips.get(position)
        date.text = dates.get(position)
        button.setOnClickListener {
            val editor = sharedPreferences.edit()
            val rm = ips.get(position)
            editor.remove(rm)
            editor.apply()
            ips = sharedPreferences.all.keys.toTypedArray()
            dates = sharedPreferences.all.values.map { a -> a.toString() }.toTypedArray()
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return ips.size
    }


}