package de.thecode.android.tazreader.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import de.thecode.android.tazreader.utils.extendedasyncdiffer.ExtendedAsyncDifferConfig;
import de.thecode.android.tazreader.utils.extendedasyncdiffer.ExtendedAdapterListUpdateCallback;
import de.thecode.android.tazreader.utils.extendedasyncdiffer.ExtendedAsyncListDiffer;

import java.util.List;

/**
 * Created by mate on 19.03.18.
 */

public abstract class TazListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private final ExtendedAsyncListDiffer<T> mHelper;

    public TazListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        mHelper = new ExtendedAsyncListDiffer<>(new ExtendedAdapterListUpdateCallback(this),
                                                new ExtendedAsyncDifferConfig.Builder<>(diffCallback).build());
    }

    public TazListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback, ExtendedAdapterListUpdateCallback.OnFirstInsertedListener firstInsertedListener) {
        mHelper = new ExtendedAsyncListDiffer<>(new ExtendedAdapterListUpdateCallback(this, firstInsertedListener),
                                                new ExtendedAsyncDifferConfig.Builder<>(diffCallback).build());
    }


    public TazListAdapter(@NonNull ExtendedAsyncDifferConfig<T> config) {
        mHelper = new ExtendedAsyncListDiffer<>(new ExtendedAdapterListUpdateCallback(this), config);
    }

    public ExtendedAsyncListDiffer<T> getHelper() {
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
