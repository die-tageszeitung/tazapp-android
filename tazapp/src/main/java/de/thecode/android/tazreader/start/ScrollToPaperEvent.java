package de.thecode.android.tazreader.start;

/**
 * Created by mate on 27.03.2015.
 */
public class ScrollToPaperEvent {
    long paperId;

    public ScrollToPaperEvent(long paperId) {
        this.paperId = paperId;
    }

    public long getPaperId() {
        return paperId;
    }
}
