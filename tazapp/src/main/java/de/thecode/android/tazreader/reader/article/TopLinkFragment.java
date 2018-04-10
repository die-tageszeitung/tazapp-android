package de.thecode.android.tazreader.reader.article;

import android.os.Bundle;
import android.view.MotionEvent;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.ReaderActivity.DIRECTIONS;
import de.thecode.android.tazreader.reader.ReaderBaseFragment;

public class TopLinkFragment extends ArticleFragment {

    public static TopLinkFragment newInstance(String bookId, String articleKey) {
        TopLinkFragment fragment = ReaderBaseFragment.newInstance(TopLinkFragment.class,bookId);
        Bundle arguments = fragment.getArguments();
        arguments.putString(ArticleFragment.ARG_KEY, articleKey);
        fragment.setArguments(arguments);
        return fragment;
    }

    public TopLinkFragment() {
        super();
    }


    @Override
    public void onSwipeBottom(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        if (getReaderActivity() != null) getReaderActivity().onLoadPrevArticle(DIRECTIONS.TOP, "0");
    }

    @Override
    public void onSwipeTop(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        if (getReaderActivity() != null) getReaderActivity().onLoadNextArticle(DIRECTIONS.BOTTOM, "0");
    }

    @Override
    public void onSwipeLeft(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        if (getReaderActivity() != null) getReaderActivity().onLoadNextArticle(DIRECTIONS.RIGHT, "0");
    }

    @Override
    public void onSwipeRight(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        String position = "0";
        if (!TazSettings.getInstance(getContext()).getPrefBoolean(TazSettings.PREFKEY.ISSCROLL, false)) position = "EOF";
        if (getReaderActivity() != null) getReaderActivity().onLoadPrevArticle(DIRECTIONS.LEFT, position);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }
}
