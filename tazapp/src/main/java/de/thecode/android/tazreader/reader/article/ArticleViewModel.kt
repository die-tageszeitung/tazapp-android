package de.thecode.android.tazreader.reader.article

import android.os.AsyncTask
import androidx.lifecycle.*
import com.github.ajalt.timberkt.Timber
import de.thecode.android.tazreader.app
import de.thecode.android.tazreader.data.ITocItem
import de.thecode.android.tazreader.data.Paper
import de.thecode.android.tazreader.reader.ReaderAudioViewModel
import de.thecode.android.tazreader.reader.ReaderViewModel
import de.thecode.android.tazreader.utils.AsyncTransformations

class ArticleViewModel(private val readerViewModel: ReaderViewModel, private val audioViewModel: ReaderAudioViewModel, val key: String, private val position_: String?) : AndroidViewModel(app) {


    var position: String? = position_
    set(value) {
        AsyncTask.execute {
            val positionStore = readerViewModel.getStore(Paper.STORE_KEY_POSITION_IN_ARTICLE + "_" + key)
            positionStore.value = position
            readerViewModel.storeRepository
                    .saveStore(positionStore)
        }
        field = value
    }

    val tocItemLiveData: LiveData<ITocItem> = AsyncTransformations.map(readerViewModel.paperLiveData) { paper ->
        if (position.isNullOrBlank()) {
            val positionStore = readerViewModel.getStore(Paper.STORE_KEY_POSITION_IN_ARTICLE + "_" + key)
            positionStore?.let { store ->
                position = store.value
            }
        }
        paper.plist.getIndexItem(key)
    }

    val playerButtonVisiblityLiveData: MediatorLiveData<Boolean> = MediatorLiveData()



    init {
        readerViewModel.currentKey = key
        playerButtonVisiblityLiveData.addSource(tocItemLiveData) {
            checkPlayerButton()
        }
        playerButtonVisiblityLiveData.addSource(audioViewModel.playerVisibleLiveData) {
            checkPlayerButton()
        }

    }

    private fun checkPlayerButton() {

        var showPlayerButton = false
        tocItemLiveData.value?.let { tocItem ->
            if (tocItem is Paper.Plist.Page.Article) {
                if (!tocItem.audiolink.isNullOrBlank()) {
                    audioViewModel.playerVisibleLiveData.value?.let {
                        showPlayerButton = !it
                    }
                }
            }

        }
        Timber.d {
            "XXXXXX CHECKING PLAYER BUTTON $showPlayerButton"
        }
        playerButtonVisiblityLiveData.value = showPlayerButton
    }

}

class ArticleViewModelFactory(private val readerViewModel: ReaderViewModel, private val audioViewModel: ReaderAudioViewModel, val key: String, val position: String?) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ArticleViewModel(readerViewModel, audioViewModel, key, position) as T
    }

}