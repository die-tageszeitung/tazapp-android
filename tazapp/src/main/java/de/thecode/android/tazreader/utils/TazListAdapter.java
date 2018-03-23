package de.thecode.android.tazreader.utils;

import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.AsyncDifferConfig;
import android.support.v7.recyclerview.extensions.AsyncListDiffer;
import android.support.v7.util.AdapterListUpdateCallback;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;

import java.util.List;

/**
 * Created by mate on 19.03.18.
 */

public abstract class TazListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private final AsyncListDiffer<T> mHelper;

    @SuppressWarnings("unused")
    public TazListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        mHelper = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                                        new AsyncDifferConfig.Builder<>(diffCallback).build());
    }

    @SuppressWarnings("unused")
    public TazListAdapter(@NonNull AsyncDifferConfig<T> config) {
        mHelper = new AsyncListDiffer<>(new AdapterListUpdateCallback(this), config);
    }

    public AsyncListDiffer<T> getHelper() {
        return mHelper;
    }

    /**
     * Submits a new list to be diffed, and displayed.
     * <p>
     * If a list is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param list The new list to be displayed.
     */
    @SuppressWarnings("WeakerAccess")
    public void submitList(List<T> list) {
        mHelper.submitList(list);
    }

    @SuppressWarnings("unused")
    public T getItem(int position) {
        return mHelper.getCurrentList()
                      .get(position);
    }

    @Override
    public int getItemCount() {
        return mHelper.getCurrentList()
                      .size();
    }

    public int indexOf(T item){
        return mHelper.getCurrentList().indexOf(item);
    }
}
