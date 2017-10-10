package de.thecode.android.tazreader.job;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.util.JobLogger;

import timber.log.Timber;

/**
 * Created by mate on 10.10.2017.
 */

public class TazJobLogger implements JobLogger {

    @Override
    public void log(int priority, @NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        Timber.log(priority,t,message);
    }
}
