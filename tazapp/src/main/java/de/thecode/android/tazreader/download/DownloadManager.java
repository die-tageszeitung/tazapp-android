package de.thecode.android.tazreader.download;

import android.annotation.SuppressLint;
import android.app.DownloadManager.Request;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;

import com.google.common.base.Strings;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.secure.Base64;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.sync.AccountHelper.CreateAccountException;
import de.thecode.android.tazreader.utils.StorageManager;

public class DownloadManager {

    private static final Logger log = LoggerFactory.getLogger(DownloadManager.class);

    android.app.DownloadManager mDownloadManager;
    Context mContext;
    StorageManager mStorage;

    private static DownloadManager instance;

    public static DownloadManager getInstance(Context context) {
        if (instance == null)
            instance = new DownloadManager(context.getApplicationContext());
        return instance;
    }

    private DownloadManager(Context context) {
        mDownloadManager = ((android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE));
        mContext = context;
        mStorage = StorageManager.getInstance(context);
    }

    @SuppressLint("NewApi")
    public void enquePaper(long paperId) throws IllegalArgumentException, Paper.PaperNotFoundException, CreateAccountException, DownloadNotAllowedException, NotEnoughSpaceException {

        Paper paper = new Paper(mContext, paperId);
        AccountHelper accountHelper = new AccountHelper(mContext);

        if (!accountHelper.isAuthenticated() && !paper.isDemo())
            throw new DownloadNotAllowedException();

        log.trace("requesting paper download: {}", paper);

        Uri downloadUri = Uri.parse(paper.getLink());

        Request request;
        try {
            request = new Request(downloadUri);
        } catch (Exception e1) {
            String httpUrl = paper.getLink()
                                  .replace("https://", "http://");
            request = new Request(Uri.parse(httpUrl));
        }

        if (paper.getPublicationId() > 0) {
            request.addRequestHeader("Authorization", "Basic " + Base64.encodeToString((accountHelper.getUser() + ":" + accountHelper.getPassword()).getBytes(), Base64.NO_WRAP));
        }

        File destinationFile = mStorage.getDownloadFile(paper);


        assertEnougSpaceForDownload(destinationFile, paper.getLen());



        request.setDestinationUri(Uri.fromFile(destinationFile));

        request.setTitle(paper.getTitelWithDate(mContext));

        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(false);

        final long downloadId = mDownloadManager.enqueue(request);

        log.trace("... download requested at android download manager, id: {}", downloadId);

        //paper.setDownloadprogress(0);
        paper.setDownloadId(downloadId);
        paper.setIsdownloaded(false);
        paper.setHasupdate(false);

        mContext.getContentResolver()
                .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);

