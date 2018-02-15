package de.thecode.android.tazreader.utils;

import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * Created by mate on 14.02.2018.
 */

public class ReadableException extends Exception {

    public ReadableException() {
    }

    public ReadableException(String message) {
        super(message);
    }

    public ReadableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadableException(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ReadableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }


    public String toString() {
        String s = getClass().getSimpleName();
        String message = getLocalizedMessage();
        return (message != null) ? (s + " - " + message) : s;
    }

}
