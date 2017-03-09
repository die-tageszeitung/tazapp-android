package de.thecode.android.tazreader.reader.page;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.artifex.mupdfdemo.PageView;
import com.artifex.mupdfdemo.ReaderView;

import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.reader.IReaderCallback;

import timber.log.Timber;


public class TAZReaderView extends ReaderView {

    private boolean tapDisabled = false;
    private IReaderCallback mReaderCallback;


    public TAZReaderView(Context context) {
        super(context);

        init(context);
    }

    public TAZReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public TAZReaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {
        if (!isInEditMode()) mReaderCallback = (IReaderCallback) context;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {

        if (!tapDisabled) {

            TAZPageView pageView = (TAZPageView) getDisplayedView();
            pageView.passClickEvent(e.getX(), e.getY());

        }

        return super.onSingleTapUp(e);
    }

    @Override
    public boolean onDown(MotionEvent e) {

        return super.onDown(e);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //Log.v(); to much
        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        return super.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {


        // Disabled showing the buttons until next touch.
        // Not sure why this is needed, but without it
        // pinch zoom can make the buttons appear
        tapDisabled = true;

        return super.onScaleBegin(detector);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Log.v(); to much

        if ((event.getAction() & event.getActionMasked()) == MotionEvent.ACTION_DOWN) {
            tapDisabled = false;
        }

        return super.onTouchEvent(event);
    }


    @Override
    protected void onChildSetup(int i, View v) {
        Timber.d("i: %s, v: %s",i, v);

    }

    @Override
    protected void onMoveToChild(int i) {
        Timber.d("i: %s",i);
        Page page = (Page) getAdapter().getItem(i);
        mReaderCallback.updateIndexes(page.getKey(), "0");

    }

    @Override
    protected void onMoveOffChild(int i) {
        Timber.d("i: %s", i);

    }

    @Override
    protected void onSettle(View v) {

        Timber.d("v: %s",v);
        // When the layout has settled ask the page to render
        // in HQ

        if (v instanceof TAZPageView) {
            if (((TAZPageView)v).mCore != null) {
                if (((TAZPageView) v).mCore.isDestroyed) return;
            }
        }
        ((PageView) v).updateHq(false);
    }

    @Override
    protected void onUnsettle(View v) {
       Timber.d("v: %s",v);
        // When something changes making the previous settled view
        // no longer appropriate, tell the page to remove HQ
        ((TAZPageView) v).removeHq();
    }

    @Override
    protected void onNotInUse(View v) {
        Timber.d("v: %s",v);
        ((TAZPageView) v).releaseResources();
    }

    @Override
    protected void onScaleChild(View v, Float scale) {
        Timber.d("v: %s, scale: %s",v, scale);
        ((TAZPageView) v).setScale(scale);
    }


    public void resetScale() {
        mScale = 1.0F;
    }


    public IReaderCallback getReaderCallback() {
        return mReaderCallback;
    }


}
