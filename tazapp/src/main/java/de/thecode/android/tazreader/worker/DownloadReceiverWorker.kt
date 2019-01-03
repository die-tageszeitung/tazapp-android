package de.thecode.android.tazreader.worker

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.github.ajalt.timberkt.Timber.d
import com.github.ajalt.timberkt.Timber.e
import com.github.ajalt.timberkt.Timber.w
import de.thecode.android.tazreader.*
import de.thecode.android.tazreader.data.DownloadState
import de.thecode.android.tazreader.data.DownloadType
import de.thecode.android.tazreader.data.Paper
import de.thecode.android.tazreader.data.Resource
import de.thecode.android.tazreader.secure.HashHelper
import de.thecode.android.tazreader.start.StartActivity
import de.thecode.android.tazreader.utils.deleteQuietly
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.compress.utils.InputStreamStatistics
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.NoSuchAlgorithmException


class DownloadReceiverWorker(context: Context, workerParams: WorkerParameters) : LoggingWorker(context, workerParams) {

    companion object {

        private const val ARG_DOWNLOAD_ID = "downloadId"
        private const val ARG_ACTION = "action"

        fun scheduleNow(downloadId: Long, action: String?) {
            val data = Data.Builder()
                    .putLong(ARG_DOWNLOAD_ID, downloadId)
                    .putString(ARG_ACTION, action)
                    .build()

            val request = OneTimeWorkRequest.Builder(DownloadReceiverWorker::class.java)
                    .setInputData(data)
                    .build()

            WorkManager.getInstance()
                    .enqueue(request)
        }

    }

