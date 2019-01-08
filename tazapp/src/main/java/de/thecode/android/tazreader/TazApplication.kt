package de.thecode.android.tazreader

import android.content.res.Resources
import android.os.Build
import android.webkit.WebView
import androidx.multidex.MultiDexApplication
import com.github.ajalt.timberkt.Timber.d
import de.thecode.android.tazreader.analytics.AnalyticsWrapper
import de.thecode.android.tazreader.data.*
import de.thecode.android.tazreader.download.TazDownloadManager
import de.thecode.android.tazreader.eventbus.EventBusIndex
import de.thecode.android.tazreader.notifications.NotificationUtils
import de.thecode.android.tazreader.picasso.PicassoHelper
import de.thecode.android.tazreader.reader.ReaderActivity
import de.thecode.android.tazreader.sync.AccountHelper
import de.thecode.android.tazreader.timber.TimberHelper
import de.thecode.android.tazreader.update.Update
import de.thecode.android.tazreader.utils.BuildTypeProvider
import de.thecode.android.tazreader.utils.StorageManager
import de.thecode.android.tazreader.utils.UserDeviceInfo
import de.thecode.android.tazreader.utils.deleteQuietly
import org.greenrobot.eventbus.EventBus
import java.io.File


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

class TazApplication : MultiDexApplication() {

    companion object {
        private var instance: TazApplication? = null

        fun applicationContext(): TazApplication {
            return instance as TazApplication
        }
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        AnalyticsWrapper.initialize(app)
        TimberHelper.initialize(app)
        BuildTypeProvider.installStetho(app)
        EventBus.builder()
                .addIndex(EventBusIndex())
                .installDefaultEventBus()
        notificationUtils.createChannels()
        PicassoHelper.initPicasso(app)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        migrations()
    }

    private fun migrations() {
        val lastVersionCode = settings.getPrefInt(TazSettings.PREFKEY.LASTVERSION, BuildConfig.VERSION_CODE)

        if (lastVersionCode < 16) {
            if (settings.getPrefString(TazSettings.PREFKEY.COLSIZE, "0") == "4")
                settings.setPref(TazSettings.PREFKEY.COLSIZE, "3")
        }
        if (lastVersionCode < 32) {
            settings.removePref(TazSettings.PREFKEY.FONTSIZE)
            settings.removePref(TazSettings.PREFKEY.COLSIZE)
            settings.setPref(TazSettings.PREFKEY.PAPERMIGRATEFROM, lastVersionCode)
        }
        if (lastVersionCode < 52) {
            //Removing all dead prefs from crashlytics
            val dir = File(filesDir.parent + "/shared_prefs/")
            val children = dir.list()
            for (aChildren in children!!) {
                if (aChildren.startsWith("com.crashlytics") || aChildren.startsWith("Twitter") || aChildren.startsWith("io.fabric"))
                    File(dir, aChildren).delete()
            }
            storageManager.getCache("library")
                    ?.deleteQuietly()
        }
        if (lastVersionCode < 3080000) {
            File(filesDir.parent + "/shared_prefs/").walkTopDown()
                    .forEach {
                        if (it.isFile && it.name.startsWith("evernote_jobs")) {
                            it.deleteQuietly()
                        }
                    }
            File(filesDir.parent + "/databases/").walkTopDown()
                    .forEach {
                        if (it.isFile && it.name.startsWith("evernote_jobs")) {
                            it.deleteQuietly()
                        }
                    }
        }


        // MIGRATION BEENDET, setzten der aktuellen Version
        settings.setPref(TazSettings.PREFKEY.LASTVERSION, BuildConfig.VERSION_CODE)
    }

    private fun settingDefaults() {
        settings.setPref(TazSettings.PREFKEY.ISFOOT, false)
        settings.setDefaultPref(TazSettings.PREFKEY.FONTSIZE, "10")
        settings.setDefaultPref(TazSettings.PREFKEY.AUTOLOAD, false)
        settings.setDefaultPref(TazSettings.PREFKEY.AUTOLOAD_WIFI, true)
        settings.setDefaultPref(TazSettings.PREFKEY.ISSCROLL, !resources.getBoolean(R.bool.isTablet))
        settings.setDefaultPref(TazSettings.PREFKEY.COLSIZE, "0.5")
        settings.setDefaultPref(TazSettings.PREFKEY.THEME, ReaderActivity.THEMES.normal.name)
        settings.setDefaultPref(TazSettings.PREFKEY.FULLSCREEN, false)
        settings.setDefaultPref(TazSettings.PREFKEY.CONTENTVERBOSE, true)
        settings.setDefaultPref(TazSettings.PREFKEY.KEEPSCREEN, false)
        settings.setDefaultPref(TazSettings.PREFKEY.ORIENTATION, "auto")
        settings.setDefaultPref(TazSettings.PREFKEY.AUTODELETE, false)
        settings.setDefaultPref(TazSettings.PREFKEY.AUTODELETE_VALUE, 14)
        settings.setDefaultPref(TazSettings.PREFKEY.VIBRATE, true)
        settings.setDefaultPref(TazSettings.PREFKEY.ISSOCIAL, false)
        settings.setDefaultPref(TazSettings.PREFKEY.PAGEINDEXBUTTON, false)
        settings.setDefaultPref(TazSettings.PREFKEY.TEXTTOSPEACH, false)
        settings.setDefaultPref(TazSettings.PREFKEY.ISCHANGEARTICLE, true)
        settings.setDefaultPref(TazSettings.PREFKEY.ISPAGING, true)
        settings.setDefaultPref(TazSettings.PREFKEY.ISJUSTIFY, resources.getBoolean(R.bool.isTablet))
        settings.setDefaultPref(TazSettings.PREFKEY.ISSCROLLTONEXT, resources.getBoolean(R.bool.isTablet))
        settings.setDefaultPref(TazSettings.PREFKEY.PAGETAPTOARTICLE, true)
        settings.setDefaultPref(TazSettings.PREFKEY.PAGEDOUBLETAPZOOM, true)
        settings.setDefaultPref(TazSettings.PREFKEY.PAGETAPBORDERTOTURN, resources.getBoolean(R.bool.isTablet))

        d { "Token: ${settings.firebaseToken}" }

    }
}