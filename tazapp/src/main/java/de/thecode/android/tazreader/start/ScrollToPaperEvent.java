package de.thecode.android.tazreader.start;

/**
 * Created by mate on 27.03.2015.
 */
public class ScrollToPaperEvent {
    private final String bookId;

    public ScrollToPaperEvent(String bookId) {
        this.bookId = bookId;
    }

    public String getBookId() {
        return bookId;
    }
}
