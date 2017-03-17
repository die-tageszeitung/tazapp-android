package de.thecode.android.tazreader.reader.article;

import android.view.MotionEvent;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.ReaderActivity.DIRECTIONS;

public class TopLinkFragment extends ArticleFragment {

    public TopLinkFragment() {
        super();
    }


    @Override
    public void onSwipeBottom(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        if (hasCallback()) getCallback().onLoadPrevArticle(DIRECTIONS.TOP, "0");
    }

    @Override
    public void onSwipeTop(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        if (hasCallback()) getCallback().onLoadNextArticle(DIRECTIONS.BOTTOM, "0");
    }

    @Override
    public void onSwipeLeft(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        if (hasCallback()) getCallback().onLoadNextArticle(DIRECTIONS.RIGHT, "0");
    }

    @Override
    public void onSwipeRight(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        String position = "0";
        if (!TazSettings.getInstance(getContext()).getPrefBoolean(TazSettings.PREFKEY.ISSCROLL, false)) position = "EOF";
        if (hasCallback()) getCallback().onLoadPrevArticle(DIRECTIONS.LEFT, position);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }
}
