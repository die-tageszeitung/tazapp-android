package de.thecode.android.tazreader.data

import androidx.annotation.WorkerThread
import de.thecode.android.tazreader.app
import de.thecode.android.tazreader.room.AppDatabase

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
    fun get(key: String): Download {
        var download = appDatabase.downloadsDao()
                .getDownloadByKey(key)
        if (download == null) download = Download(key)
        return download
    }

    @WorkerThread
    fun get(id: Long): Download? {
        return appDatabase.downloadsDao()
                .getDownloadById(id)
    }

    @WorkerThread
    fun get(type: DownloadType): List<Download> {
        return appDatabase.downloadsDao()
                .getByType(type)
    }

    @WorkerThread
    fun save(download: Download) {
        appDatabase.downloadsDao()
                .insert(download)
    }

    @WorkerThread
    fun delete(key: String) {
        appDatabase.downloadsDao()
                .deleteByKey(key)
    }

    @WorkerThread
    fun delete(download: Download) {
        appDatabase.downloadsDao()
                .delete(download)
    }

}