package de.thecode.android.tazreader.utils


import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.github.ajalt.timberkt.d
import de.thecode.android.tazreader.app
import timber.log.Timber
import java.io.File
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.log10
import kotlin.math.pow


class FileUtils {
    companion object {
        fun readableSize(size: Long): String {
            if (size <= 0) return "0"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
            return (DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))
                    + units[digitGroups])
        }
    }
}

fun File.folderSize(): Long {

    if (!this.isDirectory) return this.parentFile.folderSize()

    val size = AtomicLong(0)

    try {
        this.listFiles()
                .forEach {
                    if (it != null) {
                        if (it.isDirectory) {
                            size.addAndGet(it.folderSize())
                        } else {
                            try {
                                size.addAndGet(it.length())
                            } catch (e: Exception) {
                                Timber.e(e)
                            }
                        }
                    }
                }
    } catch (e: Exception) {
        Timber.e(e)
    }
    return size.get()
}

fun File.folderSizeReadable(): String {
    return FileUtils.readableSize(this.folderSize())
}

fun File.deleteQuietly() {
    try {
        this.deleteRecursively()
    } catch (e: Exception) {
        Timber.w(e)
    }
}



fun Context.getResourceIdByName(name: String, defType: String): Int {
    d { "$defType resource by name: $name" }
    return this.resources.getIdentifier(name, defType, this.packageName)
}

fun Context.getStringIdByName(name: String): Int {
    return this.getResourceIdByName(name,"string")
}

fun Uri.getFileFromUri(): File? {
        var filePath: String? = this.path
        if ("content" == this.scheme) {
            val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
            val contentResolver = app.contentResolver
            val cursor = contentResolver.query(this, filePathColumn, null, null, null)
            // For Kotlin Beginners: cursor? nullsafe + use: auto close after use
            cursor?.use { it ->
                it.moveToFirst()
                val columnIndex = it.getColumnIndex(filePathColumn[0])
                filePath = it.getString(columnIndex)
            }
        }
        return File(filePath)
}