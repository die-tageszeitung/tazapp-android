package de.thecode.android.tazreader.download;

import de.thecode.android.tazreader.data.Paper;

/**
 * Created by mate on 20.03.2015.
 */
public class PaperDownloadFailedEvent {
    final Paper     paper;
    final Exception exception;

    public PaperDownloadFailedEvent(Paper paper, Exception exception) {
        this.paper = paper;
        this.exception = exception;
    }

    public Paper getPaper() {
        return paper;
    }

    public Exception getException() {
        return exception;
    }
}
