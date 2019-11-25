package pw.byakuren.linuxsync.ui.notiftest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NotificationTestViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Send a test notification."
    }
    val text: LiveData<String> = _text
}