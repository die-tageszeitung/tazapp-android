package de.thecode.android.tazreader.reader.article;

import android.view.MotionEvent;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.ReaderActivity.DIRECTIONS;
import de.thecode.android.tazreader.utils.Log;

public class TopLinkFragment extends ArticleFragment {
    public TopLinkFragment() { super(); }


    @Override
    public void onSwipeBottom(ArticleWebView view, MotionEvent e1, MotionEvent e2) {
        Log.v();
        mCallback.onLoadPrevArticle(DIRECTIONS.TOP,"0");
    }

    @Override
    public void onSwipeTop(ArticleWebView view, MotionEvent e1, MotionEvent e2) {
        Log.v();
        mCallback.onLoadNextArticle(DIRECTIONS.BOTTOM,"0");
    }

    @Override
    public void onSwipeLeft(ArticleWebView view, MotionEvent e1, MotionEvent e2) {
        Log.v();
        mCallback.onLoadNextArticle(DIRECTIONS.RIGHT,"0");
    }

    @Override
    public void onSwipeRight(ArticleWebView view, MotionEvent e1, MotionEvent e2) {
        Log.v();
        String position = "0";
        if (!TazSettings.getPrefBoolean(mContext, TazSettings.PREFKEY.ISSCROLL, false))
            position = "EOF";
        mCallback.onLoadPrevArticle(DIRECTIONS.LEFT,position);
    }

}
