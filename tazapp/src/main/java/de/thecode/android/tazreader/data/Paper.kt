package de.thecode.android.tazreader.data

import androidx.annotation.WorkerThread

class PaperWithDownloadState:Paper() {
    var downloadState: DownloadState = DownloadState.NONE

    override fun equals(other: Any?): Boolean {
        var equals = super.equals(other)
        if (equals) {
            equals = downloadState == (other as PaperWithDownloadState).downloadState
        }
        return equals
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + downloadState.hashCode()
        return result
    }
}

@WorkerThread
fun Paper.extract(){

}