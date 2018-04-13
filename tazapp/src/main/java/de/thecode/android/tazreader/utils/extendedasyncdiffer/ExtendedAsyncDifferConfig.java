package de.thecode.android.tazreader.utils.extendedasyncdiffer;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import android.support.v7.util.DiffUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public final class ExtendedAsyncDifferConfig<T> {
    @NonNull
    private final Executor mMainThreadExecutor;
    @NonNull
    private final Executor mBackgroundThreadExecutor;
    @NonNull
    private final DiffUtil.ItemCallback<T> mDiffCallback;

    private ExtendedAsyncDifferConfig(
            @NonNull Executor mainThreadExecutor,
            @NonNull Executor backgroundThreadExecutor,
            @NonNull DiffUtil.ItemCallback<T> diffCallback) {
        mMainThreadExecutor = mainThreadExecutor;
        mBackgroundThreadExecutor = backgroundThreadExecutor;
        mDiffCallback = diffCallback;
    }

    /** @hide */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Executor getMainThreadExecutor() {
        return mMainThreadExecutor;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Executor getBackgroundThreadExecutor() {
        return mBackgroundThreadExecutor;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public DiffUtil.ItemCallback<T> getDiffCallback() {
        return mDiffCallback;
    }

    /**
     * Builder class for {@link android.support.v7.recyclerview.extensions.AsyncDifferConfig}.
     *
     * @param <T>
     */
    public static final class Builder<T> {
        private Executor mMainThreadExecutor;
        private Executor mBackgroundThreadExecutor;
        private final DiffUtil.ItemCallback<T> mDiffCallback;

        public Builder(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
            mDiffCallback = diffCallback;
        }

        /**
         * If provided, defines the main thread executor used to dispatch adapter update
         * notifications on the main thread.
         * <p>
         * If not provided, it will default to the main thread.
         *
         * @param executor The executor which can run tasks in the UI thread.
         * @return this
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public ExtendedAsyncDifferConfig.Builder<T> setMainThreadExecutor(Executor executor) {
            mMainThreadExecutor = executor;
            return this;
        }

        /**
         * If provided, defines the background executor used to calculate the diff between an old
         * and a new list.
         * <p>
         * If not provided, defaults to two thread pool executor, shared by all ListAdapterConfigs.
         *
         * @param executor The background executor to run list diffing.
         * @return this
         */
        @SuppressWarnings({"unused", "WeakerAccess"})
        @NonNull
        public ExtendedAsyncDifferConfig.Builder<T> setBackgroundThreadExecutor(Executor executor) {
            mBackgroundThreadExecutor = executor;
            return this;
        }

        private static class MainThreadExecutor implements Executor {
            final Handler mHandler = new Handler(Looper.getMainLooper());
            @Override
            public void execute(@NonNull Runnable command) {
                mHandler.post(command);
            }
        }


        @NonNull
        public ExtendedAsyncDifferConfig<T> build() {
            if (mMainThreadExecutor == null) {
                mMainThreadExecutor = sMainThreadExecutor;
            }
            if (mBackgroundThreadExecutor == null) {
                synchronized (sExecutorLock) {
                    if (sDiffExecutor == null) {
                        sDiffExecutor = Executors.newFixedThreadPool(2);
                    }
                }
                mBackgroundThreadExecutor = sDiffExecutor;
            }
            return new ExtendedAsyncDifferConfig<>(
                    mMainThreadExecutor,
                    mBackgroundThreadExecutor,
                    mDiffCallback);
        }

        // TODO: remove the below once supportlib has its own appropriate executors
        private static final Object sExecutorLock = new Object();
        private static Executor sDiffExecutor = null;

        // TODO: use MainThreadExecutor from supportlib once one exists
        private static final Executor sMainThreadExecutor = new MainThreadExecutor();
    }
}
