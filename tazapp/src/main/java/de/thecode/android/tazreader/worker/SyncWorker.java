package de.thecode.android.tazreader.worker;

import android.content.Context;
import android.text.TextUtils;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.TazApplicationKt;
import de.thecode.android.tazreader.data.DownloadState;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.PublicationRepository;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.TazDownloadManager;
import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;
import de.thecode.android.tazreader.okhttp3.RequestHelper;
import de.thecode.android.tazreader.sync.SyncErrorEvent;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;
import okhttp3.HttpUrl;
import timber.log.Timber;

public class SyncWorker extends LoggingWorker {


    public static final String TAG = BuildConfig.FLAVOR + "_sync_job";

    private static final String PLIST_KEY_ISSUES = "issues";

    public static final String ARG_START_DATE        = "startDate";
    public static final String ARG_END_DATE          = "endDate";
    public static final String ARG_INITIATED_BY_USER = "initiatedByUser";


    private final PaperRepository       paperRepository;
    private final ResourceRepository    resourceRepository;
    private final PublicationRepository publicationRepository;
    private final TazSettings           settings;
    private final StoreRepository       storeRepository;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        paperRepository = PaperRepository.getInstance(context);
        resourceRepository = ResourceRepository.getInstance(context);
        publicationRepository = PublicationRepository.getInstance(context);
        settings = TazSettings.getInstance(context);
        storeRepository = StoreRepository.getInstance(context);
    }

    @NonNull
    @Override
    public Result doBackgroundWork() {
//        EventBus.getDefault()
//                .postSticky(new SyncStateChangedEvent(true));

        String startDate = getInputData().getString(ARG_START_DATE);
        String endDate = getInputData().getString(ARG_END_DATE);
        boolean initByUser = getInputData().getBoolean(ARG_INITIATED_BY_USER, false);

        NSDictionary plist = callPlist(startDate, endDate);

        if (plist == null) {
            if (initByUser) {
                EventBus.getDefault()
                        .post(new SyncErrorEvent(getApplicationContext().getString(R.string.sync_job_plist_empty)));
                return Result.success();
            } else {
                return Result.retry();
            }
        } else {
            if (!initByUser) autoDeleteTask();
            handlePlist(plist);
        }
        if (startDate == null && endDate == null) {
            downloadLatestResource();
        }

        cleanUpResources();

        Paper latestPaper = paperRepository.getLatestPaper();
        if (latestPaper != null) {
            if (settings.getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false) && !TazApplicationKt.getAccountHelper().isDemo()) {
                Store autoDownloadedStore = storeRepository.getStore(latestPaper.getBookId(), Paper.STORE_KEY_AUTO_DOWNLOADED);
                boolean isAutoDownloaded = Boolean.parseBoolean(autoDownloadedStore.getValue("false"));
                if (!isAutoDownloaded /*&& (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) < latestPaper.getDateInMillis()*/) {
                    boolean wifiOnly = settings.getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD_WIFI, false);
                    if (paperRepository.getDownloadStateForPaper(latestPaper.getBookId()) == DownloadState.NONE) {
                        TazDownloadManager.Result result = TazApplicationKt.getDownloadManager()
                                                                           .downloadPaper(latestPaper.getBookId(), wifiOnly);
                        if (result.getState() == TazDownloadManager.Result.STATE.SUCCESS) {
                            autoDownloadedStore.setValue("true");
                            storeRepository.saveStore(autoDownloadedStore);
                        } else if (result.getState() == TazDownloadManager.Result.STATE.NOSPACE) {
                            TazApplicationKt.getNotificationUtils()
                                            .showDownloadErrorNotification(latestPaper,
                                                                           getApplicationContext().getString(R.string.message_not_enough_space));
                        }
                    }
                }

            }
        }
        return Result.success();
    }

    private void cleanUpResources() {
        // TODO
//        List<Paper> allPapers = paperRepository.getAllPapers();
//        List<Resource> keepResources = new ArrayList<>();
//        if (allPapers != null) {
//            for (Paper paper : allPapers) {
//                if (paperRepository.getDownloadStateForPaper(paper.getBookId()) != DownloadState.NONE) {
//                    Resource resource = resourceRepository.getResourceForPaper(paper);
//                    if (resource != null && !keepResources.contains(resource)) keepResources.add(resource);
//                }
//            }
//        }
//        List<Resource> deleteResources = resourceRepository.getAllResources();
//        Paper latestPaper = paperRepository.getLatestPaper();
//
//        if (latestPaper != null) deleteResources.remove(resourceRepository.getWithKey(latestPaper.getResource()));
//        for (Resource keepResource : keepResources) {
//            deleteResources.remove(keepResource);
//        }
//        for (Resource deleteResource : deleteResources) {
//            resourceRepository.deleteResource(deleteResource);
//        }
    }

    private void handlePlist(NSDictionary root) {
        Publication publication = new Publication(root);
        String publicationTitle = publication.getName();
        long validUntil = publication.getValidUntil();
        SyncWorker.scheduleJobIn(TimeUnit.SECONDS.toMillis(validUntil) - System.currentTimeMillis());

        publicationRepository.savePublication(publication);

        TazApplicationKt.getUpdate()
                        .setLatestVersion(publication.getAppAndroidVersion());


        NSObject[] issues = ((NSArray) root.objectForKey(PLIST_KEY_ISSUES)).getArray();
        List<Paper> newPapers = new ArrayList<>();
        for (NSObject issue : issues) {
            Paper newPaper = new Paper((NSDictionary) issue);
            newPaper.setPublication(publication.getIssueName());
            newPaper.setTitle(publicationTitle);
            newPaper.setValidUntil(validUntil);

            boolean loadImage = true;

            Paper oldPaper = paperRepository.getPaperWithBookId(newPaper.getBookId());
            if (oldPaper != null) {
                loadImage = !new EqualsBuilder().append(oldPaper.getImageHash(), newPaper.getImageHash())
                                                .isEquals();
            }
            if (loadImage) preLoadImage(newPaper);
            newPapers.add(newPaper);
            Resource resource = resourceRepository.getWithKey(newPaper.getResource());
            if (resource == null) {
                resourceRepository.saveResource(new Resource((NSDictionary) issue));
            }
        }
        paperRepository.savePapers(newPapers);
    }

    private NSDictionary callPlist(String startDate, String endDate) {
        HttpUrl url;
        if (!TextUtils.isEmpty(startDate) && !TextUtils.isEmpty(endDate)) {
            url = HttpUrl.parse(String.format(BuildConfig.PLISTARCHIVURL, startDate, endDate));
        } else {
            url = HttpUrl.parse(BuildConfig.PLISTURL);
        }
        okhttp3.Call call = OkHttp3Helper.getInstance(getApplicationContext())
                                         .getCall(url,
                                                  RequestHelper.getInstance(getApplicationContext())
                                                               .getOkhttp3RequestBody());
        try {
            okhttp3.Response response = call.execute();
            if (response.isSuccessful()) {
                return (NSDictionary) PropertyListParser.parse(response.body()
                                                                       .bytes());
            }
            throw new IOException(response.body()
                                          .string());
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    private void preLoadImage(Paper paper) {
        Picasso.with(getApplicationContext())
               .load(paper.getImage())
               .fetch();
    }

    private void downloadLatestResource() {
        Paper latestPaper = paperRepository.getLatestPaper();
        if (latestPaper != null) {
            Resource latestResource = resourceRepository.getWithKey(latestPaper.getResource());
            DownloadState downloadState = resourceRepository.getDownloadState(latestResource.getKey());
            if (latestResource != null && downloadState == DownloadState.NONE) {
                TazDownloadManager.Companion.getInstance()
                                            .downloadResource(latestResource.getKey(), false, false);
//                try {
//                    OldDownloadManager.getInstance(getApplicationContext())
//                                      .enqueResource(latestResource, false);
//                } catch (OldDownloadManager.NotEnoughSpaceException e) {
//                    Timber.e(e);
//                }
            }
        }
    }

    private void autoDeleteTask() {
        if (settings.getPrefBoolean(TazSettings.PREFKEY.AUTODELETE, false)) {

//            long currentOpenPaperId = TazSettings.getInstance(getContext())
//                                                 .getPrefLong(TazSettings.PREFKEY.LASTOPENPAPER, -1L);
//            Timber.d("+++++++ TazSettings: Current Paper SyncAdapter View: %s", currentOpenPaperId);

            //TODO Get BookId from Setting, an set it in Reader
            String currentOpenPaperBookId = null;

            int papersToKeep = settings.getPrefInt(TazSettings.PREFKEY.AUTODELETE_VALUE, 0);
            if (papersToKeep > 0) {
                List<Paper> allPapers = paperRepository.getAllPapers();
                int counter = 0;
                for (Paper paper : allPapers) {
                    if (paperRepository.getDownloadStateForPaper(paper.getBookId()) == DownloadState.READY/* && !paper.isImported() && !paper.isKiosk()*/) {
                        if (counter >= papersToKeep) {
                            Timber.d("PaperId: %s (currentOpen:%s)", paper.getBookId(), currentOpenPaperBookId);
                            if (!paper.getBookId()
                                      .equals(currentOpenPaperBookId)) {
                                boolean safeToDelete = true;
                                String bookmarksJsonString = storeRepository.getStore(paper.getBookId(),
                                                                                      Paper.STORE_KEY_BOOKMARKS)
                                                                            .getValue();
                                if (!TextUtils.isEmpty(bookmarksJsonString)) {
                                    try {
                                        JSONArray bookmarks = new JSONArray(bookmarksJsonString);
                                        if (bookmarks.length() > 0) safeToDelete = false;
                                    } catch (JSONException e) {
                                        // JSON Error, better don't delete
                                        safeToDelete = false;
                                    }
                                }
                                if (safeToDelete) {
                                    paperRepository.deletePaper(paper);
                                }
                            }
                        }
                        counter++;
                    }
                }
            }
        }
    }


    public static void scheduleJobImmediately(boolean byUser) {
        scheduleJobImmediately(byUser, null, null);
    }

    public static void scheduleJobImmediately(boolean byUser, Calendar start, Calendar end) {

        WorkManager.getInstance()
                   .cancelAllWorkByTag(TAG);

        Data.Builder dataBuilder = new Data.Builder().putBoolean(ARG_INITIATED_BY_USER, byUser);

        if (start != null && end != null) {
            dataBuilder.putString(ARG_START_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(start.getTime()));
            dataBuilder.putString(ARG_END_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(end.getTime()));
        }

        WorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).setInputData(dataBuilder.build())
                                                                              .addTag(TAG)
                                                                              .build();

        WorkManager.getInstance()
                   .enqueue(request);
    }

    private static void scheduleJobIn(long latestMillis) {

        Constraints.Builder constraintsBuilder = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).setInitialDelay(latestMillis,
                                                                                                      TimeUnit.MILLISECONDS)
                                                                                     .setConstraints(constraintsBuilder.build())
                                                                                     .build();
        WorkManager.getInstance()
                   .beginUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
                   .enqueue();
    }
}
