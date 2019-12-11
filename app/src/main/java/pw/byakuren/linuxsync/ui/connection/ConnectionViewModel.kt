package pw.byakuren.linuxsync.ui.connection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConnectionViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is bluetooth Fragment"
    }
    val text: LiveData<String> = _text
}