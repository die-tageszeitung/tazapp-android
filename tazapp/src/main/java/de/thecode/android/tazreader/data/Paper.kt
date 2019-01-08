package de.thecode.android.tazreader.data

class PaperWithDownloadState:Paper() {
    var downloadState: DownloadState = DownloadState.NONE
    var progress = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaperWithDownloadState) return false
        if (!super.equals(other)) return false

        if (downloadState != other.downloadState) return false
        if (progress != other.progress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + downloadState.hashCode()
        result = 31 * result + progress
        return result
    }
}