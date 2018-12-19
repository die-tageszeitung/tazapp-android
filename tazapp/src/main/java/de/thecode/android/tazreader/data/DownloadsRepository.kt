package de.thecode.android.tazreader.data

import android.content.Context
import androidx.annotation.WorkerThread
import de.thecode.android.tazreader.room.AppDatabase

class DownloadsRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: DownloadsRepository? = null

        fun getInstance(context: Context): DownloadsRepository {
            return INSTANCE ?: synchronized(this) {
                DownloadsRepository(context)
            }
        }
    }

    private val appDatabase: AppDatabase = AppDatabase.getInstance(context)

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
    fun save(download: Download) {
        appDatabase.downloadsDao()
                .insert(download)
    }

    @WorkerThread
    fun delete(key: String){
        appDatabase.downloadsDao().deleteByKey(key)
    }
}