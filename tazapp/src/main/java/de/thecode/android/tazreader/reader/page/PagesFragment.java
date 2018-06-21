package de.thecode.android.tazreader.reader.page;

import android.arch.lifecycle.Observer;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.artifex.mupdf.viewer.PageView;
import com.artifex.mupdf.viewer.ReaderView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.AbstractContentFragment;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.widget.ReaderButton;
import de.thecode.android.tazreader.widget.ShareButton;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PagesFragment extends AbstractContentFragment {


    ReaderView  _readerView;
    ShareButton mShareButton;

    TazReaderViewAdaper _adapter;

    String currentKey;


    public PagesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new TazReaderViewAdaper();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_pagereader, container, false);
        _readerView = view.findViewById(R.id.readerview);
//        _readerView.setListener(new TAZReaderView.TAZReaderViewListener() {
//            @Override
//            public void onMoveToChild(String key) {
//                getReaderViewModel().setCurrentKey(key);
//            }
//        });
        _readerView.setAdapter(_adapter);
        mShareButton = view.findViewById(R.id.share);
        ReaderButton mPageIndexButton = view.findViewById(R.id.pageindex);
        if (TazSettings.getInstance(getContext())
                       .getPrefBoolean(TazSettings.PREFKEY.PAGEINDEXBUTTON, false)) {
            mPageIndexButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof ReaderActivity) {
                        ((ReaderActivity) getActivity()).openPageIndexDrawer();
                    }
                }
            });
            mPageIndexButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(v.getContext(), R.string.reader_action_pageindex, Toast.LENGTH_LONG)
                         .show();
                    return true;
                }
            });
        } else mPageIndexButton.setVisibility(View.GONE);

        ReaderButton mIndexButton = view.findViewById(R.id.index);
        if (TazSettings.getInstance(getContext())
                       .getPrefBoolean(TazSettings.PREFKEY.PAGEINDEXBUTTON, false)) {
            mIndexButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof ReaderActivity) {
                        ((ReaderActivity) getActivity()).openIndexDrawer();
                    }
                }
            });
            mIndexButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(v.getContext(), R.string.reader_action_index, Toast.LENGTH_LONG)
                         .show();
                    return true;
                }
            });
        } else mIndexButton.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getReaderViewModel().getPagesLiveData()
                            .observe(this, new Observer<List<Page>>() {
                                @Override
                                public void onChanged(@Nullable List<Page> pages) {
                                    _adapter.update(pages);
                                }
                            });
        getReaderViewModel().getCurrentKeyLiveData()
                            .observe(this, new Observer<ITocItem>() {
                                @Override
                                public void onChanged(@Nullable ITocItem tocItem) {
                                    if (tocItem != null && _adapter.pages.size() > 0 && tocItem instanceof Page) {
                                        setShareButtonCallback(tocItem);
                                        int moveToPos = _adapter.pages.indexOf(tocItem);
                                        int readerPos = _readerView.getDisplayedViewIndex();
                                        if (moveToPos != -1 && readerPos != moveToPos) {
//                                            _readerView.resetScale();
                                            _readerView.setDisplayedViewIndex(moveToPos);
                                        }
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

        super.onDestroy();
    }

    public class TazReaderViewAdaper extends BaseAdapter {

        private Bitmap mSharedHqBm;
        private       List<Page>          pages      = new ArrayList<>();
        private final SparseArray<PointF> mPageSizes = new SparseArray<>();

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

//            TAZPageView pageView;
//
//            if (convertView == null) {
//                if (mSharedHqBm == null || mSharedHqBm.getWidth() != parent.getWidth() || mSharedHqBm.getHeight() != parent.getHeight())
//                    mSharedHqBm = Bitmap.createBitmap(parent.getWidth(), parent.getHeight(), Bitmap.Config.ARGB_8888);
//                pageView = new TAZPageView(getActivity(), new Point(parent.getWidth(), parent.getHeight()), mSharedHqBm);
//            } else {
//                pageView = (TAZPageView) convertView;
//            }
//
//            try {
//                pageView.init(pages.get(position));
//            } catch (IndexOutOfBoundsException e) {
//                Timber.w(e);
//            }

            final PageView pageView;
            if (convertView == null) {
                if (mSharedHqBm == null || mSharedHqBm.getWidth() != parent.getWidth() || mSharedHqBm.getHeight() != parent.getHeight())
                    mSharedHqBm = Bitmap.createBitmap(parent.getWidth(), parent.getHeight(), Bitmap.Config.ARGB_8888);

                pageView = new PageView(parent.getContext(), new Point(parent.getWidth(), parent.getHeight()), mSharedHqBm);
            } else {
                pageView = (PageView) convertView;
            }
            pageView.setCore(getReaderViewModel().getPaperLiveData().getCore(pages.get(position).getKey()));

            PointF pageSize = mPageSizes.get(position);
            if (pageSize != null) {
                // We already know the page size. Set it up
                // immediately
                pageView.setPage(position, pageSize);
            } else {
                // Page size as yet unknown. Blank it for now, and
                // start a background task to find the size
                pageView.blank(position);
                AsyncTask<Void, Void, PointF> sizingTask = new AsyncTask<Void, Void, PointF>() {
                    @Override
                    protected PointF doInBackground(Void... arg0) {
                        return pageView.getCore().getPageSize(0);
                    }

                    @Override
                    protected void onPostExecute(PointF result) {
                        super.onPostExecute(result);
                        // We now know the page size
                        mPageSizes.put(position, result);
                        // Check that this view hasn't been reused for
                        // another page since we started
                        if (pageView.getPage() == position) pageView.setPage(position, result);
                    }
                };

                sizingTask.execute((Void) null);
            }
            return pageView;
        }

    }
}
