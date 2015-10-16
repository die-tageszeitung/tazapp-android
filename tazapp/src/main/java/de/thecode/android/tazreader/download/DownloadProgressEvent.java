package de.thecode.android.tazreader.download;

/**
 * Created by mate on 11.02.2015.
 */
public class DownloadProgressEvent {

    long paperId;
    int progress;

    public DownloadProgressEvent(long paperId, int progress) {
        this.paperId = paperId;
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public long getPaperId() {
        return paperId;
    }

}