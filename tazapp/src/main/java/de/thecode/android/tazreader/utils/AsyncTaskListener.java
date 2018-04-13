package de.thecode.android.tazreader.utils;

import timber.log.Timber;

public class AsyncTaskListener<PARAM,RESULT> extends AsyncTaskWithException<PARAM,Void,RESULT> {

    private final OnExecute<PARAM,RESULT> executor;
    private final OnSuccess<RESULT> successor;
    private final OnError errorHandler;

    public AsyncTaskListener(OnExecute<PARAM, RESULT> executor) {
        this(executor,null,null);
    }

    public AsyncTaskListener(OnExecute<PARAM, RESULT> executor, OnSuccess<RESULT> successor) {
        this(executor,successor,null);
    }


    public AsyncTaskListener(OnExecute<PARAM, RESULT> executor, OnSuccess<RESULT> successor, OnError errorHandler) {
        this.executor = executor;
        this.successor = successor;
        this.errorHandler = errorHandler;
    }

    @SafeVarargs
    @Override
    public final RESULT doInBackgroundWithException(PARAM... params) throws Exception {
        if (executor != null) {
            return executor.execute(params);
        }
        return null;
    }

    @Override
    protected void onPostError(Exception exception) {
        if (errorHandler!= null) errorHandler.onError(exception);
    }

    @Override
    protected void onPostSuccess(RESULT result) {
        if (successor != null) successor.onSuccess(result);
    }

    @FunctionalInterface
    public interface OnExecute<PARAM, RESULT> {
        RESULT execute(PARAM... params) throws Exception;
    }

    @FunctionalInterface
    public interface OnSuccess<RESULT> {
        void onSuccess(RESULT result);

    }

    public interface OnError{
        default void onError(Exception exception){
            Timber.e(exception);
        }
    }
}
