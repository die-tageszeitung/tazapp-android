package de.thecode.android.tazreader.reader;

import com.google.common.base.Strings;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.dd.plist.PropertyListFormatException;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.utils.StorageManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import timber.log.Timber;

public class PaperLoader extends AsyncTaskLoader<PaperLoader.PaperLoaderResult> {

    StorageManager    mStorage;
    long              mPaperId;
    PaperLoaderResult mData;

    public PaperLoader(Context context, long paperId) {
        super(context);
        mStorage = StorageManager.getInstance(context);
        this.mPaperId = paperId;
    }

    @Override
    public PaperLoaderResult loadInBackground() {
        PaperLoaderResult result = new PaperLoaderResult();

        try {
            Paper paper = new Paper(getContext(), mPaperId);
            //paper.parsePlist(mStorage.getPaperFile(paper));

            paper.parsePlist(new File(mStorage.getPaperDirectory(paper), Paper.CONTENT_PLIST_FILENAME));

            String bookmarkJsonString = paper.getStoreValue(getContext(), ReaderActivity.STORE_KEY_BOOKMARKS);
            if (!Strings.isNullOrEmpty(bookmarkJsonString)) {
                JSONArray bookmarksJsonArray = new JSONArray(bookmarkJsonString);
                for (int i = 0; i < bookmarksJsonArray.length(); i++) {
                    IIndexItem item = paper.getPlist()
                                           .getIndexItem(bookmarksJsonArray.getString(i));
                    if (item != null) {
                        item.setBookmark(true);
                    }
                }
            }

            result.paper = paper;

        } catch (IllegalStateException | Paper.PaperNotFoundException | SAXException | ParserConfigurationException | PropertyListFormatException | JSONException | ParseException | IOException e) {
            Timber.e(e);
            result.exception = e;
        }
        return result;
    }

    @Override
    public void deliverResult(PaperLoaderResult data) {
        Timber.i("PaperLoader deliver result");
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        PaperLoaderResult oldData = mData;
        mData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            releaseResources(oldData);
        }
    }


    @Override
    protected void onStartLoading() {
        Timber.i("PaperLoader onStartLoading");
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        Timber.i("PaperLoader onStopLoading");
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();
    }

    @Override
    protected void onReset() {
        Timber.i("PaperLoader onReset");
        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (mData != null) {
            releaseResources(mData);
            mData = null;
        }
    }

    @Override
    public void onCanceled(PaperLoaderResult data) {
        Timber.i("PaperLoader onCanceled");
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(PaperLoaderResult data) {
        // All resources associated with the Loader
        // should be released here.
    }

    public class PaperLoaderResult {
        private Paper     paper;
        private Exception exception;

        public Paper getPaper() {
            return paper;
        }

        public Exception getError() {
            return exception;
        }

        public void setError(Exception exception) {
            this.exception = exception;
        }

        public boolean hasError() {
            return exception != null;
        }
    }

    public class PaperLoaderException extends Exception {
        public PaperLoaderException() {
        }

        public PaperLoaderException(String detailMessage) {
            super(detailMessage);
        }

        public PaperLoaderException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public PaperLoaderException(Throwable throwable) {
            super(throwable);
        }
    }

}
