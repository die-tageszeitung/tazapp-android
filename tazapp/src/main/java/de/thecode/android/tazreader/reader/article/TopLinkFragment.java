package de.thecode.android.tazreader.reader.article;

import android.view.MotionEvent;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.ReaderActivity.DIRECTIONS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopLinkFragment extends ArticleFragment {

    private static final Logger log = LoggerFactory.getLogger(TopLinkFragment.class);
    public TopLinkFragment() {
        super();
    }


    @Override
    public void onSwipeBottom(ArticleWebView view, MotionEvent e1, MotionEvent e2) {
        log.trace("");
        if (hasCallback()) getCallback().onLoadPrevArticle(DIRECTIONS.TOP, "0");
    }

    @Override
    public void onSwipeTop(ArticleWebView view, MotionEvent e1, MotionEvent e2) {
        log.trace("");
        if (hasCallback()) getCallback().onLoadNextArticle(DIRECTIONS.BOTTOM, "0");
    }

    @Override
    public void onSwipeLeft(ArticleWebView view, MotionEvent e1, MotionEvent e2) {
        log.trace("");
        if (hasCallback()) getCallback().onLoadNextArticle(DIRECTIONS.RIGHT, "0");
    }

    @Override
    public void onSwipeRight(ArticleWebView view, MotionEvent e1, MotionEvent e2) {
        log.trace("");
        String position = "0";
        if (!TazSettings.getInstance(mContext).getPrefBoolean(TazSettings.PREFKEY.ISSCROLL, false)) position = "EOF";
        if (hasCallback()) getCallback().onLoadPrevArticle(DIRECTIONS.LEFT, position);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }
}
