package de.thecode.android.tazreader.download;

/**
 * Created by mate on 20.03.2015.
 */
public class PaperDownloadFailedEvent {
    String bookId;
    Exception exception;

    public PaperDownloadFailedEvent(String bookId, Exception exception) {
        this.bookId = bookId;
        this.exception = exception;
    }

    public String getBookId() {
        return bookId;
    }

    public Exception getException() {
        return exception;
    }
}