        if (!Strings.isNullOrEmpty(paper.getResource())) {
            enqueResource(paper);
        }
    }

    @SuppressLint("NewApi")
    public void enqueResource(Paper paper) throws IllegalArgumentException, NotEnoughSpaceException {
        //Paper paper = new Paper(mContext, paperId);

        //Uri downloadUri = Uri.parse(paper.getResourceUrl());

        Resource resource = new Resource(mContext, paper.getResource());
        log.trace("requesting resource download: {}", resource);

        if (resource.getKey() == null) {
            resource.setKey(paper.getResource());
            mContext.getContentResolver()
                    .insert(Resource.CONTENT_URI, resource.getContentValues());
        }

        if (!resource.isDownloaded() && !resource.isDownloading()) {
            resource.setFileHash(paper.getResourceFileHash());
            resource.setUrl(paper.getResourceUrl());
            resource.setLen(paper.getResourceLen());
            resource.setUrl(paper.getResourceUrl());

            Uri downloadUri;
            if (Strings.isNullOrEmpty(resource.getUrl())) {
                downloadUri = Uri.parse(BuildConfig.RESOURCEURL)
                                 .buildUpon()
                                 .appendPath(resource.getKey())
                                 .build();
            } else downloadUri = Uri.parse(resource.getUrl());
            Request request;
            try {
                request = new Request(downloadUri);
            } catch (Exception e1) {
                String httpUrl = downloadUri.toString()
                                            .replace("https://", "http://");
                request = new Request(Uri.parse(httpUrl));
            }

            File destinationFile = mStorage.getDownloadFile(resource);

            assertEnougSpaceForDownload(destinationFile, paper.getLen());

            request.setDestinationUri(Uri.fromFile(destinationFile));

            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
            request.setVisibleInDownloadsUi(false);

            long downloadId = mDownloadManager.enqueue(request);

            log.trace("... download requested at android download manager, id: {}", downloadId);

            resource.setDownloadId(downloadId);

            mContext.getContentResolver()
                    .update(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()), resource.getContentValues(), null, null);
        }

    }

    public void cancelDownload(long downloadId) {
        DownloadState state = getDownloadState(downloadId);
        if (state != null && state.getStatus() != DownloadState.STATUS_SUCCESSFUL) {
            if (mDownloadManager.remove(downloadId) > 0) {
                Cursor cursor = mContext.getContentResolver()
                                        .query(Paper.CONTENT_URI, null, Paper.Columns.DOWNLOADID + " = " + downloadId, null, null);
                try {
                    while (cursor.moveToNext()) {
                        Paper removedPaper = new Paper(cursor);
                        removedPaper.delete(mContext);
                    }
                } finally {
                    cursor.close();
                }

            }
        }
    }

    public DownloadState getDownloadState(long downloadId) {
        return new DownloadState(downloadId);
    }


    private static Map<Long, Map<Integer, List<Integer>>> stateStack = new HashMap<>();

    public boolean isFirstOccurrenceOfState(DownloadState state) {
        boolean result = false;

        Map<Integer, List<Integer>> stateMap = stateStack.get(state.getDownloadId());

        if (stateMap == null) {
            stateMap = new HashMap<>();
            result = true;
        }

        List<Integer> reasonList = stateMap.get(state.getStatus());

        if (reasonList == null) {
            reasonList = new ArrayList<>();
            result = true;
        }

        if (!reasonList.contains(state.getReason())) {
            result = true;
        }

        if (result) {
            reasonList.add(state.getReason());
            stateMap.put(state.getStatus(), reasonList);
            stateStack.put(state.getDownloadId(), stateMap);
        }

        return result;
    }

    public class DownloadState {


        public static final int STATUS_SUCCESSFUL = android.app.DownloadManager.STATUS_SUCCESSFUL;
        public static final int STATUS_FAILED = android.app.DownloadManager.STATUS_FAILED;
        public static final int STATUS_PAUSED = android.app.DownloadManager.STATUS_PAUSED;
        public static final int STATUS_PENDING = android.app.DownloadManager.STATUS_PENDING;
        public static final int STATUS_RUNNING = android.app.DownloadManager.STATUS_RUNNING;
        public static final int STATUS_NOTFOUND = 0;

        int mStatus;
        int mReason;
        long mBytesTotal;
        long mBytesDownloaded;
        String mUri;
        String mTitle;
        String mDescription;
        long mDownloadId;

        public DownloadState(long downloadId) {
            mDownloadId = downloadId;
            android.app.DownloadManager.Query q = new android.app.DownloadManager.Query();

            q.setFilterById(downloadId);
            Cursor cursor = null;
            try {
                cursor = mDownloadManager.query(q);
                if (cursor != null && cursor.moveToFirst()) {
                    mStatus = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS));
                    mReason = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_REASON));
                    mBytesDownloaded = cursor.getLong(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    mBytesTotal = cursor.getLong(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    mUri = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_URI));
                    mTitle = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TITLE));
                    mDescription = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_DESCRIPTION));
                }
            } finally {
                if (cursor != null) cursor.close();
            }
            // Assume donwload is Canceled if no status was found
        }

        public long getDownloadId() {
            return mDownloadId;
        }

        /**
         * @return Status des Downloads nach {@link android.app.DownloadManager}
         * @see android.app.DownloadManager
         */
        public int getStatus() {
            return mStatus;
        }

        /**
         * @return Grund für den Status von getStatus() des Downloads nach {@link android.app.DownloadManager}
         * @see android.app.DownloadManager
         */
        public int getReason() {
            return mReason;
        }

        public long getBytesTotal() {
            return mBytesTotal;
        }

        public long getBytesDownloaded() {
            return mBytesDownloaded;
        }

        public int getDownloadProgress() {
            if (mBytesTotal > 0) return (int) (mBytesDownloaded * 100 / mBytesTotal);
            else return 0;
        }

        public Uri getUri() {
            return Uri.parse(mUri);
        }

        public String getDescription() {
            return mDescription;
        }

        public String getTitle() {
            return mTitle;
        }

        /**
         * Gibt einen lesbaren Text des Grundes für den DownloadStatus zurück... Dieser ist in /res/values/strings_download.xml
         * hinterlegt und kann im erbenden Projekt überschrieben werden.
         *
         * @return
         */
        public String getReasonText() {
            int id = mContext.getResources()
                             .getIdentifier("download_reason_" + getReason(), "string", mContext.getPackageName());
            if (id == 0) id = R.string.download_reason_notext;
            StringBuilder builder = new StringBuilder(mContext.getString(id)).append("(")
                                                                             .append(getReason())
                                                                             .append(")");
            return builder.toString();
        }

        /**
         * Gibt einen lesbaren Text des DownloadStatus zurück... Dieser ist in /res/values/strings_download.xml hinterlegt und kann im
         * erbenden Projekt überschrieben werden.
         *
         * @return
         */
        public String getStatusText() {
            switch (getStatus()) {
                case STATUS_SUCCESSFUL:
                    return mContext.getString(R.string.download_status_successful);
                case STATUS_FAILED:
                    return mContext.getString(R.string.download_status_failed);
                case STATUS_PAUSED:
                    return mContext.getString(R.string.download_status_paused);
                case STATUS_PENDING:
                    return mContext.getString(R.string.download_status_pending);
                case STATUS_RUNNING:
                    return mContext.getString(R.string.download_status_running);
                default:
                    return mContext.getString(R.string.download_status_unknown);
            }
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public class DownloadProgressThread extends Thread {

        long downloadId;
        long paperId;
        int progress;

        public DownloadProgressThread(long downloadId, long paperId) {
            this.downloadId = downloadId;
            this.paperId = paperId;
            setName(this.getClass()
                        .getSimpleName() + paperId);
        }

        @Override
        public void run() {
            log.trace("starting");
            boolean downloading = true;
            while (downloading && !isInterrupted()) {
                DownloadState downloadState = new DownloadState(downloadId);
                switch (downloadState.getStatus()) {
                    case DownloadState.STATUS_FAILED:
                    case DownloadState.STATUS_NOTFOUND:
                    case DownloadState.STATUS_SUCCESSFUL:
                        downloading = false;
                        break;
                }

                int oldProgress = progress;
                progress = downloadState.getDownloadProgress();
                if (progress != oldProgress) EventBus.getDefault()
                                                     .post(new DownloadProgressEvent(paperId, progress));
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    log.warn(e.getMessage());
                }
            }
            log.trace("finished");
        }
    }


    private static void assertEnougSpaceForDownload(File dir, long bytesNeeded) throws NotEnoughSpaceException {
        if (bytesNeeded <= 0) return;
        StatFs statFs = new StatFs(dir.getAbsolutePath());
        long available;
        long requested = bytesNeeded * 10;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            available = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        } else {
            available = (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
        }
        if (available < requested) throw new NotEnoughSpaceException(requested,available);
        return ;
    }


    public class DownloadNotAllowedException extends Exception {
        public DownloadNotAllowedException() {
        }

        public DownloadNotAllowedException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static class NotEnoughSpaceException extends Exception{
        final long requestedByte;
        final long availableByte;
        public NotEnoughSpaceException(long requestedByte, long availableByte) {
            super();
            this.requestedByte = requestedByte;
            this.availableByte = availableByte;
        }

        public long getRequestedByte() {
            return requestedByte;
        }

        public long getAvailableByte() {
            return availableByte;
        }
    }


}
