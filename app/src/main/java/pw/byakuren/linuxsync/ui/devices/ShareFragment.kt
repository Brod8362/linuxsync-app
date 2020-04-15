package pw.byakuren.linuxsync.ui.devices

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pw.byakuren.linuxsync.R

class ShareFragment : Fragment() {

    private lateinit var shareViewModel: ShareViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        shareViewModel =
            ViewModelProviders.of(this).get(ShareViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_share, container, false)
        val prefs = this.activity?.getPreferences(MODE_PRIVATE)
        if (prefs != null)
            populateRecycler(prefs, root)
        return root
    }


    fun populateRecycler(sharedPreferences: SharedPreferences, view: View) {

        val ipList: Array<String> = sharedPreferences.all.keys.toTypedArray()
        val dateList: Array<String> = sharedPreferences.all.values.map { a -> a.toString() }.toTypedArray()

        val linearLayoutManager = LinearLayoutManager(view.context)
        val recyclerView: RecyclerView = view.findViewById(R.id.deviceRecyclerView)
        val adapter = DeviceAdapter(ipList, dateList, sharedPreferences)

        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter
    }

}