package de.thecode.android.tazreader.utils.extendedasyncdiffer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

public class ExtendedAdapterListUpdateCallback implements ListUpdateCallback {

    private int firstInserted = -1;

    @NonNull
    private final RecyclerView.Adapter mAdapter;

    private final OnFirstInsertedListener mListener;

    public ExtendedAdapterListUpdateCallback(@NonNull RecyclerView.Adapter mAdapter, OnFirstInsertedListener listener) {
        this.mAdapter = mAdapter;
        mListener = listener;
    }

    public ExtendedAdapterListUpdateCallback(@NonNull RecyclerView.Adapter mAdapter) {
        this(mAdapter,null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInserted(int position, int count) {
        if (firstInserted == -1 || position < firstInserted) firstInserted = position;
        mAdapter.notifyItemRangeInserted(position, count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemoved(int position, int count) {
        mAdapter.notifyItemRangeRemoved(position, count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMoved(int fromPosition, int toPosition) {
        mAdapter.notifyItemMoved(fromPosition, toPosition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChanged(int position, int count, Object payload) {
        mAdapter.notifyItemRangeChanged(position, count, payload);
    }

    public interface OnFirstInsertedListener {
        void onFinished(int firstInserted);
    }

    void resetCounters() {
        firstInserted = -1;
    }

    void callListener(){
        if (mListener != null && firstInserted != -1) mListener.onFinished(firstInserted);
    }
}
