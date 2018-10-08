package de.thecode.android.tazreader.worker;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public abstract class LoggingWorker extends Worker {

    public LoggingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Timber.d("starting work for %s",this);
        Result result = doBackgroundWork();
        Timber.d("finished work with %s for %s", result,this);
        return result;
    }

    @NonNull
    public abstract Result doBackgroundWork();


    @Override
    public void onStopped(boolean cancelled) {
        super.onStopped(cancelled);
        Timber.d("Stopped called (canceled: %b) for %s",cancelled,this);
    }

    @Override
    public String toString() {
        return "Worker{id="+getId()+", tags="+getTags()+", inputData="+getInputData()+"}";
    }
}
