package de.thecode.android.tazreader.timber;

import android.util.Log;

import androidx.annotation.NonNull;

import timber.log.Timber;

/**
 * Created by mate on 02.03.2017.
 */

public class TazTimberTree extends Timber.DebugTree {

    private final int mLogPriority;

    public TazTimberTree() {
        this(Log.INFO);
    }

    public TazTimberTree(int logPriority) {
        mLogPriority = logPriority;
    }


    @Override
    protected boolean isLoggable(String tag, int priority) {
        return priority >= mLogPriority && super.isLoggable(tag, priority);
    }

    @Override
    protected String createStackElementTag(@NonNull StackTraceElement element) {
        return super.createStackElementTag(
                element) + "." + element.getMethodName() + ":" + element.getLineNumber() + "[" + Thread.currentThread()
                                                                                                       .getName() + "]";
    }

    @Override
    public void e(String message, Object... args) {
        super.e(message, args);
    }
}
