package de.thecode.android.tazreader.reader.page;

import com.google.common.base.Strings;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Paper.Plist.Book;
import de.thecode.android.tazreader.data.Paper.Plist.Category;
import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.data.Paper.Plist.Source;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.AbstractContentFragment;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.reader.ReaderTtsFragment;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.widget.PageIndexButton;
import de.thecode.android.tazreader.widget.ShareButton;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PagesFragment extends AbstractContentFragment {

    TAZReaderView _readerView;
    ShareButton mShareButton;

    TazReaderViewAdaper _adapter;

    List<Page> pages;

    String _startKey;


    public PagesFragment() {
        super();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.reader_pagereader, container, false);
        _readerView = (TAZReaderView) view.findViewById(R.id.readerview);
        _readerView.setAdapter(_adapter);
        mShareButton = (ShareButton) view.findViewById(R.id.share);
        PageIndexButton mPageIndexButton = (PageIndexButton) view.findViewById(R.id.pageindex);
        if (TazSettings.getInstance(mContext).getPrefBoolean(TazSettings.PREFKEY.PAGEINDEXBUTTON, false)) {
            mPageIndexButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof ReaderActivity) {
                        ((ReaderActivity) getActivity()).openPageIndexDrawer();
                    }
                }
            });
        } else
            mPageIndexButton.setVisibility(View.GONE);


        if (!Strings.isNullOrEmpty(_startKey)) setPage(_startKey);

        return view;
    }

    @Override
    public void init(Paper paper, String key, String postion) {
        Timber.d("paper: %s, key: %s, postion: %s", paper, key, postion);
        _startKey = key;
        pages = new ArrayList<>();
        for (Source source : paper.getPlist()
                                  .getSources()) {
            for (Book book : source.getBooks()) {
                for (Category category : book.getCategories()) {
                    pages.addAll(category.getPages());
                }
            }
        }
        _adapter = new TazReaderViewAdaper();
    }

    public void setPage(String key) {
        Timber.d("key: %s",key);
        for (Page page : pages) {
            if (page.getKey()
                    .equals(key)) {
                Timber.d("setting page with key: %s",key);
                _readerView.resetScale();
                _readerView.setDisplayedViewIndex(pages.indexOf(page));
                break;
            }
        }
    }

    public void setShareButtonCallback(IIndexItem item) {
        if (mShareButton != null) mShareButton.setCallback(item);
    }

    @Override
    public void onConfigurationChange(String key, String value) {
        setConfig(key, value);
    }

    @Override
    public void onDestroy() {

        if (_readerView != null) {
            if (_readerView.mChildViews != null) {
                for (int i = 0; i < _readerView.mChildViews.size(); i++) {
                    if (_readerView.mChildViews.get(_readerView.mChildViews.keyAt(i)) instanceof TAZPageView) {
                        TAZPageView pageView = (TAZPageView) _readerView.mChildViews.get(_readerView.mChildViews.keyAt(i));
                        if (pageView.mCore != null) pageView.mCore.onDestroy();
                        pageView.releaseBitmaps();
                    }
                }
            }
        }
        super.onDestroy();
    }

    public class TazReaderViewAdaper extends BaseAdapter {

        private Bitmap mSharedHqBm;

        @Override
        public int getCount() {
            if (pages != null) return pages.size();
            return 0;
        }

        @Override
        public Page getItem(int position) {
            Timber.d("position: %s",position);
            if (pages != null) return pages.get(position);
            return null;
        }

        @Override
        public long getItemId(int position) {
            Timber.d("position: %s",position);
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Timber.d("position: %s, convertView: %s, parent: %s",position, convertView, parent);

            TAZPageView pageView;

            if (convertView == null) {
                if (mSharedHqBm == null || mSharedHqBm.getWidth() != parent.getWidth() || mSharedHqBm.getHeight() != parent.getHeight())
                    mSharedHqBm = Bitmap.createBitmap(parent.getWidth(), parent.getHeight(), Bitmap.Config.ARGB_8888);
                pageView = new TAZPageView(getActivity(), new Point(parent.getWidth(), parent.getHeight()), mSharedHqBm);
            } else {
                pageView = (TAZPageView) convertView;
            }

            pageView.init(pages.get(position));

            return pageView;
        }

    }

    @Override
    public void onTtsStateChanged(ReaderTtsFragment.TTS state) {
        Timber.d("state: %s",state);
    }
}
