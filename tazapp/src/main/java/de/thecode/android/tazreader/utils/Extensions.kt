package de.thecode.android.tazreader.utils


import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Html
import android.text.Spanned
import androidx.annotation.StringRes
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

@Suppress("DEPRECATION")
fun Context.getText(@StringRes resId: Int, vararg formatArgs: Any): CharSequence {
    // First, convert any styled Spanned back to HTML strings before applying String.format. This
    // converts the styling to HTML and also does HTML escaping.
    // For other CharSequences, just do HTML escaping.
    // (Leave any other args alone.)
    val htmlFormatArgs = formatArgs.map {
        if (it is Spanned) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.toHtml(it, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
            } else {
                Html.toHtml(it)
            }
        } else if (it is CharSequence) {
            Html.escapeHtml(it)
        } else {
            it
        }
    }.toTypedArray()

    // Next, get the format string, and do the same to that.
    val formatString = getText(resId);
    val htmlFormatString = if (formatString is Spanned) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.toHtml(formatString, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            Html.toHtml(formatString)
        }
    } else {
        Html.escapeHtml(formatString)
    }

    // Now apply the String.format
    val htmlResultString = String.format(htmlFormatString, *htmlFormatArgs)

    // Convert back to a CharSequence, recovering any of the HTML styling.
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(htmlResultString, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(htmlResultString)
    }
}