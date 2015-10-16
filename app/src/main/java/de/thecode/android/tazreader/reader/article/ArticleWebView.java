package de.thecode.android.tazreader.reader.article;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.WebView;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.utils.Log;

public class ArticleWebView extends WebView {

    Context mContext;
    boolean isScroll;
    
    boolean mScrolling;

    public ArticleWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public ArticleWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ArticleWebView(Context context) {
        super(context);
        init(context);
    }

    @SuppressLint("NewApi")
    private void init(Context context) {
        mContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE))
            {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
    }

    private void setIsScroll() {
        isScroll = TazSettings.getPrefBoolean(mContext, TazSettings.PREFKEY.ISSCROLL, false);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        //Log.d(l, t, oldl, oldt);
        checkY = t;
        super.onScrollChanged(l, t, oldl, oldt);
        if (!isAlreadyChecking)
        {
            mScrolling = true;
            mCallback.onScrollStarted(this);
            isAlreadyChecking = true;
            this.postDelayed(scrollStopCheckerTask, 500);
        }
    }

    @SuppressLint("NewApi")
    public void smoothScrollToY(int y)
    {
            float density = getResources().getDisplayMetrics().density;
            ObjectAnimator scrollAnimation = ObjectAnimator.ofInt(this, "scrollY", (int) (Math.round(y * density)));
            scrollAnimation.setDuration(500);
            scrollAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            scrollAnimation.start();
    }

    boolean isAlreadyChecking = false;
    int lastCheckedY = 0;
    int checkY = 0;
    Runnable scrollStopCheckerTask = new Runnable() {

        @Override
        public void run() {
            if (checkY != lastCheckedY)
            {

                lastCheckedY = checkY;
                ArticleWebView.this.postDelayed(scrollStopCheckerTask, 500);
            }
            else
            {
                mScrolling = false;
                if (mCallback != null)
                {
                    mCallback.onScrollFinished(ArticleWebView.this);
                }
                isAlreadyChecking = false;
            }

        }
    };

    public void loadUrl(String url) {
        Log.v(url);
        gestureDetector = new GestureDetector(mContext, simpleOnGestureListener);
        setIsScroll();
        super.loadUrl(url);
    };

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String failUrl) {
        Log.v(baseUrl);
        gestureDetector = new GestureDetector(mContext, simpleOnGestureListener);
        setIsScroll();
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mScrolling)
            return gestureDetector.onTouchEvent(ev) || super.onTouchEvent(ev);
        else
            return super.onTouchEvent(ev);
    }

    private GestureDetector gestureDetector;

    private ArticleWebViewCallback mCallback;

    public void setArticleWebViewCallback(ArticleWebViewCallback listener)
    {
        mCallback = listener;
    }

    public interface ArticleWebViewCallback
    {

        public void onScrollStarted(ArticleWebView view);

        public void onScrollFinished(ArticleWebView view);

        public void onSwipeRight(ArticleWebView view, MotionEvent e1, MotionEvent e2);

        public void onSwipeLeft(ArticleWebView view, MotionEvent e1, MotionEvent e2);

        public void onSwipeBottom(ArticleWebView view, MotionEvent e1, MotionEvent e2);

        public void onSwipeTop(ArticleWebView view, MotionEvent e1, MotionEvent e2);
    }

    GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            if (mCallback != null)
                                mCallback.onSwipeRight(ArticleWebView.this,e1,e2);
                        } else {
                            if (mCallback != null)
                                mCallback.onSwipeLeft(ArticleWebView.this,e1,e2);
                        }
                    }
                    result = true;
                }
                else if (!isScroll && Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        if (mCallback != null)
                            mCallback.onSwipeBottom(ArticleWebView.this,e1,e2);
                    } else {
                        if (mCallback != null)
                            mCallback.onSwipeTop(ArticleWebView.this,e1,e2);
                    }
                    result = true;
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    };
}
