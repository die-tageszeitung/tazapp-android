package de.thecode.android.tazreader.download;

/**
 * Created by mate on 21.05.2015.
 */
public class ResourceDownloadEvent {
    String key;
    Exception exception;

    public ResourceDownloadEvent(String key, Exception exception) {
        this.key = key;
        this.exception = exception;
    }

    public ResourceDownloadEvent(String key) {
        this.key = key;
    }

    public boolean isSuccessful() {
        if (exception == null)
            return true;
        return false;
    }

    public Exception getException() {
        return exception;
    }

    public String getKey() {
        return key;
    }
}
