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

import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.utils.StorageManager;

import java.io.File;
import java.util.Locale;

import timber.log.Timber;

public class TAZPageView extends PageView {

    TAZMuPDFCore mCore;
    Page _page;

    public TAZPageView(Context c, Point parentSize, Bitmap sharedHqBm) {
        super(c, parentSize, sharedHqBm);
        mSize = parentSize;
    }

    public void init(Page page) {

        if (_page != null) Timber.d("page: %s",page.getKey());
        if (_page != null) {
            if (!_page.getKey()
                      .equals(page.getKey())) {
                try {
                    if (mCore != null) mCore.onDestroy();
                } catch (Exception e) {
                    Timber.w(e);
                }
                mCore = null;
            }
        }
        _page = page;

        if (mCore != null) setPage();
        else {
            new LoadCoreTask(getContext(), _page) {

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

        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {

            @Override
            public Void doInBackground(MuPDFCore.Cookie cookie, Void... params) {
                Timber.d("cookie: %s, params: %s",cookie, params);
                // Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
                // is not incremented when drawing.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) bm.eraseColor(0);
                mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };

    }

    protected CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY, final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        Timber.d("bm: %s, sizeX: %s, sizeY: %s, patchX: %s, patchY: %s, patchWidth: %s, patchHeight: %s",bm, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight);
        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {

            @Override
            public Void doInBackground(MuPDFCore.Cookie cookie, Void... params) {
                Timber.d("cookie: %s, params: %s",cookie, params);
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

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected TextWord[][] getText() {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void addMarkup(PointF[] quadPoints, Type type) {
        Timber.d("quadPoints: %s, type: %s",quadPoints, type);
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
                    Timber.w(e);
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
        Timber.d("scale: %s",scale);
        // This type of view scales automatically to fit the size
        // determined by the parent view groups during layout
    }

    public void passClickEvent(float x, float y) {

        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        final float docRelX = (x - getLeft()) / scale;
        final float docRelY = (y - getTop()) / scale;

        // do nothing if core is not initialized yet
        if (mCore != null) {
            float relativeX = docRelX / mCore.getPageSize().x;
            float relativeY = docRelY / mCore.getPageSize().y;

            Timber.d("relativeX: %s, relativeY: %s", relativeX, relativeY);

            ReaderActivity readerActivity = (ReaderActivity) getContext();

            for (Page.Geometry geometry : _page.getGeometries()) {
                if (geometry.checkCoordinates(relativeX, relativeY)) {
                    String link = geometry.getCleanLink();
                    Timber.d("Found link: %s", link);
                    if (link != null) {
                        ITocItem indexItem = _page.getPaper()
                                .getPlist()
                                .getIndexItem(link);
                        if (indexItem != null) {
                            readerActivity.loadContentFragment(link);
                            return;
                        } else {
                            Timber.i("found no item for link in index");
                            if (link.toLowerCase(Locale.getDefault())
                                    .startsWith("http")) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                                getContext().startActivity(browserIntent);
                                return;
                            }
                        }
                    }
                }
            }
            readerActivity.loadContentFragment(_page.getDefaultLink());
        }
    }


}
