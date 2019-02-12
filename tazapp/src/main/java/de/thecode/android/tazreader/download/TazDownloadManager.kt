package de.thecode.android.tazreader.download

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Base64
import androidx.annotation.WorkerThread
import com.github.ajalt.timberkt.d
import de.thecode.android.tazreader.*
import de.thecode.android.tazreader.data.Download
import de.thecode.android.tazreader.data.DownloadState
import de.thecode.android.tazreader.data.DownloadType
import de.thecode.android.tazreader.data.Paper
import de.thecode.android.tazreader.okhttp3.RequestHelper
import de.thecode.android.tazreader.sync.AccountHelper
import de.thecode.android.tazreader.utils.UserAgentHelper
import de.thecode.android.tazreader.utils.UserDeviceInfo
import de.thecode.android.tazreader.utils.deleteQuietly
import de.thecode.android.tazreader.utils.getStringIdByName
import java.io.File


class TazDownloadManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: TazDownloadManager? = null

        fun getInstance(): TazDownloadManager {
            return INSTANCE ?: synchronized(this) {
                TazDownloadManager()
            }
        }
    }

    private val systemDownloadManager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val requestHelper = RequestHelper.getInstance(app)
    private val userAgentHelper = UserAgentHelper.getInstance(app)

    @WorkerThread
    fun downloadPaper(bookId: String, wifiOnly: Boolean = false): Result {
        val paper = paperRepository.getPaperWithBookId(bookId)
        d { "requesting paper download for paper $paper" }
        val download = paperRepository.getDownloadForPaper(bookId)
        d { "download $download" }
        val result = Result(download = download)
        download.file = storageManager.getDownloadFile("$bookId.paper.zip")
        //val destinationFile = storageManager.getDownloadFile(paper)
        if (!checkFreeSpace(download.file, calculateBytesNeeded(paper.len))) {
            result.state = Result.STATE.NOSPACE
            return result
        }
        val requestUri = requestHelper.addToUri(Uri.parse(paper.link))
        val request = createRequest(downloadUri = requestUri, destinationFile = download.file, title = download.title)
        if (wifiOnly) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        if (!paper.publication.isNullOrBlank()) {
            val credentials = accountHelper.getUser(AccountHelper.ACCOUNT_DEMO_USER) + ":" + accountHelper.getPassword(
                    AccountHelper.ACCOUNT_DEMO_PASS)
            request.addRequestHeader("Authorization",
                    "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP))
        }
        try {
            download.downloadManagerId = systemDownloadManager.enqueue(request)
            download.state = DownloadState.DOWNLOADING
            downloadsRepository.save(download)
            val resourceKeyForPaper = paper.resource
            val resourcePartnerStore = storeRepository.getStore(bookId, Paper.STORE_KEY_RESOURCE_PARTNER)
            resourcePartnerStore.value = resourceKeyForPaper
            storeRepository.saveStore(resourcePartnerStore)
            downloadResource(resourceKeyForPaper, wifiOnly)
        } catch (e: IllegalArgumentException) {
            result.state = Result.STATE.NOMANAGER
            return result
        }
        result.state = Result.STATE.SUCCESS
        return result
    }

    @WorkerThread
    fun downloadResource(key: String, wifiOnly: Boolean = false, override: Boolean = false): Result {
        val resource = resourceRepository.getWithKey(key)
        d { "requesting resource download for resource $resource" }
        val download = resourceRepository.getDownload(key)
        d { "download $download" }
        val result = Result(download = download)
        if (download.state != DownloadState.NONE) {
            if (download.state == DownloadState.DOWNLOADING || override) {
                systemDownloadManager.remove(download.downloadManagerId)
                download.downloadManagerId = 0
                download.state = DownloadState.NONE
                downloadsRepository.save(download)
            } else {
                result.state = Result.STATE.ALLREADYDOWNLOADED
                return result
            }
        }

        if (!checkFreeSpace(download.file, calculateBytesNeeded(resource.len))) {
            result.state = Result.STATE.NOSPACE
            return result
        }

        val requestUri = requestHelper.addToUri(Uri.parse(resource.url))
        val request = createRequest(downloadUri = requestUri, destinationFile = download.file, title = download.title)
        if (wifiOnly) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        try {
            download.downloadManagerId = systemDownloadManager.enqueue(request)
            download.state = DownloadState.DOWNLOADING
            downloadsRepository.save(download)
        } catch (e: IllegalArgumentException) {
            result.state = Result.STATE.NOMANAGER
            return result
        }
        result.state = Result.STATE.SUCCESS
        return result
    }

    @WorkerThread
    fun downloadUpdate() {
        val downloads = downloadsRepository.get(DownloadType.UPDATE)
        downloads.forEach {
            systemDownloadManager.remove(it.downloadManagerId)
            downloadsRepository.delete(it)
        }

        val supportedArchs = userDeviceInfo.supportedArchList
        var arch: String? = null
        if (supportedArchs != null && supportedArchs.size > 0) {
            var bestArch = 0
            for (supportedArch in supportedArchs) {
                val newArch = UserDeviceInfo.getWeightForArch(supportedArch)
                if (newArch > bestArch) bestArch = newArch
            }
            if (bestArch == 2 || bestArch == 3 || bestArch == 6 || bestArch == 7) {
                //Filter for build archTypes
                arch = UserDeviceInfo.getArchForWeight(bestArch)
            }
            if (arch.isNullOrBlank()) arch = "universal"
        }


        val fileName = StringBuilder("tazapp-").append(BuildConfig.FLAVOR)
                .append("-")
                .append(arch)
                .append("-")
                .append(BuildConfig.BUILD_TYPE)
                .append(".apk")

        val uri = Uri.parse(BuildConfig.APKURL)
                .buildUpon()
                .appendEncodedPath(fileName.toString())
                .build()

        val updateFile = File(storageManager.updateAppCache, fileName.toString())

        val request = createRequest(
                downloadUri = uri,
                destinationFile = updateFile,
                title = res.getString(R.string.update_app_download_notification, res.getString(R.string.app_name)),
                notificationVisility = DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                visibleInDownloadUi = true,
                mimeType = "application/vnd.android.package-archive"
        )
        systemDownloadManager.enqueue(request)

    }

    fun cancelDownload(downloadId: Long) {
        val download = downloadsRepository.get(downloadId)
        systemDownloadManager.remove(downloadId)
        if (download != null && download.state == DownloadState.DOWNLOADING) {
            downloadsRepository.delete(download)
        }
    }


    private fun createRequest(
            downloadUri: Uri,
            destinationFile: File,
            title: String,
            notificationVisility: Int = DownloadManager.Request.VISIBILITY_VISIBLE,
            mimeType: String = "application/zip",
            visibleInDownloadUi: Boolean = false
    ): DownloadManager.Request {
        d { "create request for $downloadUri" }
        val request = try {
            DownloadManager.Request(downloadUri)
        } catch (e: Exception) {
            DownloadManager.Request(Uri.parse(downloadUri.toString().replace("https://", "http")))
        }
        request.addRequestHeader(UserAgentHelper.USER_AGENT_HEADER_NAME, userAgentHelper.userAgentHeaderValue)
        if (destinationFile.exists()) destinationFile.deleteQuietly()
        request.setTitle(title)
        request.setDestinationUri(Uri.fromFile(destinationFile))
        request.setVisibleInDownloadsUi(visibleInDownloadUi)
        request.setMimeType(mimeType)
        request.setNotificationVisibility(notificationVisility)
        return request
    }

    fun getSystemDownloadManagerInfo(downloadId: Long): SystemDownloadManagerInfo {
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        var cursor: Cursor? = null
        try {
            cursor = systemDownloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                return SystemDownloadManagerInfo.fromCursor(cursor)
            }
        } finally {
            cursor?.close()
        }
        return SystemDownloadManagerInfo(downloadId)
    }

    data class Result(var state: STATE = STATE.UNKNOWN, val download: Download) {
        enum class STATE { UNKNOWN, SUCCESS, NOMANAGER, NOSPACE, ALLREADYDOWNLOADED;

            fun getText(): String {
                return app.getString(app.getStringIdByName("start_download_result_" + this.toString()))
            }
        }
    }

    data class SystemDownloadManagerInfo(var downloadId: Long = 0,
                                         var status: STATE = STATE.NOTFOUND,
                                         var statusText: String = res.getString(R.string.download_status_NOTFOUND),
                                         var reason: Int = DownloadManager.ERROR_UNKNOWN,
                                         var reasonText: String = res.getString(R.string.download_reason_0),
                                         var bytesDownloadedSoFar: Long = 0L,
                                         var totalSizeBytes: Long = 0L,
                                         var uri: Uri? = null,
                                         var title: String? = null,
                                         var description: String? = null,
                                         var lastModifiedTimestamp: Long = 0L,
                                         var localUri: Uri? = null,
                                         var mediaProviderUri: Uri? = null,
                                         var mediaType: String? = null
    ) {
        enum class STATE(val int: Int = 0) {
            SUCCESSFUL(DownloadManager.STATUS_SUCCESSFUL),
            FAILED(DownloadManager.STATUS_FAILED),
            PAUSED(DownloadManager.STATUS_PAUSED),
            PENDING(DownloadManager.STATUS_PENDING),
            RUNNING(DownloadManager.STATUS_RUNNING),
            NOTFOUND;

            companion object {
                private val map = STATE.values()
                        .associateBy(STATE::int)

                fun fromInt(type: Int) = map[type] ?: NOTFOUND
            }
        }

        companion object {
            fun fromCursor(cursor: Cursor): SystemDownloadManagerInfo {
                val result = SystemDownloadManagerInfo()
                result.downloadId = cursor.getLong(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_ID))
                result.status = SystemDownloadManagerInfo.STATE.fromInt(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)))
                result.statusText = try { res.getString(app.getStringIdByName("download_status_" + result.status)) } catch (e: Exception) { "status: ${result.status}" }
                result.reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                result.reasonText = try { res.getString(app.getStringIdByName("download_reason_" + result.reason)) } catch (e: Exception) { "reason: ${result.reason}" }
                result.bytesDownloadedSoFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                result.totalSizeBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                result.uri = try {
                    Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)))
                } catch (e: Exception) {
                    null
                }
                result.title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
                result.description = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION))
                result.lastModifiedTimestamp = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP))
                result.localUri = try {
                    Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))
                } catch (e: Exception) {
                    null
                }
                result.mediaProviderUri = try {
                    Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI)))
                } catch (e: Exception) {
                    null
                }
                result.mediaType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
                return result
            }
        }
    }


}


private fun calculateBytesNeeded(bytesOfDownload: Long): Long {
    val oneHundred = (100 * 1024 * 1024).toLong()
    val threeDownloads = bytesOfDownload * 3
    val fiveDownloads = bytesOfDownload * 5
    return if (threeDownloads > oneHundred) {
        threeDownloads
    } else
        Math.min(oneHundred, fiveDownloads)
}

private fun checkFreeSpace(downloadFile: File, requestedBytes: Long): Boolean {
    if (requestedBytes <= 0) return true
    return downloadFile.parentFile.freeSpace >= requestedBytes
}

