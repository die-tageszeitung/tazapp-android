package de.thecode.android.tazreader.download;

/**
 * Created by mate on 17.03.2015.
 */
public class PaperDeletedEvent {
    long paperId;

    public PaperDeletedEvent(long paperId) {
        this.paperId = paperId;
    }

    public long getPaperId() {
        return paperId;
    }
}
