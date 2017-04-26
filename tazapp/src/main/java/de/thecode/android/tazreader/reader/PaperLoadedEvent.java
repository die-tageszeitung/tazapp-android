package de.thecode.android.tazreader.reader;

/**
 * Created by mate on 25.04.2017.
 */

public class PaperLoadedEvent {

    private Exception exception = null;

    public PaperLoadedEvent() {
    }

    public PaperLoadedEvent(Exception exception) {
        this.exception = exception;
    }

    public boolean hasError() {
        return exception != null;
    }

    public Exception getException() {
        return exception;
    }

    public void setError(Exception exception) {
        this.exception = exception;
    }
}
