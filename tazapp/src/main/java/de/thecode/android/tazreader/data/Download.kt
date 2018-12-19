package de.thecode.android.tazreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "DOWNLOADS")


data class Download(@PrimaryKey()
                    val key: String,
                    var type: DownloadType = DownloadType.UNKNOWN,
                    var downloadManagerId: Long = 0,
                    var state: DownloadState = DownloadState.NONE)

class DownloadStateTypeConverter {
    @TypeConverter
    fun toDownloadState(value: String?): DownloadState {
        val state = value?.let { DownloadState.getByName(it) }
        if (state != null) return state
        return DownloadState.NONE
    }

    @TypeConverter
    fun toString(value: DownloadState): String {
        return value.toString()
    }

}

class DownloadTypeTypeConverter {
    @TypeConverter
    fun toDownloadType(value: String): DownloadType {
        return DownloadType.getByName(value)
    }

    @TypeConverter
    fun toString(value: DownloadType): String {
        return value.toString()
    }

}

enum class DownloadType {
    UNKNOWN, PAPER, RESOURCE;

    companion object {
        fun getByName(name: String) = DownloadType.valueOf(name.toUpperCase())
    }
}

enum class DownloadState {
    NONE, DOWNLOADING, DOWNLOADED, EXTRACTING, READY;

    companion object {
        fun getByName(name: String) = valueOf(name.toUpperCase())
    }
}