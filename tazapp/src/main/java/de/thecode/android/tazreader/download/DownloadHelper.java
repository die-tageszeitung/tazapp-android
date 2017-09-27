package de.thecode.android.tazreader.download;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.okhttp3.RequestHelper;
import de.thecode.android.tazreader.secure.Base64;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.utils.StorageHelper;
import de.thecode.android.tazreader.utils.UserAgentHelper;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class DownloadHelper {

    @SuppressLint("NewApi")
    public static void  enquePaper(Context context, long paperId) throws IllegalArgumentException, Paper.PaperNotFoundException,
            DownloadNotAllowedException, NotEnoughSpaceException {


        Paper paper = Paper.getPaperWithId(context, paperId);
        if (paper == null) throw new Paper.PaperNotFoundException();

        if (TazSettings.getInstance(context)
                       .isDemoMode() && !paper.isDemo()) throw new DownloadNotAllowedException();

        Timber.i("requesting paper download: %s", paper);

        RequestHelper requestHelper = RequestHelper.getInstance(context);

        Uri downloadUri = requestHelper.addToUri(Uri.parse(paper.getLink()));

        Request request;
        try {
            request = new Request(downloadUri);
        } catch (Exception e1) {
            String httpUrl = paper.getLink()
                                  .replace("https://", "http://");
            request = new Request(requestHelper.addToUri(Uri.parse(httpUrl)));
        }
        addUserAgent(context, request);

        if (paper.getPublicationId() > 0) {
            request.addRequestHeader("Authorization",
                                     "Basic " + Base64.encodeToString((AccountHelper.getInstance(context)
                                                                                    .getUser(AccountHelper.ACCOUNT_DEMO_USER) + ":" + AccountHelper.getInstance(
                                             context)
                                                                                                                                                   .getPassword(
                                                                                                                                                           AccountHelper.ACCOUNT_DEMO_PASS)).getBytes(),
                                                                      Base64.NO_WRAP));
        }

        File destinationFile = StorageHelper.getDownloadFile(context, paper);

        if (destinationFile == null) throw new DownloadNotAllowedException("Fehler beim Ermitteln des Downloadverzeichnisses.");

        assertEnougSpaceForDownload(destinationFile.getParentFile(), calculateBytesNeeded(paper.getLen()));

        request.setDestinationUri(Uri.fromFile(destinationFile));

        request.setTitle(paper.getTitelWithDate(context));

        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(false);

        final long downloadId = ((android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);

        Timber.i("... download requested at android download manager, id: %d", downloadId);

        //paper.setDownloadprogress(0);
        paper.setDownloadId(downloadId);
        paper.setIsdownloaded(false);
        paper.setHasupdate(false);

        context.getContentResolver()
               .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);

        if (!TextUtils.isEmpty(paper.getResource())) {
            Resource resource = Resource.getWithKey(context, paper.getResource());
            paper.saveResourcePartner(context, resource);
            enqueResource(context, resource);
        }
    }

    @SuppressLint("NewApi")
    public static void enqueResource(Context context, Resource resource) throws IllegalArgumentException, NotEnoughSpaceException {
        //Paper paper = new Paper(context, paperId);

        //Uri downloadUri = Uri.parse(paper.getResourceUrl());

//        Resource resource = Resource.getWithKey(context, paper.getResource());
        Timber.i("requesting resource download: %s", resource);

//        if (resource.getKey() == null) {
//            resource.setKey(paper.getResource());
//            context.getContentResolver()
//                    .insert(Resource.CONTENT_URI, resource.getContentValues());
//        }

        if (!resource.isDownloaded() && !resource.isDownloading()) {
//            resource.setFileHash(paper.getResourceFileHash());
//            resource.setUrl(paper.getResourceUrl());
//            resource.setLen(paper.getResourceLen());
//            resource.setUrl(paper.getResourceUrl());

            Uri downloadUri;
            if (TextUtils.isEmpty(resource.getUrl())) {
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

                request = new Request(RequestHelper.getInstance(context)
                                                   .addToUri(Uri.parse(httpUrl)));
            }
            addUserAgent(context, request);

            File destinationFile = StorageHelper.getDownloadFile(context, resource);

            assertEnougSpaceForDownload(destinationFile.getParentFile(), calculateBytesNeeded(resource.getLen()));

            request.setDestinationUri(Uri.fromFile(destinationFile));

            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
            request.setVisibleInDownloadsUi(false);

            long downloadId = ((android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);

            Timber.i("... download requested at android download manager, id: %d", downloadId);

            resource.setDownloadId(downloadId);

            context.getContentResolver()
                   .update(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()),
                           resource.getContentValues(),
                           null,
                           null);
        }

    }

    private static void addUserAgent(Context context, Request request) {
        request.addRequestHeader(UserAgentHelper.USER_AGENT_HEADER_NAME,
                                 UserAgentHelper.getInstance(context)
                                                .getUserAgentHeaderValue());
    }

    public static void cancelDownload(Context context, long downloadId) {
        DownloadState state = getDownloadState(context, downloadId);
        if (state != null && state.getStatus() != DownloadState.STATUS_SUCCESSFUL) {
            if (((android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).remove(downloadId) > 0) {
                Cursor cursor = context.getContentResolver()
                                       .query(Paper.CONTENT_URI, null, Paper.Columns.DOWNLOADID + " = " + downloadId, null, null);
                try {
                    while (cursor.moveToNext()) {
                        Paper removedPaper = new Paper(cursor);
                        removedPaper.delete(context);
                    }
                } finally {
                    cursor.close();
                }

            }
        }
    }

    public static DownloadState getDownloadState(Context context, long downloadId) {
        return new DownloadState(((android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)), downloadId);
    }


    private static Map<Long, Map<Integer, List<Integer>>> stateStack = new HashMap<>();

    public static boolean isFirstOccurrenceOfState(DownloadState state) {
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

    public static class DownloadState {


        public static final int STATUS_SUCCESSFUL = android.app.DownloadManager.STATUS_SUCCESSFUL;
        public static final int STATUS_FAILED     = android.app.DownloadManager.STATUS_FAILED;
        public static final int STATUS_PAUSED     = android.app.DownloadManager.STATUS_PAUSED;
        public static final int STATUS_PENDING    = android.app.DownloadManager.STATUS_PENDING;
        public static final int STATUS_RUNNING    = android.app.DownloadManager.STATUS_RUNNING;
        public static final int STATUS_NOTFOUND   = 0;

        int    mStatus;
        int    mReason;
        long   mBytesTotal;
        long   mBytesDownloaded;
        String mUri;
        String mTitle;
        String mDescription;
        long   mDownloadId;

        public DownloadState(DownloadManager downloadManager, long downloadId) {
            mDownloadId = downloadId;
            android.app.DownloadManager.Query q = new android.app.DownloadManager.Query();

            q.setFilterById(downloadId);
            Cursor cursor = null;
            try {
                cursor = downloadManager.query(q);
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
         */
        public String getReasonText(Context context) {
            int id = context.getResources()
                            .getIdentifier("download_reason_" + getReason(), "string", context.getPackageName());
            if (id == 0) id = R.string.download_reason_notext;
            StringBuilder builder = new StringBuilder(context.getString(id)).append("(")
                                                                            .append(getReason())
                                                                            .append(")");
            return builder.toString();
        }

        /**
         * Gibt einen lesbaren Text des DownloadStatus zurück... Dieser ist in /res/values/strings_download.xml hinterlegt und
         * kann im erbenden Projekt überschrieben werden.
         */
        public String getStatusText(Context context) {
            switch (getStatus()) {
                case STATUS_SUCCESSFUL:
                    return context.getString(R.string.download_status_successful);
                case STATUS_FAILED:
                    return context.getString(R.string.download_status_failed);
                case STATUS_PAUSED:
                    return context.getString(R.string.download_status_paused);
                case STATUS_PENDING:
                    return context.getString(R.string.download_status_pending);
                case STATUS_RUNNING:
                    return context.getString(R.string.download_status_running);
                default:
                    return context.getString(R.string.download_status_unknown);
            }
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static class DownloadProgressThread extends Thread {

        long            downloadId;
        long            paperId;
        int             progress;
        DownloadManager downloadManager;

        public DownloadProgressThread(Context context, long downloadId, long paperId) {
            downloadManager = ((android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE));
            this.downloadId = downloadId;
            this.paperId = paperId;
            setName(this.getClass()
                        .getSimpleName() + paperId);
        }

        @Override
        public void run() {
            Timber.i("starting");
            boolean downloading = true;
            while (downloading && !isInterrupted()) {
                DownloadState downloadState = new DownloadState(downloadManager, downloadId);
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
                    Timber.w(e);
                }
            }
            Timber.i("finished");
        }
    }

    private static long calculateBytesNeeded(long bytesOfDownload) {
        long oneHundred = 100 * 1024 * 1024;
        long threeDownloads = bytesOfDownload * 3;
        long fiveDownloads = bytesOfDownload * 5;
        if (threeDownloads > oneHundred) {
            return threeDownloads;
        } else return Math.min(oneHundred, fiveDownloads);
    }

    private static void assertEnougSpaceForDownload(File dir, long requested) throws NotEnoughSpaceException {
        if (requested <= 0) return;
        StatFs statFs = new StatFs(dir.getAbsolutePath());
        long available;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            available = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        } else {
            available = (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
        }
        if (available < requested) throw new NotEnoughSpaceException(requested, available);
        return;
    }


    public static class DownloadNotAllowedException extends Exception {
        public DownloadNotAllowedException() {
        }

        public DownloadNotAllowedException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static class NotEnoughSpaceException extends Exception {
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
