package de.thecode.android.tazreader.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import timber.log.Timber;

/**
 * Created by mate on 09.02.2015.
 */
public class AutofitRecyclerView extends RecyclerView {

    private AutoFitGridLayoutManager manager;
    private int columnWidth = -1;

    public AutofitRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public AutofitRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AutofitRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            int[] attrsArray = {
                    android.R.attr.columnWidth
            };
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            columnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }

        manager = new AutoFitGridLayoutManager(getContext(), 1);
        setLayoutManager(manager);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (columnWidth > 0) {
            int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
            manager.setSpanCount(spanCount);
        }
    }

    public int findFirstVisibleItemPosition() {
        return getLayoutManager().findFirstVisibleItemPosition();
    }

    public int findFirstCompletelyVisibleItemPosition() {
        return getLayoutManager().findFirstCompletelyVisibleItemPosition();
    }
    public int findLastVisibleItemPosition() {
        return getLayoutManager().findLastVisibleItemPosition();
    }
    public int findLastCompletelyVisibleItemPosition() {
        return getLayoutManager().findLastCompletelyVisibleItemPosition();
    }

    @Override
    public AutoFitGridLayoutManager getLayoutManager() {
        return manager;
    }

    public class AutoFitGridLayoutManager extends GridLayoutManager {

        public AutoFitGridLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                                           int position) {

            int newPosition = position;
            int first = findFirstCompletelyVisibleItemPosition();

            if (first < position) {


                int last = findLastCompletelyVisibleItemPosition();
                double dif = last - first;
                double spanCount = getSpanCount();
                double completeVisibleRows = Math.ceil(dif / spanCount);
                newPosition = position + (int) (completeVisibleRows * spanCount - (spanCount - 1));
                if (newPosition > getItemCount()) newPosition = getItemCount();

               Timber.d("%s %s %s %s %s %s %s %s", position, first, last, dif, spanCount, completeVisibleRows, newPosition, getItemCount());
            }

            super.smoothScrollToPosition(recyclerView,state,newPosition);
        }

    }
}
