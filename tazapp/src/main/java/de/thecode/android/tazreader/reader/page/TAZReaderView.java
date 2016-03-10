package de.thecode.android.tazreader.reader.page;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.artifex.mupdfdemo.PageView;
import com.artifex.mupdfdemo.ReaderView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.reader.IReaderCallback;


public class TAZReaderView extends ReaderView {

    private static final Logger log = LoggerFactory.getLogger(TAZReaderView.class);

    private boolean tapDisabled = false;
    private IReaderCallback mReaderCallback;


    public TAZReaderView(Context context) {
        super(context);
        log.debug("");
        init(context);
    }

    public TAZReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        log.debug("");
        init(context);
    }

    public TAZReaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        log.debug("");
        init(context);
    }

    private void init(Context context) {
        if (!isInEditMode()) mReaderCallback = (IReaderCallback) context;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        log.debug("");
        if (!tapDisabled) {

            TAZPageView pageView = (TAZPageView) getDisplayedView();
            pageView.passClickEvent(e.getX(), e.getY());

        }

        return super.onSingleTapUp(e);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        log.debug("");
        return super.onDown(e);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //Log.v(); to much
        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        log.debug("");
        return super.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        log.debug("");

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
        log.debug("i: {}, v: {}",i, v);

    }

    @Override
    protected void onMoveToChild(int i) {
        log.debug("i: {}",i);
        Page page = (Page) getAdapter().getItem(i);
        mReaderCallback.updateIndexes(page.getKey(), "0");

    }

    @Override
    protected void onMoveOffChild(int i) {
        log.debug("i: {}",i);

    }

    @Override
    protected void onSettle(View v) {

        log.debug("v: {}",v);
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
       log.debug("v: {}",v);
        // When something changes making the previous settled view
        // no longer appropriate, tell the page to remove HQ
        ((TAZPageView) v).removeHq();
    }

    @Override
    protected void onNotInUse(View v) {
        log.debug("v: {}",v);
        ((TAZPageView) v).releaseResources();
    }

    @Override
    protected void onScaleChild(View v, Float scale) {
        log.debug("v: {}, scale: {}",v, scale);
        ((TAZPageView) v).setScale(scale);
    }


    public void resetScale() {
        mScale = 1.0F;
    }


    public IReaderCallback getReaderCallback() {
        return mReaderCallback;
    }


}