    override fun doBackgroundWork(): Result {
        val downloadId = inputData.getLong(ARG_DOWNLOAD_ID, -1)
        d { "downloadId $downloadId" }
        if (downloadId != -1L) {
            val systemDownloadManagerInfo = downloadManager.getSystemDownloadManagerInfo(downloadId)
            val download = downloadsRepository.get(downloadId)
            if (download != null) {
                d { "download $download" }
                val action = inputData.getString(ARG_ACTION)
                d { "action $action" }
                if (!action.isNullOrBlank()) {
                    if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                        download.state = DownloadState.DOWNLOADED
                        download.downloadManagerId = 0
                        downloadsRepository.save(download)
                        val downloadedFile = getFileFromUri(systemDownloadManagerInfo.localUri)
                        d { "File downloaded: $downloadedFile" }
                        if (downloadedFile != null && downloadedFile.exists()) {
                            val deleteDownload = fun() {
                                downloadedFile.deleteQuietly()
                                downloadsRepository.delete(download)
                            }
                            when (download.type) {
                                DownloadType.UPDATE -> {
                                    downloadsRepository.delete(download)
                                    return Result.success()
                                }
                                DownloadType.PAPER, DownloadType.RESOURCE -> {
                                    val downloadable = when (download.type) {
                                        DownloadType.PAPER -> paperRepository.getPaperWithBookId(download.key)
                                        DownloadType.RESOURCE -> resourceRepository.getWithKey(download.key)
                                        else -> null
                                    }
                                    if (downloadable != null) {
                                        val checkDownload = fun(): Boolean {
                                            d { "checking file size… " }
                                            if (downloadable.len != 0L && downloadable.len != downloadedFile.length()) {
                                                e { "Wrong size of download. expected: ${downloadable.len}, file: ${downloadedFile.length()}" }
                                                return false
                                            }
                                            d { "checking file hash… " }
                                            try {
                                                val fileHash = HashHelper.getHash(downloadedFile, HashHelper.SHA_1)
                                                if (!fileHash.isNullOrBlank() && fileHash != downloadable.fileHash) {
                                                    e { "Wrong hash of download. expected: ${downloadable.fileHash}, fileHash: $fileHash" }
                                                    return false
                                                }
                                            } catch (e: NoSuchAlgorithmException) {
                                                w(e)
                                            } catch (e: IOException) {
                                                e(e)
                                                return false
                                            }
                                            return true
                                        }
                                        if (checkDownload()) {
                                            download.progress = 0
                                            download.state = DownloadState.EXTRACTING
                                            downloadsRepository.save(download)
                                            val outputDir = when (download.type) {
                                                DownloadType.PAPER -> storageManager.getPaperDirectory(download.key)
                                                DownloadType.RESOURCE -> storageManager.getResourceDirectory(download.key)
                                                else -> null
                                            }
                                            if (outputDir != null) {
                                                try {
                                                    outputDir.mkdirs()
                                                    val zipFile = ZipFile(downloadedFile)
                                                    zipFile.use { file ->
                                                        var compressedCount = 0L
                                                        var compressedSizeofAll = 0L
                                                        val countEntries = file.entries
                                                        while (countEntries.hasMoreElements()) {
                                                            val entry = countEntries.nextElement()
                                                            compressedSizeofAll += entry.compressedSize
                                                        }
                                                        val entries = file.entriesInPhysicalOrder
                                                        while (entries.hasMoreElements()) {
                                                            val entry = entries.nextElement()
                                                            val entryDestination = File(outputDir, entry.name)
                                                            if (entry.isDirectory) {
                                                                entryDestination.mkdirs()
                                                            } else {
                                                                entryDestination.parentFile.mkdirs()
                                                                val inputStream = zipFile.getInputStream(entry)
                                                                d { "extracting ${entry.name}…" }
                                                                val outputStream = FileOutputStream(entryDestination)
                                                                outputStream.use {
                                                                    IOUtils.copy(inputStream, outputStream)
                                                                    IOUtils.closeQuietly(inputStream)
                                                                    d { "… done" }
                                                                    if (inputStream is InputStreamStatistics) {
                                                                        compressedCount += inputStream.compressedCount
                                                                        // TODO publish progress
                                                                        val progress = (compressedCount * 100 / compressedSizeofAll).toInt()
                                                                        if (progress != download.progress) {
                                                                            download.progress = progress
                                                                            downloadsRepository.save(download)
                                                                        }

                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    downloadedFile.deleteQuietly() // not needed anymore
                                                    download.state = DownloadState.CHECKING
                                                    download.progress = 0
                                                    downloadsRepository.save(download)
                                                    val plistFile = when (download.type) {
                                                        DownloadType.PAPER -> File(outputDir, Paper.CONTENT_PLIST_FILENAME)
                                                        DownloadType.RESOURCE -> File(outputDir, Resource.SHA1_PLIST)
                                                        else -> null
                                                    }
                                                    if (plistFile != null && plistFile.exists()) {

                                                        d { "parsing plist for HashVals…" }
                                                        val root = PropertyListParser.parse(plistFile) as NSDictionary
                                                        val hashValsDict = root.objectForKey("HashVals") as NSDictionary
                                                        hashValsDict.entries.forEach {
                                                            if (!it.key.isNullOrBlank()) {
                                                                val checkFile = File(outputDir, it.key)
                                                                d { "checking file ${checkFile.absolutePath}" }
                                                                if (!checkFile.exists()) throw FileNotFoundException("${checkFile.absolutePath} not found")
                                                                else {
                                                                    try {
                                                                        if (!HashHelper.verifyHash(checkFile, (it.value as NSString).content, HashHelper.SHA_1))
                                                                            throw FileNotFoundException("Wrong hash for file " + checkFile.name)
                                                                    } catch (e: NoSuchAlgorithmException) {
                                                                        w(e)
                                                                    }

                                                                }
                                                            }
                                                        }
                                                        download.state = DownloadState.READY
                                                        downloadsRepository.save(download)
                                                        return Result.success()
                                                    } else {
                                                        throw IOException("no plist file")
                                                    }
                                                    //TODO check Unzip


                                                } catch (exception: Exception) {
                                                    e(exception)
                                                    outputDir.deleteQuietly()
                                                    deleteDownload()
                                                }
                                            } else {
                                                deleteDownload()
                                            }
                                        } else {
                                            deleteDownload()
                                        }
                                    }
                                }
                                else -> {
                                    w { "download type unknown" }
                                    deleteDownload()
                                }
                            }
                        } else {
                            e { "downloaded File cannot be found" }
                            downloadsRepository.delete(download)
                        }
                    } else if (action == DownloadManager.ACTION_NOTIFICATION_CLICKED) {
                        if (download.type == DownloadType.PAPER) {
                            val libIntent = Intent(applicationContext, StartActivity::class.java)
                            libIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            applicationContext.startActivity(libIntent)
                        }
                    }
                } else {
                    w { "no action found" }
                }
            } else {
                w { "no internal download found" }
            }

        } else {
            w { "no downloadId found" }
        }
        return Result.failure()
    }

    private fun getFileFromUri(uri: Uri?): File? {
        uri?.let {
            var filePath: String? = uri.path
            if ("content" == uri.scheme) {
                val filePathColumn = arrayOf(MediaColumns.DATA)
                val contentResolver = app.contentResolver
                val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
                // For Kotlin Beginners: cursor? nullsafe + use: auto close after use
                cursor?.use {
                    it.moveToFirst()
                    val columnIndex = it.getColumnIndex(filePathColumn[0])
                    filePath = it.getString(columnIndex)
                }
            }
            return File(filePath)
        }
        return null
    }


}