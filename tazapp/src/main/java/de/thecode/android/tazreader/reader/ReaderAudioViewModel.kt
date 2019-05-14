package de.thecode.android.tazreader.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class ReaderAudioViewModel(application: Application) : AndroidViewModel(application) {
    val playerVisibleLiveData = MutableLiveData<Boolean>()
    init {
        playerVisibleLiveData.value = false
    }

    fun setPlayerVisible(visible:Boolean) {
        playerVisibleLiveData.value = visible
    }
}