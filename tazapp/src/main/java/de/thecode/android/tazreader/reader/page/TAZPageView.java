package de.thecode.android.tazreader.reader.page;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.artifex.mupdfdemo.Annotation.Type;
import com.artifex.mupdfdemo.CancellableTaskDefinition;
import com.artifex.mupdfdemo.LinkInfo;
import com.artifex.mupdfdemo.MuPDFCancellableTaskDefinition;
import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.PageView;
import com.artifex.mupdfdemo.TextWord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;

import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.reader.IReaderCallback;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.utils.StorageManager;

public class TAZPageView extends PageView {

    private static final Logger log = LoggerFactory.getLogger(TAZPageView.class);


    TAZMuPDFCore mCore;
    Page _page;
    Context _context;

    public TAZPageView(Context c, Point parentSize, Bitmap sharedHqBm) {
        super(c, parentSize, sharedHqBm);
        log.trace("");
        _context = c;
        mSize = parentSize;
    }

    public void init(Page page) {

        if (_page != null) log.debug("page: {}",page.getKey());
        if (_page != null) {
            if (!_page.getKey()
                      .equals(page.getKey())) {
                try {
                    if (mCore != null) mCore.onDestroy();
                } catch (Exception e) {
                    log.warn("",e);
                }
                mCore = null;
            }
        }
        _page = page;

        if (mCore != null) setPage();
        else {
            new LoadCoreTask(_context, _page) {

                @Override
                protected void onPostExecute(TAZMuPDFCore result) {
                    mCore = result;
                    if (mCore != null) setPage();
                }
            }.execute();
        }
    }


    public void setPage() {
        super.setPage(0, mCore.getPageSize());
    }

    @Override
    protected CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY, final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        log.trace("");
        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {

            @Override
            public Void doInBackground(MuPDFCore.Cookie cookie, Void... params) {
                log.debug("cookie: {}, params: {}",cookie, params);
                // Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
                // is not incremented when drawing.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) bm.eraseColor(0);
                mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };

    }

    protected CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY, final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        log.debug("bm: {}, sizeX: {}, sizeY: {}, patchX: {}, patchY: {}, patchWidth: {}, patchHeight: {}",bm, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight);
        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {

            @Override
            public Void doInBackground(MuPDFCore.Cookie cookie, Void... params) {
                log.debug("cookie: {}, params: {}",cookie, params);
                // Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
                // is not incremented when drawing.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) bm.eraseColor(0);
                mCore.updatePage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };
    }

    @Override
    protected LinkInfo[] getLinkInfo() {
        log.debug("");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected TextWord[][] getText() {
        log.debug("");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void addMarkup(PointF[] quadPoints, Type type) {
        log.debug("quadPoints: {}, type: {}",quadPoints, type);
        // TODO Auto-generated method stub
    }

    public class LoadCoreTask extends AsyncTask<Void, Void, TAZMuPDFCore> {

        Context _context;
        String _filename;

        public LoadCoreTask(Context context, Page page) {
            StorageManager storage = StorageManager.getInstance(context);
            File pdfFile = new File(storage.getPaperDirectory(page.getPaper()), page.getKey());
            _filename = pdfFile.getAbsolutePath();
        }

        @Override
        protected TAZMuPDFCore doInBackground(Void... params) {
            if (!isCancelled()) {
                try {
                    TAZMuPDFCore result = new TAZMuPDFCore(_context, _filename);
                    if (!isCancelled()) result.countPages();
                    if (!isCancelled()) result.setPageSize(result.getPageSize(0));
                    if (!isCancelled()) return result;
                } catch (Exception e) {
                    log.warn("",e);
                }
            }
            return null;
        }

        @Override
        protected void onCancelled(TAZMuPDFCore tazMuPDFCore) {
            if (tazMuPDFCore != null) tazMuPDFCore.onDestroy();
            super.onCancelled(tazMuPDFCore);
        }
    }

    public void setScale(float scale) {
        log.debug("scale: {}",scale);
        // This type of view scales automatically to fit the size
        // determined by the parent view groups during layout
    }

    public void passClickEvent(float x, float y) {

        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        final float docRelX = (x - getLeft()) / scale;
        final float docRelY = (y - getTop()) / scale;

        float relativeX = docRelX / mCore.getPageSize().x;
        float relativeY = docRelY / mCore.getPageSize().y;

        log.debug("relativeX: {}, relativeY: {}",relativeX, relativeY);

        IReaderCallback readerCallback = ((TAZReaderView) getParent()).getReaderCallback();

        for (Page.Geometry geometry : _page.getGeometries()) {
            if (geometry.checkCoordinates(relativeX, relativeY)) {
                String link = geometry.getLink();
                if (link != null) {
                    IIndexItem indexItem = _page.getPaper()
                                                .getPlist()
                                                .getIndexItem(link);
                    if (indexItem != null) {
                        if (readerCallback != null) readerCallback.onLoad(link);
                        return;
                    } else {
                        if (link.toLowerCase(Locale.getDefault())
                                .startsWith("http")) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            _context.startActivity(browserIntent);
                            return;
                        }
                    }
                }
            }
        }
        if (readerCallback != null) readerCallback.onLoad(_page.getDefaultLink());

    }


}
