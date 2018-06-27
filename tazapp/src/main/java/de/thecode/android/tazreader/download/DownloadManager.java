package de.thecode.android.tazreader.download;

import android.annotation.SuppressLint;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.okhttp3.RequestHelper;
import de.thecode.android.tazreader.secure.Base64;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.utils.ReadableException;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.UserAgentHelper;
import de.thecode.android.tazreader.utils.UserDeviceInfo;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class DownloadManager {

    private final android.app.DownloadManager mDownloadManager;
    //    private final Context                     mContext;
    private final StorageManager              mStorage;
    private final UserAgentHelper             userAgentHelper;
    private final RequestHelper               requestHelper;
    private final TazSettings                 settings;
    private final PaperRepository             paperRepository;
    private final AccountHelper               accountHelper;
    private final ResourceRepository          resourceRepository;
    private final StoreRepository             storeRepository;
    private final Resources                   appResources;
    private final UserDeviceInfo              userDeviceInfo;

    private static volatile DownloadManager instance;

    public static DownloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null) {
                    instance = new DownloadManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private DownloadManager(Context context) {
        mDownloadManager = ((android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE));
//        mContext = context;
        mStorage = StorageManager.getInstance(context);
        userAgentHelper = UserAgentHelper.getInstance(context);
        requestHelper = RequestHelper.getInstance(context);
        settings = TazSettings.getInstance(context);
        paperRepository = PaperRepository.getInstance(context);
        accountHelper = AccountHelper.getInstance(context);
        resourceRepository = ResourceRepository.getInstance(context);
        storeRepository = StoreRepository.getInstance(context);
        appResources = context.getResources();
        userDeviceInfo = UserDeviceInfo.getInstance(context);
    }

    public Uri getUriForDownloadedFile(long downloadId) {
        return mDownloadManager.getUriForDownloadedFile(downloadId);
    }

    public static class DownloadManagerResult {
        public enum STATE {SUCCESS, NOMANAGER, NOTALLOWED, NOSPACE, UNKNOWN}

        private STATE state      = STATE.UNKNOWN;
        private long  downloadId = -1;
        private String details;

        public void setDownloadId(long downloadId) {
            this.downloadId = downloadId;
        }

        public void setState(STATE state) {
            this.state = state;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public long getDownloadId() {
            return downloadId;
        }

        public STATE getState() {
            return state;
        }

        public String getDetails() {
            return details;
        }
    }

    @WorkerThread
    public DownloadManagerResult downloadPaper(String bookId, boolean wifiOnly) {

        DownloadManagerResult result = new DownloadManagerResult();

        try {

            Paper paper = paperRepository.getPaperWithBookId(bookId);
            if (paper == null) throw new Paper.PaperNotFoundException();

            if (settings.isDemoMode() && !paper.isDemo()) throw new DownloadNotAllowedException();

            Timber.i("requesting paper download: %s", paper);

            Uri downloadUri = requestHelper.addToUri(Uri.parse(paper.getLink()));

            Request request;
            try {
                request = new Request(downloadUri);
            } catch (Exception e1) {
                String httpUrl = paper.getLink()
                                      .replace("https://", "http://");
                request = new Request(requestHelper.addToUri(Uri.parse(httpUrl)));
            }
            addUserAgent(request);

            if (!TextUtils.isEmpty(paper.getPublication())) {
                request.addRequestHeader("Authorization",
                                         "Basic " + Base64.encodeToString((accountHelper.getUser(AccountHelper.ACCOUNT_DEMO_USER) + ":" + accountHelper.getPassword(
                                                 AccountHelper.ACCOUNT_DEMO_PASS)).getBytes(), Base64.NO_WRAP));
            }

            File destinationFile = mStorage.getDownloadFile(paper);

            if (destinationFile == null)
                throw new DownloadNotAllowedException("Fehler beim Ermitteln des Downloadverzeichnisses.");
            if (destinationFile.exists()) {
                if (!destinationFile.delete()) Timber.w("Cannot delete file %s", destinationFile.getAbsolutePath());
            }


            assertEnoughSpaceForDownload(destinationFile.getParentFile(), calculateBytesNeeded(paper.getLen()));

            request.setDestinationUri(Uri.fromFile(destinationFile));

            request.setMimeType("application/zip");

            request.setTitle(paper.getTitelWithDate(appResources));

            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
            request.setVisibleInDownloadsUi(false);
            if (wifiOnly) request.setAllowedNetworkTypes(Request.NETWORK_WIFI);

            long downloadId = mDownloadManager.enqueue(request);
            result.setDownloadId(downloadId);

            Timber.i("... download requested at android download manager, id: %d", downloadId);

            //paper.setDownloadprogress(0);
            paper.setDownloadId(downloadId);
            paper.setDownloaded(false);
            paper.setHasUpdate(false);

            paperRepository.savePaper(paper);
            result.setState(DownloadManagerResult.STATE.SUCCESS);


            if (!TextUtils.isEmpty(paper.getResource())) {
                Resource resource = resourceRepository.getWithKey(paper.getResource());
                Store resourcePartnerStore = storeRepository.getStore(paper.getBookId(), Paper.STORE_KEY_RESOURCE_PARTNER);
                resourcePartnerStore.setValue(paper.getResource());
                storeRepository.saveStore(resourcePartnerStore);
                try {
                    enqueResource(resource, wifiOnly);
                } catch (Exception e) {
                    Timber.w(e);
                }
            }

        } catch (Paper.PaperNotFoundException e) {
            Timber.e(e);
            result.setState(DownloadManagerResult.STATE.UNKNOWN);
            result.setDetails(e.toString());
        } catch (IllegalArgumentException e) {
            Timber.e(e);
            result.setState(DownloadManagerResult.STATE.NOMANAGER);
            result.setDetails(e.toString());
        } catch (NotEnoughSpaceException e) {
            Timber.e(e);
            result.setState(DownloadManagerResult.STATE.NOSPACE);
            result.setDetails(appResources.getString(R.string.message_not_enough_space));
        } catch (DownloadNotAllowedException e) {
            Timber.e(e);
            result.setState(DownloadManagerResult.STATE.NOTALLOWED);
            result.setDetails(e.toString());
        }
        return result;
    }


    @WorkerThread
    @SuppressLint("NewApi")
    public void enqueResource(Resource resource, boolean wifiOnly) throws IllegalArgumentException, NotEnoughSpaceException {
        //Paper paper = new Paper(mContext, paperId);

        //Uri downloadUri = Uri.parse(paper.getResourceUrl());

//        Resource resource = Resource.getWithKey(mContext, paper.getResource());
        Timber.i("requesting resource download: %s", resource);

//        if (resource.getPath() == null) {
//            resource.setKey(paper.getResource());
//            mContext.getContentResolver()
//                    .insert(Resource.CONTENT_URI, resource.getContentValues());
//        }

        if (!resource.isDownloaded()) {
//            resource.setFileHash(paper.getResourceFileHash());
//            resource.setUrl(paper.getResourceUrl());
//            resource.setLen(paper.getResourceLen());
//            resource.setUrl(paper.getResourceUrl());


            if (resource.isDownloading()) {
                Timber.w("Resource is downloading, checking for state");
                DownloadState state = getDownloadState(resource.getDownloadId());
                switch (state.getStatus()) {
                    case DownloadState.STATUS_PENDING:
                    case DownloadState.STATUS_RUNNING:
                    case DownloadState.STATUS_PAUSED:
                        Timber.e("Download already running");
                        return;
                    default:
                        mDownloadManager.remove(resource.getDownloadId());
                        break;
                }
            }


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
                request = new Request(requestHelper.addToUri(Uri.parse(httpUrl)));
            }
            addUserAgent(request);

            File destinationFile = mStorage.getDownloadFile(resource);
            if (destinationFile.exists()) {
                if (!destinationFile.delete()) Timber.w("Cannot delete file %s", destinationFile.getAbsolutePath());
            }

            assertEnoughSpaceForDownload(destinationFile.getParentFile(), calculateBytesNeeded(resource.getLen()));

            request.setDestinationUri(Uri.fromFile(destinationFile));
            if (wifiOnly) request.setAllowedNetworkTypes(Request.NETWORK_WIFI);
            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
            request.setVisibleInDownloadsUi(false);
            request.setMimeType("application/zip");

            long downloadId = mDownloadManager.enqueue(request);

            Timber.i("... download requested at android download manager, id: %d", downloadId);

            resource.setDownloadId(downloadId);

            resourceRepository.saveResource(resource);

//            mContext.getContentResolver()
//                    .insert(Resource.CONTENT_URI, resource.getContentValues());
//            mContext.getContentResolver()
//                    .update(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()),
//                            resource.getContentValues(),
//                            null,
//                            null);
        }

    }

    public void downloadUpdate() {

        List<String> supportedArchs = userDeviceInfo.getSupportedArchList();
        String arch = null;
        if (supportedArchs != null && supportedArchs.size() > 0) {
            int bestArch = 0;
            for (String supportedArch : supportedArchs) {
                int newArch = UserDeviceInfo.getWeightForArch(supportedArch);
                if (newArch > bestArch) bestArch = newArch;
            }
            if (bestArch == 2 || bestArch == 3 || bestArch == 6 || bestArch == 7) {
                //Filter for build archTypes
                arch = UserDeviceInfo.getArchForWeight(bestArch);
            }
            if (TextUtils.isEmpty(arch)) arch = "universal";
        }


        StringBuilder fileName = new StringBuilder("tazapp-").append(BuildConfig.FLAVOR)
                                                             .append("-")
                                                             .append(arch)
                                                             .append("-")
                                                             .append(BuildConfig.BUILD_TYPE)
                                                             .append(".apk");

        Uri uri = Uri.parse(BuildConfig.APKURL)
                     .buildUpon()
                     .appendEncodedPath(fileName.toString())
                     .build();


        File updateFile = new File(mStorage.getUpdateAppCache(), fileName.toString());

        List<DownloadState> allUpdateDownloads = getDownloadStatesForUrl(uri.toString());
        for (DownloadState updateDownload : allUpdateDownloads) {
            mDownloadManager.remove(updateDownload.getDownloadId());
        }

        if (updateFile.exists()) updateFile.delete();

        Request request = new Request(uri);
        request.setMimeType("application/vnd.android.package-archive");
        request.setTitle(appResources.getString(R.string.update_app_download_notification,
                                                appResources.getString(R.string.app_name)));
        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(true);
        request.setDestinationUri(Uri.fromFile(updateFile));
        mDownloadManager.enqueue(request);

    }

    private void addUserAgent(Request request) {
        request.addRequestHeader(UserAgentHelper.USER_AGENT_HEADER_NAME, userAgentHelper.getUserAgentHeaderValue());
    }



    public void cancelDownload(long downloadId) {
        DownloadState state = getDownloadState(downloadId);
        if (state != null && state.getStatus() != DownloadState.STATUS_SUCCESSFUL) {
            if (mDownloadManager.remove(downloadId) > 0) {
                Paper paper = paperRepository.getPaperWithDownloadId(downloadId);
                if (paper != null) {
                    paperRepository.deletePaper(paper);
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

    public List<DownloadState> getDownloadStatesForUrl(String url) {
        List<DownloadState> result = new ArrayList<>();
        android.app.DownloadManager.Query q = new android.app.DownloadManager.Query();
        Cursor cursor = null;
        try {
            cursor = mDownloadManager.query(q);
            if (cursor != null && cursor.moveToFirst()) {
                String uri = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_URI));
                if (uri.equals(url)) {
                    long downloadId = cursor.getLong(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_ID));
                    result.add(new DownloadState(downloadId));
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    public class DownloadState {


        public static final int STATUS_SUCCESSFUL = android.app.DownloadManager.STATUS_SUCCESSFUL;
        public static final int STATUS_FAILED     = android.app.DownloadManager.STATUS_FAILED;
        public static final int STATUS_PAUSED     = android.app.DownloadManager.STATUS_PAUSED;
        public static final int STATUS_PENDING    = android.app.DownloadManager.STATUS_PENDING;
        public static final int STATUS_RUNNING    = android.app.DownloadManager.STATUS_RUNNING;
        public static final int STATUS_NOTFOUND   = 0;

        private int    status;
        private int    reason;
        private long   bytesTotal;
        private long   bytesDownloaded;
        private String uri;
        private String title;
        private String description;
        private long   downloadId;
        private long   lastModified;
        private Uri    localUri;
        private Uri    mediaProviderUri;
        private String mediaType;

        public DownloadState(long downloadId) {
            this.downloadId = downloadId;
            android.app.DownloadManager.Query q = new android.app.DownloadManager.Query();
            q.setFilterById(downloadId);
            Cursor cursor = null;
            try {
                cursor = mDownloadManager.query(q);
                if (cursor != null && cursor.moveToFirst()) {
                    status = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS));
                    reason = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_REASON));
                    bytesDownloaded = cursor.getLong(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    bytesTotal = cursor.getLong(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    uri = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_URI));
                    title = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TITLE));
                    description = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_DESCRIPTION));
                    lastModified = cursor.getLong(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP));
                    localUri = parseUriWithoutException(cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_LOCAL_URI)));
                    mediaProviderUri = parseUriWithoutException(cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_MEDIAPROVIDER_URI)));
                    mediaType = cursor.getString(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_MEDIA_TYPE));

                }
            } finally {
                if (cursor != null) cursor.close();
            }
            // Assume donwload is Canceled if no status was found
        }

        public long getDownloadId() {
            return downloadId;
        }

        /**
         * @return Status des Downloads nach {@link android.app.DownloadManager}
         * @see android.app.DownloadManager
         */
        public int getStatus() {
            return status;
        }

        /**
         * @return Grund für den Status von getStatus() des Downloads nach {@link android.app.DownloadManager}
         * @see android.app.DownloadManager
         */
        public int getReason() {
            return reason;
        }

        public long getBytesTotal() {
            return bytesTotal;
        }

        public long getBytesDownloaded() {
            return bytesDownloaded;
        }

        public int getDownloadProgress() {
            if (bytesTotal > 0) return (int) (bytesDownloaded * 100 / bytesTotal);
            else return 0;
        }

        public Uri getUri() {
            try {
                return Uri.parse(uri);
            } catch (Exception e) {
                return null;
            }
        }

        public String getDescription() {
            return description;
        }

        public String getTitle() {
            return title;
        }

        public long getLastModified() {
            return lastModified;
        }

        public Uri getLocalUri() {
            return localUri;
        }

        public Uri getMediaProviderUri() {
            return mediaProviderUri;
        }

        public String getMediaType() {
            return mediaType;
        }

        /**
         * Gibt einen lesbaren Text des Grundes für den DownloadStatus zurück... Dieser ist in /res/values/strings_download.xml
         * hinterlegt und kann im erbenden Projekt überschrieben werden.
         */
        public String getReasonText() {
            int id = appResources.getIdentifier("download_reason_" + getReason(), "string", userDeviceInfo.getPackageName());
            if (id == 0) id = R.string.download_reason_notext;
            StringBuilder builder = new StringBuilder(appResources.getString(id)).append("(")
                                                                                 .append(getReason())
                                                                                 .append(")");
            return builder.toString();
        }

        /**
         * Gibt einen lesbaren Text des DownloadStatus zurück... Dieser ist in /res/values/strings_download.xml hinterlegt und
         * kann im erbenden Projekt überschrieben werden.
         */
        public String getStatusText() {
            switch (getStatus()) {
                case STATUS_SUCCESSFUL:
                    return appResources.getString(R.string.download_status_successful);
                case STATUS_FAILED:
                    return appResources.getString(R.string.download_status_failed);
                case STATUS_PAUSED:
                    return appResources.getString(R.string.download_status_paused);
                case STATUS_PENDING:
                    return appResources.getString(R.string.download_status_pending);
                case STATUS_RUNNING:
                    return appResources.getString(R.string.download_status_running);
                default:
                    return appResources.getString(R.string.download_status_unknown);
            }
        }

        private Uri parseUriWithoutException(String uri) {
            try {
                return Uri.parse(uri);
            } catch (Exception e) {
                return null;
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
        int  progress;

        public DownloadProgressThread(long downloadId, long paperId) {
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

                if (progress != oldProgress) {
                    EventBus.getDefault()
                            .post(new DownloadProgressEvent(paperId, progress));
                }
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

    private static void assertEnoughSpaceForDownload(File dir, long requested) throws NotEnoughSpaceException {
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


    public static class DownloadNotAllowedException extends ReadableException {
        public DownloadNotAllowedException() {
        }

        public DownloadNotAllowedException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static class NotEnoughSpaceException extends ReadableException {
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
