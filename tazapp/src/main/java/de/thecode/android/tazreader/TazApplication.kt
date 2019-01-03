package de.thecode.android.tazreader

import android.content.res.Resources
import de.thecode.android.tazreader.data.DownloadsRepository
import de.thecode.android.tazreader.data.PaperRepository
import de.thecode.android.tazreader.data.ResourceRepository
import de.thecode.android.tazreader.data.TazSettings
import de.thecode.android.tazreader.download.TazDownloadManager
import de.thecode.android.tazreader.utils.StorageManager

val app: TazApplication by lazy {
    TazApplication.applicationContext()
}

val res: Resources by lazy {
    app.resources
}

val downloadManager by lazy {
    TazDownloadManager.getInstance()
}

val downloadsRepository by lazy {
    DownloadsRepository.getInstance()
}

val paperRepository by lazy {
    PaperRepository.getInstance(app)
}

val resourceRepository by lazy {
    ResourceRepository.getInstance(app)
}

val storageManager by lazy {
    StorageManager.getInstance(app)
}


val settings by lazy { TazSettings.getInstance(app) }

class TazApplication: TazReaderApplication() {

    companion object {
        private var instance: TazApplication? = null

        fun applicationContext() : TazApplication {
            return instance as TazApplication
        }
    }
    init {
        instance = this
    }
}