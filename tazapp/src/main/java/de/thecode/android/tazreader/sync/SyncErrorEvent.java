package de.thecode.android.tazreader.sync;

/**
 * Created by mate on 11.10.2017.
 */

public class SyncErrorEvent {

    private String message;

    public SyncErrorEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
