package de.thecode.android.tazreader.reader

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.ajalt.timberkt.Timber.d
import de.thecode.android.tazreader.TazApplication
import de.thecode.android.tazreader.audio.AudioItem
import de.thecode.android.tazreader.audio.AudioPlayerService
import java.util.concurrent.TimeUnit

class ReaderAudioViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        fun millisToTimeString(duration: Int):String {
            if (duration >= 0) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration.toLong())
                val seconds = TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) - TimeUnit.MINUTES.toSeconds(minutes)
                return String.format("%02d:%02d",minutes,seconds)
            }
            return "-"
        }
    }

    private var service = AudioPlayerService.instance
    private val serviceBroadcastReceiver = ServiceCommunicationReceiver()
    private val timeBroadcastReceiver = ServiceTimeActionReceiver()

    val currentAudioItemLiveData = MutableLiveData<AudioItem?>()
    val currentStateLiveData = MutableLiveData<AudioPlayerService.State>()
    val currentPositionLiveData = MutableLiveData<Int>()
//    val isPlayingLiveData = MutableLiveData<Boolean>()

    init {
        currentAudioItemLiveData.observeForever {
            if (it!=null) currentPositionLiveData.postValue(it.resumePosition)
        }
        syncAudioItemFromService()
        LocalBroadcastManager.getInstance(getApplication())
                .registerReceiver(serviceBroadcastReceiver, IntentFilter(AudioPlayerService.ACTION_STATE_CHANGED))
        LocalBroadcastManager.getInstance(getApplication())
                .registerReceiver(timeBroadcastReceiver, IntentFilter(AudioPlayerService.ACTION_POSITION_UPDATE))

    }

    override fun onCleared() {
        LocalBroadcastManager.getInstance(getApplication())
                .unregisterReceiver(serviceBroadcastReceiver)
        LocalBroadcastManager.getInstance(getApplication())
                .unregisterReceiver(timeBroadcastReceiver)
    }

    fun startPlaying(audioItem: AudioItem){
        d {
            "start playing $audioItem"
        }
        val intent = Intent(getApplication(), AudioPlayerService::class.java)
        intent.putExtra(AudioPlayerService.EXTRA_AUDIO_ITEM, audioItem)
    
        getApplication<TazApplication>().startService(intent)
    }

    fun stopPlaying() {
        service?.stopPlaying()
    }

    fun pauseOrResume() {
        service?.pauseOrResumePlaying()
    }

    fun seekTo(millis:Int) {
        service?.seekToPosition(millis)
    }

    fun rewind30Seconds() {
        service?.rewind30Seconds()
    }


    private fun syncAudioItemFromService() {
        service = AudioPlayerService.instance
        service?.let {
            currentAudioItemLiveData.postValue(it.audioItem)
            currentStateLiveData.postValue(it.state)
        } ?: run {
            currentAudioItemLiveData.postValue(null)
        }
    }




    inner class ServiceCommunicationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            d { "onReceive $intent"}
            syncAudioItemFromService()
        }
    }

    inner class ServiceTimeActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            d { "onReceive $intent"}
            service?.let { innerService ->
                innerService.audioItem?.let {
                    currentPositionLiveData.postValue(it.resumePosition)
                }
            }
        }
    }

}