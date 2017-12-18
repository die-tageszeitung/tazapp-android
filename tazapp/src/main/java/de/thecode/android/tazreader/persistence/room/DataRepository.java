package de.thecode.android.tazreader.persistence.room;

import android.content.Context;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;

/**
 * Created by mate on 18.12.2017.
 */

public class DataRepository {


    private static volatile DataRepository mInstance;

    private TazappDatabase database;

    private DataRepository(Context context) {
        database = TazappDatabase.getInstance(context);
    }

    public static DataRepository getInstance(Context context) {
        if (mInstance == null) {
            synchronized (DataRepository.class) {
                if (mInstance == null) {
                    mInstance = new DataRepository(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    public void getPaperByBookId(String bookId, DataRepositoryListener<Paper> listener) {

        DataRepositoryTask<String, Paper> task = new DataRepositoryTask<>(parameter -> database.paperDao()
                                                                                               .getPaperWithBookId(parameter),
                                                                          listener);
        task.execute(bookId);
    }


    private static class DataRepositoryTask<T, V> extends AsyncTaskWithExecption<T, Void, V> {

        private final DataRepositoryExecutionInterface<T, V> executionInterface;
        private final DataRepositoryListener<V>              listener;

        private DataRepositoryTask(DataRepositoryExecutionInterface<T, V> executionInterface,
                                   DataRepositoryListener<V> listener) {
            this.executionInterface = executionInterface;
            this.listener = listener;
        }

        @Override
        public V doInBackgroundWithException(T[] ts) throws Exception {
            if (ts != null && ts.length > 0) {
                return executionInterface.execute(ts[0]);
            }
            return null;
        }

        @Override
        protected void onPostError(Exception exception) {
            if (listener != null) listener.onError(exception);
        }

        @Override
        protected void onPostSuccess(V v) {
            if (listener != null) listener.onData(v);
        }
    }

    public interface DataRepositoryListener<T> {
        void onData(T data);

        void onError(Exception e);
    }

    private interface DataRepositoryExecutionInterface<T, V> {
        V execute(T parameter);
    }
}
