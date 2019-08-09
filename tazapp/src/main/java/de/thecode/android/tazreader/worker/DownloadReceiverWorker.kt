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
import de.thecode.android.tazreader.download.TazDownloadManager
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
            d {
                "scheduling DownloadReceiverWorker for $downloadId with action $action"
            }
            val download = downloadsRepository.get(downloadId)
            if (download != null) {
                val data = Data.Builder()
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
        var download: Download? = null
        var targetDir: File? = null
        try {
            download = getDownload()
            d { "download $download" }
            val systemDownloadManagerInfo = downloadManager.getSystemDownloadManagerInfo(download.downloadManagerId)
            d { "systemDownloadState $systemDownloadManagerInfo" }
            val action = getAction()
            when (action) {
                DownloadManager.ACTION_NOTIFICATION_CLICKED -> {
                    if (download.type == DownloadType.PAPER) {
                        val libIntent = Intent(applicationContext, StartActivity::class.java)
                        libIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        applicationContext.startActivity(libIntent)
                    }
                    return Result.success()
                }
                DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {

                    //Check systemDownloadManagerInfo
                    when(systemDownloadManagerInfo.status) {
                        TazDownloadManager.SystemDownloadManagerInfo.STATE.FAILED -> {
                            throw DownloadException("${systemDownloadManagerInfo.statusText}: ${systemDownloadManagerInfo.reasonText} (${systemDownloadManagerInfo.status},${systemDownloadManagerInfo.reason})");
                        }
                        TazDownloadManager.SystemDownloadManagerInfo.STATE.SUCCESSFUL -> {
                            //Download abgeschloßen
                            download.state = DownloadState.DOWNLOADED
                            downloadsRepository.save(download)

                            systemDownloadManagerInfo.localUri?.let {
                                val fileFromLocalUri = File(systemDownloadManagerInfo.localUri!!.path)
                                if (download.file.absolutePath != fileFromLocalUri.absolutePath) {
                                    download.file = fileFromLocalUri
                                    downloadsRepository.save(download)
                                }
                            }
                            if (!download.file.exists()) throw DownloadException("Heruntergeladene Datei nicht gefunden")
                            when (download.type) {
                                DownloadType.UPDATE -> {
                                    downloadsRepository.delete(download)
                                    return Result.success()
                                }
                                else -> {
                                    val downloadable = getDownloadable(download)

                                    //Dateigröße und Hash überprüfen
                                    checkDownloadedFile(download.file, downloadable)

                                    //Zielverzeichnis anlegen
                                    targetDir = getTargetDir(download)

                                    //Entpacken
                                    download.progress = 0
                                    download.state = DownloadState.EXTRACTING
                                    downloadsRepository.save(download)
                                    extractDownload(download, targetDir)

                                    //Extraktion überprüfen
                                    download.state = DownloadState.CHECKING
                                    download.progress = 100
                                    downloadsRepository.save(download)
                                    checkFilesInTargetDir(download, targetDir)

                                    //Ist der Worker noch aktuell?
                                    if (isStopped) {
                                        throw DownloadException("Download-Verarbeitung abgebrochen",true)
                                    }

                                    //Überprüfung erfolgreich
                                    download.state = DownloadState.READY
                                    download.progress = 100
                                    download.downloadManagerId = 0
                                    downloadsRepository.save(download)
                                    if (download.type == DownloadType.PAPER) {
                                        notificationUtils.showDownloadFinishedNotification(downloadable as Paper)
                                    }
                                    EventBus.getDefault()
                                            .post(DownloadEvent(download))
                                    return Result.success()
                                }
                            }
                        }
                        else -> {
                            // Other SystemDownloadManagerResult than Success Or Failure
                            // do nothing
                            return Result.success()
                        }
                    }
                }
                else -> {
                    throw DownloadException("Unbekannte Aktion: $action")
                }
            }
        } catch (e: DownloadException) {
            e(e)
            download?.let {
                download.file.deleteQuietly()
                downloadsRepository.delete(download)
                if (!e.quiet) {
                    EventBus.getDefault()
                            .post(DownloadEvent(download, e.localizedMessage))
                }
            }
            targetDir?.deleteQuietly()
        }
        return Result.failure()
    }


    private fun getDownload(): Download {
        return downloadsRepository.getByWorkerUuid(id) ?: throw DownloadException("no internal download found")
    }

    private fun getAction(): String {
        val result = inputData.getString(ARG_ACTION)
        if (result.isNullOrBlank()) throw DownloadException("no action found")
        return result
    }

    private fun getDownloadable(download: Download): Downloadable {
        return when (download.type) {
            DownloadType.PAPER -> paperRepository.getPaperWithBookId(download.key)
            DownloadType.RESOURCE -> resourceRepository.getWithKey(download.key)
            else -> throw DownloadException("Unbekannter Download Typ: ${download.type}")
        } ?: throw DownloadException("Keine Daten zu Download-Key gefunden")
    }

    private fun checkDownloadedFile(file: File, downloadable: Downloadable) {
        if (isStopped) return
        d { "checking file size… " }
        if (downloadable.len != 0L && downloadable.len != file.length()) {
            throw DownloadException("Wrong size of download. expected: ${downloadable.len}, file: ${file.length()}")
        }
        d { "checking file hash… " }
        try {
            val fileHash = HashHelper.getHash(file, HashHelper.SHA_1)
            if (!fileHash.isNullOrBlank() && fileHash != downloadable.fileHash) {
                throw DownloadException("Wrong hash of download. expected: ${downloadable.fileHash}, fileHash: $fileHash")
            }
        } catch (e: NoSuchAlgorithmException) {
            w(e)
        } catch (e: IOException) {
            throw DownloadException(e.localizedMessage)
        }
    }

    private fun getTargetDir(download: Download): File {
        val targetDir = when (download.type) {
            DownloadType.PAPER -> storageManager.getPaperDirectory(download.key)
            DownloadType.RESOURCE -> storageManager.getResourceDirectory(download.key)
            else -> null
        } ?: throw DownloadException("Kann kein Verzeichnis für Download-Typ anlegen")
        targetDir.mkdirs()
        if (!targetDir.exists()) throw DownloadException("Konnte Zielverzeichnis nicht anlegen")
        return targetDir
    }

    private fun extractDownload(download: Download, targetDir:File) {
        if (isStopped) return
        try {
            val zipFile = ZipFile(download.file)
            zipFile.use { file ->
                var compressedCount = 0L
                var compressedSizeofAll = 0L
                var uncompressedSizeofAll = 0L
                val countEntries = file.entries
                while (countEntries.hasMoreElements()) {
                    val entry = countEntries.nextElement()
                    compressedSizeofAll += entry.compressedSize
                    uncompressedSizeofAll += entry.size
                }
                d { "size of all zp entries: compressed = $compressedSizeofAll uncompressed = $uncompressedSizeofAll" }
                if (targetDir.freeSpace < uncompressedSizeofAll) throw IOException("Nicht genügend Platz zum Entpacken in ${targetDir.absolutePath}")
                val entries = file.entriesInPhysicalOrder
                while (entries.hasMoreElements()) {
                    if (isStopped) return
                    val entry = entries.nextElement()
                    val entryDestination = File(targetDir, entry.name)
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
        } catch (e: IOException) {
            throw DownloadException(e.localizedMessage)
        } finally {
            download.file.deleteQuietly()
        }
    }

    private fun checkFilesInTargetDir(download: Download, targetDir: File){
        if (isStopped) return
        val plistFile = when (download.type) {
            DownloadType.PAPER -> File(targetDir, Paper.CONTENT_PLIST_FILENAME)
            DownloadType.RESOURCE -> File(targetDir, Resource.SHA1_PLIST)
            else -> throw DownloadException("Kein PList-File zur Überprüfung für Download-Typ")
        }
        if (!plistFile.exists()) throw DownloadException("Plist-Datei nicht vorhanden: ${plistFile.absolutePath}")
        d { "parsing plist for HashVals…" }
        try {
            val root = PropertyListParser.parse(plistFile) as NSDictionary
            val hashValsDict = root.objectForKey("HashVals") as NSDictionary
            hashValsDict.entries.forEach {
                if (isStopped) return
                if (!it.key.isNullOrBlank()) {
                    val checkFile = File(targetDir, it.key)
                    d { "checking file ${checkFile.absolutePath}" }
                    if (!checkFile.exists()) throw FileNotFoundException("${checkFile.absolutePath} not found")
                    else {
                        try {
                            if (!HashHelper.verifyHash(checkFile, (it.value as NSString).content, HashHelper.SHA_1))
                                throw IOException("Falscher Hash-Wert für Datei " + checkFile.name)
                        } catch (e: NoSuchAlgorithmException) {
                            w(e)
                        }

                    }
                }
            }
        } catch (e: Exception) {
            throw DownloadException(e.localizedMessage)
        }
    }



    class DownloadException(message: String, val quiet:Boolean=false) : Exception(message)

}