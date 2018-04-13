package de.thecode.android.tazreader.utils;

import android.os.AsyncTask;



/**
 * Created by mate on 20.04.2015.
 */
public abstract class AsyncTaskWithException<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private Exception exception;

    public abstract Result doInBackgroundWithException(Params... params) throws Exception;


    @Override
    final protected Result doInBackground(Params... params) {
        try {
            return doInBackgroundWithException(params);
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    @Override
    final protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        if (exception != null)
            onPostError(exception);
        else
            onPostSuccess(result);
    }


    abstract protected void onPostError(Exception exception);
    abstract protected void onPostSuccess(Result result);
}
