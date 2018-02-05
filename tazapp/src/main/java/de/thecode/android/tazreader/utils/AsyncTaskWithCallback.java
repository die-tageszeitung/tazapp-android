package de.thecode.android.tazreader.utils;

import android.os.AsyncTask;

/**
 * Created by mate on 15.01.2018.
 */

public abstract class AsyncTaskWithCallback<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private Exception exception;
    Params[] params;

    protected abstract Result doInBackgroundWithException(Params... params) throws Exception;


    @Override
    final protected Result doInBackground(Params... params) {
        try {
            this.params = params;
            return doInBackgroundWithException(params);
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    @Override
    final protected void onPostExecute(Result result) {
        super.onPostExecute(result);

        if (callback != null) {
            if (exception != null) callback.onError(exception);
            else callback.onData(result);
        }

    }

    private Callback<Result> callback;

    public void setCallback(Callback<Result> callback) {
        this.callback = callback;
    }

    public interface Callback<Result> {
        void onData(Result result);
        void onError(Exception exception);
    }
}
