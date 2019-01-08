package de.thecode.android.tazreader.data

class ResourceWithDownloadState:Resource() {
    var downloadState: DownloadState = DownloadState.NONE

    override fun equals(other: Any?): Boolean {
        var equals = super.equals(other)
        if (equals) {
            equals = downloadState == (other as ResourceWithDownloadState).downloadState
        }
        return equals
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + downloadState.hashCode()
        return result
    }
}