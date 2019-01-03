package de.thecode.android.tazreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "DOWNLOADS")


data class Download(@PrimaryKey()
                    val key: String,
                    var type: DownloadType = DownloadType.UNKNOWN,
                    var downloadManagerId: Long = 0,
                    var progress: Int = 0,
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
    UNKNOWN, PAPER, RESOURCE, UPDATE;

    companion object {
        fun getByName(name: String) = DownloadType.valueOf(name.toUpperCase())
    }
}

enum class DownloadState {
    NONE, DOWNLOADING, DOWNLOADED, EXTRACTING, CHECKING, READY;

    companion object {
        fun getByName(name: String) = valueOf(name.toUpperCase())
    }
}


//TODO rework to data class if no JAVA class extend
abstract class Downloadable {
    @JvmField
    var fileHash: String? = null
    @JvmField
    var len: Long = 0L

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Downloadable) return false

        if (fileHash != other.fileHash) return false
        if (len != other.len) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileHash?.hashCode() ?: 0
        result = 31 * result + len.hashCode()
        return result
    }


}