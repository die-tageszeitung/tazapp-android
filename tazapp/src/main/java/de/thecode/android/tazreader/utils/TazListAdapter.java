package de.thecode.android.tazreader.utils;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;

import de.thecode.android.tazreader.utils.asyncdiffer.AsyncDifferConfig;
import de.thecode.android.tazreader.utils.asyncdiffer.ExtendedAdapterListUpdateCallback;
import de.thecode.android.tazreader.utils.asyncdiffer.FixedAsyncListDiffer;

import java.util.List;

import timber.log.Timber;

/**
 * Created by mate on 19.03.18.
 */

public abstract class TazListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private final FixedAsyncListDiffer<T> mHelper;

    public TazListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        mHelper = new FixedAsyncListDiffer<>(new ExtendedAdapterListUpdateCallback(this),
                                              new AsyncDifferConfig.Builder<>(diffCallback).build());
    }

    public TazListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback, ExtendedAdapterListUpdateCallback.OnFirstInsertedListener firstInsertedListener) {
        mHelper = new FixedAsyncListDiffer<>(new ExtendedAdapterListUpdateCallback(this,firstInsertedListener),
                                             new AsyncDifferConfig.Builder<>(diffCallback).build());
    }


    public TazListAdapter(@NonNull AsyncDifferConfig<T> config) {
        mHelper = new FixedAsyncListDiffer<>(new ExtendedAdapterListUpdateCallback(this), config);
    }

    public FixedAsyncListDiffer<T> getHelper() {
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

    public int indexOf(T item) {
        return mHelper.getCurrentList()
                      .indexOf(item);
    }

}
