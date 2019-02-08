package de.thecode.android.tazreader.reporterror

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.os.Build
import com.commonsware.cwac.provider.StreamProvider
import de.thecode.android.tazreader.BuildConfig
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.TazSettings
import de.thecode.android.tazreader.sync.AccountHelper
import de.thecode.android.tazreader.utils.FileUtils
import de.thecode.android.tazreader.utils.StorageManager
import de.thecode.android.tazreader.utils.UserDeviceInfo
import timber.log.Timber
import java.io.File


class ErrorReporter {
    companion object {
        fun sendErrorMail(context: Context) {

            val settings = TazSettings.getInstance(context)

            val aboId = AccountHelper.getInstance(context)
                    .getUser("")
            val userDeviceInfo = UserDeviceInfo.getInstance(context)

            val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            emailIntent.type = "message/rfc822"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.ERRORMAIL))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.errormail_subject, context.getString(R.string.app_name), aboId))
            try {
                val bodyInputStream = context.assets.open("errorReportMail/body.txt")

                val body = bodyInputStream.bufferedReader()
                        .use { it.readText() }
                        .replaceFirst("{appversion}", userDeviceInfo.versionName)
                        .replaceFirst("{installid}", userDeviceInfo.installationId)
                        .replaceFirst("{aboid}", aboId)
                        .replaceFirst("{androidVersion}", Build.VERSION.SDK_INT.toString() + " (" + Build.VERSION.RELEASE + ")")
                        .replaceFirst("{manufacturer}", Build.MANUFACTURER)
                        .replaceFirst("{model}", Build.MODEL)
                        .replaceFirst("{product}", Build.PRODUCT)
                        .replaceFirst("{device}", Build.DEVICE)
                        .replaceFirst("{brand}", Build.BRAND)
                        .replaceFirst("{ram}", ramMemorySize(context))
                        .replaceFirst("{archs}", userDeviceInfo.supportedArchList.joinToString(","))
                        .replaceFirst("{space}", space(context))
                        .replaceFirst("{pushToken}", settings.firebaseToken)

                emailIntent.putExtra(Intent.EXTRA_TEXT, body)
            } catch (e: Exception) {
                Timber.e(e, "Error parsing body")
            }
            if (settings.isWriteLogfile) {
                val logDir = StorageManager.getInstance(context)
                        .logCache
                try {
                    val uris = logDir.walk()
                            .maxDepth(1)
                            .filter {
                                !it.isDirectory
                            }
                            .map {
                                Timber.d("Adding %s to mail", it)
                                StreamProvider.getUriForFile(BuildConfig.APPLICATION_ID + ".streamprovider", it)
//                                uris.add(contentUri)
                            }
                            .toList()


                    if (uris.isNotEmpty()) {
                        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error reading logs")
                }
                settings.isWriteLogfile = false
            }
            context.startActivity(emailIntent)
        }

        private fun ramMemorySize(context: Context): String {
            val mi = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager?
            activityManager!!.getMemoryInfo(mi)
            return "${mi.availMem / 1048576L} (${mi.totalMem / 1048576L})"
        }

        private fun space(context: Context): String {
            val currentPath = File(TazSettings.getInstance(context).dataFolderPath)
            return "${currentPath.absolutePath} (frei: ${FileUtils.readableSize(currentPath.freeSpace)})"
        }
    }


}