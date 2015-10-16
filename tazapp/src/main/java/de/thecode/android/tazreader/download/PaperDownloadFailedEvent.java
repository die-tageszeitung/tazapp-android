package de.thecode.android.tazreader.download;

/**
 * Created by mate on 20.03.2015.
 */
public class PaperDownloadFailedEvent {
    long paperId;
    Exception exception;

    public PaperDownloadFailedEvent(long paperId, Exception exception) {
        this.paperId = paperId;
        this.exception = exception;
    }

    public long getPaperId() {
        return paperId;
    }

    public Exception getException() {
        return exception;
    }
}
