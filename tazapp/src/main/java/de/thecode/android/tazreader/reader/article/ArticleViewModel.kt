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

    init {
        readerViewModel.currentKey = key
    }

}

class ArticleViewModelFactory(private val readerViewModel: ReaderViewModel, val key: String, val position: String?) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ArticleViewModel(readerViewModel, key, position) as T
    }

}