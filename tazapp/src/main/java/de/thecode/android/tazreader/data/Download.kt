package de.thecode.android.tazreader.data

import androidx.room.PrimaryKey

data class Download(@PrimaryKey() val key: String, val downloadMangerId: Int)

enum class DownloadState {
    NONE,DOWNLOADING, DOWNLOADED, EXTRACTING, READY
}