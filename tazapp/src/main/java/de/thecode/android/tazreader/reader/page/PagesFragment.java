package de.thecode.android.tazreader.reader.page;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.AbstractContentFragment;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.widget.ShareButton;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import timber.log.Timber;

public class PagesFragment extends AbstractContentFragment {


    TAZReaderView _readerView;
    ShareButton   mShareButton;

    TazReaderViewAdaper _adapter;

    public PagesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new TazReaderViewAdaper();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_pagereader, container, false);
        _readerView = view.findViewById(R.id.readerview);
        _readerView.setListener(key -> {
                getReaderViewModel().setCurrentKey(key);
        });
        _readerView.setAdapter(_adapter);
        mShareButton = view.findViewById(R.id.share);
        ImageView mPageIndexButton = view.findViewById(R.id.pageindex);
        if (TazSettings.getInstance(getContext())
                       .getPrefBoolean(TazSettings.PREFKEY.PAGEINDEXBUTTON, false)) {
            mPageIndexButton.setOnClickListener(v -> {
                    if (getActivity() instanceof ReaderActivity) {
                        ((ReaderActivity) getActivity()).openPageIndexDrawer();
                    }
            });
            mPageIndexButton.setOnLongClickListener(v ->  {
                    Toast.makeText(v.getContext(), R.string.reader_action_pageindex, Toast.LENGTH_LONG)
                         .show();
                    return true;
            });
        } else mPageIndexButton.setVisibility(View.GONE);

        ImageView mIndexButton = view.findViewById(R.id.index);
        if (TazSettings.getInstance(getContext())
                       .getPrefBoolean(TazSettings.PREFKEY.PAGEINDEXBUTTON, false)) {
            mIndexButton.setOnClickListener(v -> {
                    if (getActivity() instanceof ReaderActivity) {
                        ((ReaderActivity) getActivity()).openIndexDrawer();
                    }
            });
            mIndexButton.setOnLongClickListener(v -> {
                    Toast.makeText(v.getContext(), R.string.reader_action_index, Toast.LENGTH_LONG)
                         .show();
                    return true;
            });
        } else mIndexButton.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getReaderViewModel().getPagesLiveData()
                            .observe(this, _adapter::update);
        getReaderViewModel().getCurrentKeyLiveData()
                            .observe(this, tocItem -> {
                                    if (tocItem != null && _adapter.pages.size() > 0 && tocItem instanceof Page) {
                                        setShareButtonCallback(tocItem);
                                        int moveToPos = _adapter.pages.indexOf(tocItem);
                                        int readerPos = _readerView.getDisplayedViewIndex();
                                        if (moveToPos != -1 && readerPos != moveToPos) {
                                            _readerView.resetScale();
                                            _readerView.setDisplayedViewIndex(moveToPos);
                                        }
                                    }
                            });
    }

    public void setShareButtonCallback(ITocItem item) {
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
        List<Page> pages = new ArrayList<>();

        public void update(List<Page> newPages) {
            pages.clear();
            pages.addAll(newPages);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (pages != null) return pages.size();
            return 0;
        }

        @Override
        public Page getItem(int position) {
            Timber.d("position: %s", position);
            if (pages != null) return pages.get(position);
            return null;
        }

        @Override
        public long getItemId(int position) {
            Timber.d("position: %s", position);
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Timber.d("position: %s, convertView: %s, parent: %s", position, convertView, parent);

            TAZPageView pageView;

            if (convertView == null) {
                if (mSharedHqBm == null || mSharedHqBm.getWidth() != parent.getWidth() || mSharedHqBm.getHeight() != parent.getHeight())
                    mSharedHqBm = Bitmap.createBitmap(parent.getWidth(), parent.getHeight(), Bitmap.Config.ARGB_8888);
                pageView = new TAZPageView(getActivity(), new Point(parent.getWidth(), parent.getHeight()), mSharedHqBm);
            } else {
                pageView = (TAZPageView) convertView;
            }

            try {
                pageView.init(pages.get(position));
            } catch (IndexOutOfBoundsException e) {
                Timber.w(e);
            }

            return pageView;
        }
    }
}
