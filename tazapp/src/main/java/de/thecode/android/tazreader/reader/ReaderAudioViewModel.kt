package de.thecode.android.tazreader.reader

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.thecode.android.tazreader.audio.AudioPlayerService

class ReaderAudioViewModel(application: Application) : AndroidViewModel(application) {

    private var service = AudioPlayerService.instance
    private val serviceCreatedReceiver = ServiceCreatedReceiver()

    val playerVisibleLiveData = MutableLiveData<Boolean>()


    init {
        LocalBroadcastManager.getInstance(getApplication())
                .registerReceiver(serviceCreatedReceiver, IntentFilter(AudioPlayerService.ACTION_SERVICE_CREATED))
        playerVisibleLiveData.value = AudioPlayerService.isServiceCreated()
    }

    override fun onCleared() {
        LocalBroadcastManager.getInstance(getApplication())
                .unregisterReceiver(serviceCreatedReceiver)
    }

    fun setPlayerVisible(visible: Boolean) {
        playerVisibleLiveData.value = visible
    }

    inner class ServiceCreatedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            service = AudioPlayerService.instance
            playerVisibleLiveData.postValue(true)
        }
    }
}