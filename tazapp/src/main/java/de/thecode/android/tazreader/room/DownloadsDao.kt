package de.thecode.android.tazreader.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import de.thecode.android.tazreader.data.Download
import de.thecode.android.tazreader.data.DownloadType
import java.util.*

@Dao
interface DownloadsDao : BaseDao<Download> {
    @Query("SELECT * FROM DOWNLOADS WHERE `key` = :key")
    fun getDownloadByKey(key: String): Download?

    @Query("SELECT * FROM DOWNLOADS WHERE `key` = :key")
    fun getDownloadByKeyLive(key: String): LiveData<Download>

    @Query("SELECT * FROM DOWNLOADS WHERE downloadManagerId = :id")
    fun getDownloadById(id: Long): Download?

    @Query("SELECT * FROM DOWNLOADS WHERE type = :type")
    fun getByType(type: DownloadType): List<Download>

    @Query("DELETE FROM DOWNLOADS WHERE `key` = :key")
    fun deleteByKey(key: String)

    @Query("SELECT * FROM DOWNLOADS WHERE workerUuid = :uuid")
    fun getByWorkerUuid(uuid: UUID): Download?
}