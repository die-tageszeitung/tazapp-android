package de.thecode.android.tazreader.utils


import android.content.Context
import com.github.ajalt.timberkt.d
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