package de.thecode.android.tazreader.data

import androidx.annotation.WorkerThread
import com.github.ajalt.timberkt.d
import de.thecode.android.tazreader.app
import de.thecode.android.tazreader.room.AppDatabase
import java.util.*

class DownloadsRepository private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: DownloadsRepository? = null

        fun getInstance(): DownloadsRepository {
            return INSTANCE ?: synchronized(this) {
                DownloadsRepository()
            }
        }
    }

    private val appDatabase: AppDatabase = AppDatabase.getInstance(app)

    @WorkerThread
    fun get(key: String): Download? {
        return appDatabase.downloadsDao()
                .getDownloadByKey(key)
    }

    @WorkerThread
    fun get(id: Long): Download? {
        return appDatabase.downloadsDao()
                .getDownloadById(id)
    }

    @WorkerThread
    fun getByWorkerUuid(uuid: UUID): Download? {
        return appDatabase.downloadsDao().getByWorkerUuid(uuid)
    }

    @WorkerThread
    fun get(type: DownloadType): List<Download> {
        return appDatabase.downloadsDao()
                .getByType(type)
    }

    @WorkerThread
    fun save(download: Download) {
        d {
            "saving $download"
        }
        appDatabase.downloadsDao()
                .insert(download)
    }

    @WorkerThread
    fun delete(key: String) {
        d {
            "deleting $key"
        }
        appDatabase.downloadsDao()
                .deleteByKey(key)
    }

    @WorkerThread
    fun delete(download: Download) {
        appDatabase.downloadsDao()
                .delete(download)
    }



}