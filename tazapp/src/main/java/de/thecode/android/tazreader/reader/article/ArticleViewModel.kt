package de.thecode.android.tazreader.reader.article

import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.thecode.android.tazreader.app
import de.thecode.android.tazreader.data.ITocItem
import de.thecode.android.tazreader.data.Paper
import de.thecode.android.tazreader.reader.ReaderViewModel
import de.thecode.android.tazreader.utils.AsyncTransformations

class ArticleViewModel(private val readerViewModel: ReaderViewModel, val key: String, private val position_: String?) : AndroidViewModel(app) {


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

//    val playerButtonVisiblityLiveData: MediatorLiveData<Boolean> = MediatorLiveData()



    init {
        readerViewModel.currentKey = key
//        playerButtonVisiblityLiveData.addSource(tocItemLiveData) {
//            d {
//                "XXX source tocItemLiveData changed"
//            }
//            checkPlayerButton()
//        }
//        playerButtonVisiblityLiveData.addSource(audioViewModel.currentAudioItemLiveData) {
//            d {
//                "XXX source currentAudioItemLiveData changed"
//            }
//            checkPlayerButton()
//        }

    }

//    private fun checkPlayerButton() {
//
//        var showPlayerButton = false
//        tocItemLiveData.value?.let { tocItem ->
//            if (tocItem is Paper.Plist.Page.Article) {
//                if (!tocItem.audiolink.isNullOrBlank()) {
//                    d {
//                        "XXX ${audioViewModel.currentAudioItemLiveData.value}"
//                    }
//                    if (audioViewModel.currentAudioItemLiveData.value == null) {
//                        showPlayerButton = true
//                    }
//                }
//            }
//
//        }
//        Timber.d {
//            "XXXXXX CHECKING PLAYER BUTTON $showPlayerButton"
//        }
//        playerButtonVisiblityLiveData.value = showPlayerButton
//    }

}

class ArticleViewModelFactory(private val readerViewModel: ReaderViewModel, val key: String, val position: String?) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ArticleViewModel(readerViewModel, key, position) as T
    }

}