package de.thecode.android.tazreader.widget;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created by mate on 19.10.2017.
 */

public class SnapLayoutManager extends LinearLayoutManager {

    private static final float MILLISECONDS_PER_INCH = 100F;

    public static final int SNAP_TO_START = LinearSmoothScroller.SNAP_TO_START;
    public static final int SNAP_TO_END = LinearSmoothScroller.SNAP_TO_END;
    public static final int SNAP_TO_CENTER = 2;

    private int snapPreference = SNAP_TO_START;

    public SnapLayoutManager(Context context) {
        super(context);
    }

    public SnapLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public SnapLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        RecyclerView.SmoothScroller smoothScroller = new CenterSmoothScroller(recyclerView.getContext(), snapPreference);
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    public void setSnapPreference(int snapPreference) {
        this.snapPreference = snapPreference;
    }

    private static class CenterSmoothScroller extends LinearSmoothScroller {

        CenterSmoothScroller(Context context, int snapPreference) {
            super(context);
            this.snapPreference = snapPreference;
        }

        private final int snapPreference;

        @Override
        protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
            super.onTargetFound(targetView, state, action);
        }

        @Override
        public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
            if (snapPreference == SNAP_TO_CENTER) return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            else
                return super.calculateDtToFit(viewStart,viewEnd,boxStart,boxEnd,snapPreference);
        }

        @Override
        protected int getVerticalSnapPreference() {
            return snapPreference;

        }

        @Override
        protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {

            return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
        }

        @Override
        protected int calculateTimeForScrolling(int dx) {
            return super.calculateTimeForScrolling(dx);
        }

        @Override
        protected int calculateTimeForDeceleration(int dx) {
            return super.calculateTimeForDeceleration(dx);
        }

        @Override
        public int calculateDyToMakeVisible(View view, int snapPreference) {
            return super.calculateDyToMakeVisible(view, snapPreference);
        }
    }
}
