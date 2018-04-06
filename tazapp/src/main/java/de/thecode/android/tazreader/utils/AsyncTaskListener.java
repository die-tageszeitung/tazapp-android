package de.thecode.android.tazreader.utils;

public class AsyncTaskListener<PARAM,RESULT> extends AsyncTaskWithExecption<PARAM,Void,RESULT>{

    private final OnExecute<PARAM,RESULT> executor;
    private final OnSuccess<RESULT> successor;
    private final OnError errorHandler;

    public AsyncTaskListener(OnExecute<PARAM, RESULT> executor) {
        this(executor,null,null);
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
            PARAM parameter = null;
            if (params != null && params.length > 0) parameter = params[0];
            return executor.execute(parameter);
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

    public interface OnExecute<PARAM, RESULT> {
        RESULT execute(PARAM param) throws Exception;
    }

    public interface OnSuccess<RESULT> {
        void onSuccess(RESULT result);
    }

    public interface OnError{
        void onError(Exception exception);
    }
}
