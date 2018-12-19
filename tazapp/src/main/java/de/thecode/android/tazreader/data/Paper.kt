package de.thecode.android.tazreader.data

class PaperWithDownloadState:Paper() {
    var downloadState: DownloadState = DownloadState.NONE

    override fun equals(other: Any?): Boolean {
        var equals = super.equals(other)
        if (equals) {
            equals = downloadState == (other as PaperWithDownloadState).downloadState
        }
        return equals
    }
}