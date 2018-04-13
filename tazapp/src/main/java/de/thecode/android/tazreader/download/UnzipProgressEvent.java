package de.thecode.android.tazreader.download;

/**
 * Created by mate on 11.02.2015.
 */
public class UnzipProgressEvent {

    private final String bookId;
    private final int progress;

    public UnzipProgressEvent(String bookId, int progress) {
        this.bookId = bookId;
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public String getBookId() {
        return bookId;
    }
}