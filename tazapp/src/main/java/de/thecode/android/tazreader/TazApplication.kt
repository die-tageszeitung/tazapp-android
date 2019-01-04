package de.thecode.android.tazreader

import android.content.res.Resources
import de.thecode.android.tazreader.data.*
import de.thecode.android.tazreader.download.TazDownloadManager
import de.thecode.android.tazreader.notifications.NotificationUtils
import de.thecode.android.tazreader.sync.AccountHelper
import de.thecode.android.tazreader.update.Update
import de.thecode.android.tazreader.utils.StorageManager
import de.thecode.android.tazreader.utils.UserDeviceInfo

val app: TazApplication by lazy {
    TazApplication.applicationContext()
}

val res: Resources by lazy {
    app.resources
}

val downloadManager: TazDownloadManager by lazy {
    TazDownloadManager.getInstance()
}

val downloadsRepository by lazy {
    DownloadsRepository.getInstance()
}

val paperRepository: PaperRepository by lazy {
    PaperRepository.getInstance(app)
}

val resourceRepository: ResourceRepository by lazy {
    ResourceRepository.getInstance(app)
}

val storeRepository: StoreRepository by lazy { StoreRepository.getInstance(app) }

val storageManager: StorageManager by lazy {
    StorageManager.getInstance(app)
}

val notificationUtils: NotificationUtils by lazy {
    NotificationUtils.getInstance(app)
}

val settings: TazSettings by lazy { TazSettings.getInstance(app) }

val update: Update by lazy {
    Update.getInstance()
}

val accountHelper: AccountHelper by lazy { AccountHelper.getInstance(app) }

val userDeviceInfo: UserDeviceInfo by lazy { UserDeviceInfo.getInstance(app) }

class TazApplication : TazReaderApplication() {

    companion object {
        private var instance: TazApplication? = null

        fun applicationContext(): TazApplication {
            return instance as TazApplication
        }
    }

    init {
        instance = this
    }
}