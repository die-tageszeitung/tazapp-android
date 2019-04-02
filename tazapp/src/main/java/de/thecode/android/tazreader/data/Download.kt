package de.thecode.android.tazreader.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.app
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
                    var state: DownloadState = DownloadState.NONE,
                    var unmeteredOnly: UnmeteredDownloadOnly?) {
    companion object {
        fun create(type: DownloadType, key: String, title: String, file: File): Download {
            return Download(type = type, key = key, title = title, file = file, unmeteredOnly = UnmeteredDownloadOnly.NO)
        }
    }
}

class UnmeteredDownloadOnlyConverter {
    @TypeConverter
    fun toBoolean(value: UnmeteredDownloadOnly): Boolean? {
        return value.booleanValue
    }
    @TypeConverter
    fun toWifiOnlyEnum(value: Boolean?): UnmeteredDownloadOnly {
        return UnmeteredDownloadOnly.fromBoolean(value)
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

enum class UnmeteredDownloadOnly(val booleanValue: Boolean?, @StringRes val resiId: Int) {
    UNKNOWN(null, R.string.unmetered_only_unknown),
    YES(true, R.string.unmetered_only_yes),
    NO(false, R.string.unmetered_only_no);

    companion object {
        fun fromBoolean(bool:Boolean?): UnmeteredDownloadOnly {
            UnmeteredDownloadOnly.values().forEach {
                if (it.booleanValue == bool) return it
            }
            return UNKNOWN
        }
    }

    fun readable(): String {
        return app.getString(resiId)
    }
}

enum class DownloadType {
    PAPER, RESOURCE, UPDATE;

    companion object {
        fun getByName(name: String) = DownloadType.valueOf(name.toUpperCase())
    }
}

enum class DownloadState(@StringRes val readableId: Int) {
    NONE(R.string.download_state_none),
    DOWNLOADING(R.string.download_state_downloading),
    DOWNLOADED(R.string.download_state_downloaded),
    EXTRACTING(R.string.download_state_extracting),
    CHECKING(R.string.download_state_checking),
    READY(R.string.download_state_ready);

    companion object {
        fun getByName(name: String) = valueOf(name.toUpperCase())
    }

    fun readable(): String {
        return app.getString(readableId);
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