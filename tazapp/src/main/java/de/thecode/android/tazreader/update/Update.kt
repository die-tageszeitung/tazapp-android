package de.thecode.android.tazreader.update

import android.content.Intent
import android.net.Uri
import com.github.ajalt.timberkt.Timber.e
import com.github.ajalt.timberkt.Timber.w
import de.thecode.android.tazreader.*
import org.jetbrains.anko.doAsync

class Update private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: Update? = null

        fun getInstance(): Update {
            return INSTANCE ?: synchronized(this) {
                Update()
            }
        }
    }

    private var isGooglePlayVersion: Boolean = false
    var updateMessageShown: Boolean = false

    init {
        try {
            val installer = app.packageManager
                    .getInstallerPackageName(userDeviceInfo.packageName)
            isGooglePlayVersion = InstallerID.GOOGLE_PLAY.containsId(installer)
        } catch (e: Throwable) {
            w(e)
        }
    }

    fun  setLatestVersion(latest:String){
        try {
            settings.latestVersion = latest.toInt()
        } catch (ex: NumberFormatException) {
            e(ex)
        }

    }

    fun update() {
        var selfDownload = false
        if (isGooglePlayVersion) {
            try {
                val updateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + userDeviceInfo.packageName))
                updateIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                app.startActivity(updateIntent)
            } catch (anfe: android.content.ActivityNotFoundException) {
                selfDownload = true
            }
        } else {
            selfDownload = true
        }
        if (selfDownload) {
            doAsync {
                downloadManager.downloadUpdate()
            }
        }
    }

    fun updateAvailable(): Boolean {
        return settings.latestVersion > BuildConfig.VERSION_CODE
    }

    enum class InstallerID(vararg val ids: String) {
        GOOGLE_PLAY("com.android.vending", "com.google.android.feedback"),
        AMAZON_APP_STORE("com.amazon.venezia"),
        GALAXY_APPS("com.sec.android.app.samsungapps");

        fun containsId(id: String): Boolean {
            return ids.contains(id)
        }
    }
}