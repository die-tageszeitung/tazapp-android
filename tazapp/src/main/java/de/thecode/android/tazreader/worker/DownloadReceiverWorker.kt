package de.thecode.android.tazreader.worker

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import androidx.annotation.WorkerThread
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
import de.thecode.android.tazreader.data.*
import de.thecode.android.tazreader.secure.HashHelper
import de.thecode.android.tazreader.start.StartActivity
import de.thecode.android.tazreader.utils.deleteQuietly
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.IOUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.NoSuchAlgorithmException


class DownloadReceiverWorker(context: Context, workerParams: WorkerParameters) : LoggingWorker(context, workerParams) {

    companion object {

        private const val ARG_ACTION = "action"

        @WorkerThread
        fun scheduleNow(downloadId: Long, action: String?) {
            val download = downloadsRepository.get(downloadId)
            if (download != null) {
                val data = Data.Builder()
                        //.putLong(ARG_DOWNLOAD_ID, downloadId)
                        .putString(ARG_ACTION, action)
                        .build()
                val request = OneTimeWorkRequest.Builder(DownloadReceiverWorker::class.java)
                        .setInputData(data)
                        .build()

                download.workerUuid = request.id
                downloadsRepository.save(download)
                WorkManager.getInstance()
                        .enqueue(request)
            }
        }

    }

    override fun doBackgroundWork(): Result {
        // val downloadId = inputData.getLong(ARG_DOWNLOAD_ID, -1)
        // d { "downloadId $downloadId" }
        // if (downloadId != -1L) {


            val download = downloadsRepository.getByWorkerUuid(id)
            if (download != null) {
                d { "download $download" }
                val systemDownloadManagerInfo = downloadManager.getSystemDownloadManagerInfo(download.downloadManagerId)
                d { "systemDownloadState $systemDownloadManagerInfo" }
                val action = inputData.getString(ARG_ACTION)
                d { "action $action" }
                if (!action.isNullOrBlank()) {
                    if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                        download.state = DownloadState.DOWNLOADED
                        downloadsRepository.save(download)
                        //val downloadedFile = getFileFromUri(systemDownloadManagerInfo.localUri)
                        //d { "File downloaded: $downloadedFile" }
                        val cancelDownload = fun(message:String?) {
                            e { "$message" }
                            download.file.deleteQuietly()
                            downloadsRepository.delete(download)
                            EventBus.getDefault().post(DownloadEvent(download,message))
                        }
                        if (download.file.exists()) {
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
                                            if (downloadable.len != 0L && downloadable.len != download.file.length()) {
                                                e { "Wrong size of download. expected: ${downloadable.len}, file: ${download.file.length()}" }
                                                return false
                                            }
                                            d { "checking file hash… " }
                                            try {
                                                val fileHash = HashHelper.getHash(download.file, HashHelper.SHA_1)
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
                                                    val zipFile = ZipFile(download.file)
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
                                                                    compressedCount += entry.compressedSize
                                                                    val progress = (compressedCount * 100 / compressedSizeofAll).toInt()
                                                                    if (progress != download.progress) {
                                                                        download.progress = progress
                                                                        downloadsRepository.save(download)
                                                                    }
                                                                    d { "… done" }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    download.file.deleteQuietly() // not needed anymore
                                                    download.state = DownloadState.CHECKING
                                                    download.progress = 100
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
                                                        download.progress = 100
                                                        download.downloadManagerId = 0
                                                        downloadsRepository.save(download)
                                                        when (download.type) {
                                                            DownloadType.PAPER -> {
                                                                notificationUtils.showDownloadFinishedNotification(downloadable as Paper)
                                                            }
                                                        }
                                                        EventBus.getDefault().post(DownloadEvent(download))
                                                        return Result.success()
                                                    } else {
                                                        throw IOException("Keine PList gefunden")
                                                    }
                                                    //TODO check Unzip


                                                } catch (exception: Exception) {
                                                    e(exception)
                                                    outputDir.deleteQuietly()
                                                    cancelDownload(exception.localizedMessage)
                                                }
                                            } else {
                                                cancelDownload("Zielverzeichnis nicht vorhanden")
                                            }
                                        } else {
                                            cancelDownload("Größe oder Prüfsumme falsch")
                                        }
                                    }
                                }
                                else -> {
                                    cancelDownload("Unbekannter Download-Typ: ${download.type}")
                                }
                            }
                        } else {
                            cancelDownload("Heruntergeladene Datei nicht gefunden")
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

//        } else {
//            w { "no downloadId found" }
//        }
        return Result.failure()
    }
}