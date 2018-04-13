package de.thecode.android.tazreader.download;

/**
 * Created by mate on 17.03.2015.
 */
public class PaperDeletedEvent {
    private final String bookId;

    public PaperDeletedEvent(String bookId) {
        this.bookId = bookId;
    }

    public String getBookId() {
        return bookId;
    }
}
