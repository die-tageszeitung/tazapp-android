package de.thecode.android.tazreader.reader.pagetoc;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.core.content.ContextCompat;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.ParametrizedRunnable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by mate on 22.03.18.
 */

public class PageTocLiveData extends LiveData<PageTocLiveData.ResultWrapper> {

    private final ExecutorService          executor   = new ThreadPoolExecutor(1,
                                                                               1,
                                                                               0L,
                                                                               TimeUnit.MILLISECONDS,
                                                                               new LinkedBlockingDeque<>());
    private final Map<String, PageTocItem> pageTocMap = new LinkedHashMap<>();
    private final LiveData<ITocItem> currentKeyLiveData;

    private final Bitmap overlayBitmap;
    private final int    overlayBitmapWith;
    private final int    overlayBitmapHeight;
    private final int    overlayBitmapColor;
    private final int    overlayBitmapPadding;

    private final Observer<ITocItem> currentKeyObserver = tocItem ->
        executor.execute(new ParametrizedRunnable<ITocItem>() {
            @Override
            public void run(ITocItem tocItem) {
                if (tocItem != null) {

                    Paper.Plist.Page page = null;
                    if (tocItem instanceof Paper.Plist.Page) page = (Paper.Plist.Page) tocItem;
                    else if (tocItem instanceof Paper.Plist.Page.Article)
                        page = ((Paper.Plist.Page.Article) tocItem).getRealPage();
                    else if (tocItem instanceof Paper.Plist.TopLink) page = ((Paper.Plist.TopLink) tocItem).getPage();
                    if (page != null) {
                        for (PageTocItem pageTocItem : pageTocMap.values()) {
                            if (pageTocItem.getKey().equals(page.getKey())){
                                float x1 = 0;
                                float x2 = 1F;
                                float y1 = 0;
                                float y2 = 1F;
                                for (Paper.Plist.Page.Geometry geometry : page.getGeometries()) {
                                    if (geometry.getLink()
                                                .equals(tocItem.getKey())) {
                                        if (x1 == 0 || geometry.getX1() < x1) x1 = geometry.getX1();
                                        if (y1 == 0 || geometry.getY1() < y1) y1 = geometry.getY1();
                                        if (x2 == 1F || geometry.getX2() > x2) x2 = geometry.getX2();
                                        if (y2 == 1F || geometry.getY2() > y2) y2 = geometry.getY2();
                                    }
                                }
                                makeOverlayBitmap(x1, y1, x2, y2);
                                pageTocItem.setOverlayBitmap(overlayBitmap);
                            } else {
                                pageTocItem.removeOverlayBitmap();
                            }
                        }
                    }
                    publish();
                }
            }
        }.set(tocItem));


    public PageTocLiveData(Context context, LiveData<ITocItem> currentKeyLiveData) {
        overlayBitmapHeight = context.getResources()
                                     .getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_height) - (2 * context.getResources()
                                                                                                                    .getDimensionPixelSize(
                                                                                                                            R.dimen.pageindex_padding));
        overlayBitmapWith = context.getResources()
                                   .getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_width) - (2 * context.getResources()
                                                                                                                 .getDimensionPixelSize(
                                                                                                                         R.dimen.pageindex_padding));

        overlayBitmapPadding = context.getResources()
                                      .getDimensionPixelSize(R.dimen.pageindex_thumbnail_current_borderwidth);

        overlayBitmapColor = ContextCompat.getColor(context, R.color.pageindex_overlay_color);

        overlayBitmap = Bitmap.createBitmap(overlayBitmapWith, overlayBitmapHeight, Bitmap.Config.ARGB_8888);

        this.currentKeyLiveData = currentKeyLiveData;
    }

    public void create(Paper.Plist plist) {
        currentKeyLiveData.observeForever(currentKeyObserver);
        executor.execute(new ParametrizedRunnable<Paper.Plist>() {
            @Override
            public void run(Paper.Plist plist) {
                Map<String, PageTocItem> result = new LinkedHashMap<>();
                for (Paper.Plist.Source source : plist.getSources()) {
                    PageTocItem sourceItem = new PageTocItem(source);
                    result.put(source.getKey(), sourceItem);
                    for (Paper.Plist.Book book : source.getBooks()) {
                        for (Paper.Plist.Category category : book.getCategories()) {
                            for (Paper.Plist.Page page : category.getPages()) {
                                PageTocItem pageTocItem = new PageTocItem(page);
                                result.put(page.getKey(), pageTocItem);
                            }
                        }
                    }
                }
                pageTocMap.clear();
                pageTocMap.putAll(result);

            }
        }.set(plist));
    }

    private void publish() {
        executor.execute(() -> {
            List<PageTocItem> shownList = new ArrayList<>();
            ResultWrapper resultWrapper = new ResultWrapper();
            for (PageTocItem pageTocItem : pageTocMap.values()) {
                try {
                    shownList.add(pageTocItem.clone());
                    if (resultWrapper.scrollToPosition == -1 && pageTocItem.hasOverlayBitmap())
                        resultWrapper.scrollToPosition = shownList.size()-1;
                } catch (CloneNotSupportedException e) {
                    Timber.e(e);
                }
            }
            resultWrapper.list = shownList;
            postValue(resultWrapper);
        });
    }

    private void makeOverlayBitmap(float x1, float y1, float x2, float y2) {
        try {
            overlayBitmap.eraseColor(Color.TRANSPARENT);
            Canvas canvas = new Canvas(overlayBitmap);

            Paint paint = new Paint();

            float halfPadding = ((float) overlayBitmapPadding) / 2;
            paint.setColor(overlayBitmapColor);
            paint.setAlpha(128);
            paint.setStrokeWidth(overlayBitmapPadding);
            paint.setStyle(Paint.Style.STROKE);

            float dx1 = (x1 * overlayBitmapWith) - halfPadding;
            if (dx1 < halfPadding) dx1 = halfPadding;

            float dy1 = (y1 * overlayBitmapHeight) - halfPadding;
            if (dy1 < halfPadding) dy1 = halfPadding;

            float dx2 = (x2 * overlayBitmapWith) + halfPadding;
            if (dx2 > overlayBitmapWith - halfPadding) dx2 = overlayBitmapWith - halfPadding;

            float dy2 = (y2 * overlayBitmapHeight) + halfPadding;
            if (dy2 > overlayBitmapHeight - halfPadding) dy2 = overlayBitmapHeight - halfPadding;

            canvas.drawRect(dx1, dy1, dx2, dy2, paint);
        } catch (IllegalStateException e) {
            Timber.w(e);
        }
    }


    public static class ResultWrapper {
        private List<PageTocItem> list;
        private int scrollToPosition = -1;

        int getScrollToPosition() {
            return scrollToPosition;
        }

        public List<PageTocItem> getList() {
            return list;
        }
    }
}
