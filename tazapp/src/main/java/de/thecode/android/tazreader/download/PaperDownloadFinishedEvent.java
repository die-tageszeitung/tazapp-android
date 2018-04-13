package de.thecode.android.tazreader.download;

/**
 * Created by mate on 11.02.2015.
 */
public class PaperDownloadFinishedEvent {

    private final String bookId;

    public PaperDownloadFinishedEvent(String bookId) {
        this.bookId = bookId;
    }

    public String getBookId() {
        return bookId;
    }
}