package de.thecode.android.tazreader.room

import androidx.room.Query
import de.thecode.android.tazreader.data.Download

interface DownloadsDao : BaseDao<Download> {
    @Query("SELECT * FROM DOWNLOADS WHERE key LIKE :key")
    fun getDownload(key: String): Download
}