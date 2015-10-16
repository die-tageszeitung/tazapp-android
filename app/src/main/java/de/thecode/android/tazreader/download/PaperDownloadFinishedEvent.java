package de.thecode.android.tazreader.download;

/**
 * Created by mate on 11.02.2015.
 */
public class PaperDownloadFinishedEvent {

    long paperId;

    public PaperDownloadFinishedEvent(long paperId) {
        this.paperId = paperId;
    }

    public long getPaperId() {
        return paperId;
    }

}