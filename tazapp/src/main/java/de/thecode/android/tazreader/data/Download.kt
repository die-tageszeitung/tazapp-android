package de.thecode.android.tazreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.io.File
import java.util.*

@Entity(tableName = "DOWNLOADS")
data class Download(@PrimaryKey()
                    val key: String,
                    val title: String,
                    var file: File,
                    val type: DownloadType,
                    var downloadManagerId: Long = 0,
                    var progress: Int = 0,
                    var workerUuid: UUID? = null,
                    var state: DownloadState = DownloadState.NONE) {
    companion object {
        fun create(type: DownloadType, key: String, title: String, file: File): Download {
            return Download(type = type, key = key, title = title, file = file)
        }
    }
}

class FileTypeConverter {
    @TypeConverter
    fun toFile(value: String?): File? {
        if (value.isNullOrBlank()) return null
        return File(value)
    }

    @TypeConverter
    fun toString(value: File?): String? {
        if (value != null) return value.absolutePath
        return null
    }
}

class UuidTypeConverter {
    @TypeConverter
    fun toUuid(value: String?): UUID? {
        if (value.isNullOrBlank()) return null
        return UUID.fromString(value)
    }

    @TypeConverter
    fun toString(value: UUID?): String? {
        if (value != null) return value.toString()
        return null
    }
}

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
    PAPER, RESOURCE, UPDATE;

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

data class DownloadEvent(val download: Download,
                         var message: String? = null) {
    public val success: Boolean = download.state == DownloadState.READY
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