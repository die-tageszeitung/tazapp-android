package de.thecode.android.tazreader.download;

/**
 * Created by mate on 17.03.2015.
 */
public class CoverDownloadedEvent{
    long paperId;

    public CoverDownloadedEvent(long paperId) {
        this.paperId = paperId;
    }

    public long getPaperId() {
        return paperId;
    }
}
