package de.thecode.android.tazreader.sync;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.CoverDownloadedEvent;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.download.NotificationHelper;
import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;
import de.thecode.android.tazreader.okhttp3.RequestHelper;
import de.thecode.android.tazreader.start.ScrollToPaperEvent;
import de.thecode.android.tazreader.utils.Connection;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.HttpUrl;
import timber.log.Timber;

/**
 * Created by Mate on 11.02.2017.
 */

public class SyncService extends IntentService {

    private static final String PLIST_KEY_ISSUES = "issues";

    public static final String ARG_START_DATE = "startDate";
    public static final String ARG_END_DATE   = "endDate";

    //FileCacheCoverHelper mCoverHelper;

    private Paper moveToPaperAtEnd;

    private long minDataValidUntil = Long.MAX_VALUE;

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        EventBus.getDefault()
                .postSticky(new SyncStateChangedEvent(true));

        //If migration is needed dont sync
        if (TazSettings.getInstance(this)
                       .getPrefInt(TazSettings.PREFKEY.PAPERMIGRATEFROM, 0) != 0) return;
        //if (TazSettings.getPrefBoolean(this, TazSettings.PREFKEY.PAPERMIGRATERUNNING, false)) return;


        //mCoverHelper = new FileCacheCoverHelper(StorageManager.getInstance(this));

        String startDate = null;
        String endDate = null;
        if (intent.hasExtra(ARG_START_DATE) && intent.hasExtra(ARG_END_DATE)) {
            startDate = intent.getStringExtra(ARG_START_DATE);
            endDate = intent.getStringExtra(ARG_END_DATE);
        }

        // If ForceSync it's now done
        TazSettings.getInstance(this)
                   .setPref(TazSettings.PREFKEY.FORCESYNC, false);

        // AutoDelete
        if (TazSettings.getInstance(this)
                       .getPrefBoolean(TazSettings.PREFKEY.AUTODELETE, false)) {

            autoDeleteTask();
        }

        NSDictionary plist = callPlist(startDate, endDate, 5);
        if (plist != null) {
            handlePlist(plist);
        }

        if (startDate == null && endDate == null) {
            downloadLatestRessource();
        }

        cleanUpResources();

        reloadDataForImportedPapers();

        // AutoUpdate und Download der nächsten Ausgabe
        Paper tomorrowPaper = checkForTommorrowPaper();

        if (TazSettings.getInstance(this)
                       .getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false) && tomorrowPaper != null) {
            boolean connectionCheck = false;
            if (TazSettings.getInstance(this)
                           .getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD_WIFI, true)) {
                if (Connection.getConnectionType(this) >= Connection.CONNECTION_FAST) connectionCheck = true;
            } else {
                if (Connection.getConnectionType(this) >= Connection.CONNECTION_MOBILE_ROAMING) connectionCheck = true;
            }
            if (connectionCheck) {
                if (!tomorrowPaper.isDownloaded() && !tomorrowPaper.isDownloading()) downloadPaper(tomorrowPaper);
            }
        }

        long nextPlannedRunAt = TazSettings.getInstance(this)
                                           .getSyncServiceNextRun();
        if (nextPlannedRunAt <= System.currentTimeMillis()) nextPlannedRunAt = Long.MAX_VALUE;
        minDataValidUntil = Math.min(nextPlannedRunAt, minDataValidUntil);

        SyncHelper.setAlarmManager(this, tomorrowPaper != null, minDataValidUntil);

        EventBus.getDefault()
                .postSticky(new SyncStateChangedEvent(false));

        if (moveToPaperAtEnd != null) {
            EventBus.getDefault()
                    .post(new ScrollToPaperEvent(moveToPaperAtEnd.getId()));
            moveToPaperAtEnd = null;
        }

        while (!imagePreloadingQueue.isEmpty()) {
//            Timber.v("Waiting for imageQueue to be empty...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
//                Timber.e(e);
            }
        }

    }

    private void cleanUpResources() {
        List<Paper> allPapers = Paper.getAllPapers(this);
        List<Resource> keepResources = new ArrayList<>();
        if (allPapers != null) {
            for (Paper paper : allPapers) {
                if (paper.isDownloaded() || paper.isDownloading()) {
                    Resource resource = paper.getResourcePartner(this);
                    if (resource != null && !keepResources.contains(resource)) keepResources.add(resource);
                }
            }
        }
        List<Resource> deleteResources = Resource.getAllResources(this);
        Paper latestPaper = Paper.getLatestPaper(this);
        if (latestPaper != null) deleteResources.remove(Resource.getWithKey(this, latestPaper.getResource()));
        for (Resource keepResource : keepResources) {
            if (deleteResources.contains(keepResource)) {
                deleteResources.remove(keepResource);
            }
        }
        for (Resource deleteResource : deleteResources) {
            deleteResource.delete(this);
        }
    }

    private Paper checkForTommorrowPaper() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_YEAR, 1); // <--
        Date tomorrow = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Cursor tomorrowCursor = this.getContentResolver()
                                    .query(Paper.CONTENT_URI,
                                           null,
                                           Paper.Columns.DATE + " LIKE '" + sdf.format(tomorrow) + "'",
                                           null,
                                           null);
        try {
            if (tomorrowCursor.moveToNext()) {
                // Ausgabe von morgen ist da
                Timber.d("Ausgabe von morgen veröffentlicht");
                return new Paper(tomorrowCursor);
            } else {
                // Ausgabe von morgen ist noch nicht da
                Timber.d("Ausgabe von morgen noch nicht veröffentlicht");
            }
        } finally {
            tomorrowCursor.close();
        }
        return null;
    }

    private void downloadPaper(Paper paper) {
        try {
            DownloadManager.getInstance(this)
                           .enquePaper(paper.getId());
        } catch (IllegalArgumentException | DownloadManager.DownloadNotAllowedException | Paper.PaperNotFoundException ignored) {
        } catch (DownloadManager.NotEnoughSpaceException e) {
            NotificationHelper.showDownloadErrorNotification(this,
                                                             this.getString(R.string.message_not_enough_space),
                                                             paper.getId());
        }
    }

    private void downloadLatestRessource() {
        Paper latestPaper = Paper.getLatestPaper(this);
        if (latestPaper != null) {
            Resource latestResource = Resource.getWithKey(this, latestPaper.getResource());
            if (latestResource != null && !latestResource.isDownloaded() && !latestResource.isDownloading()) {
                try {
                    DownloadManager.getInstance(this)
                                   .enqueResource(latestResource);
                } catch (DownloadManager.NotEnoughSpaceException e) {
                    Timber.e(e);
                }
            }
        }
    }

    private void reloadDataForImportedPapers() {
        Cursor importedPaperCursor = this.getContentResolver()
                                         .query(Paper.CONTENT_URI,
                                                null,
                                                Paper.TABLE_NAME + "." + Paper.Columns.IMAGE + " IS NULL",
                                                null,
                                                null);
        try {
            while (importedPaperCursor.moveToNext()) {
                Paper importedPaper = new Paper(importedPaperCursor);

                if (TextUtils.isEmpty(importedPaper.getImage())) {
                    handlePlist(callPlist(importedPaper.getDate(), importedPaper.getDate(), 1));
                }
            }
        } finally {
            importedPaperCursor.close();
        }
    }

    private void autoDeleteTask() {
        long currentOpenPaperId = TazSettings.getInstance(this)
                                             .getPrefLong(TazSettings.PREFKEY.LASTOPENPAPER, -1L);
        Timber.d("+++++++ TazSettings: Current Paper SyncAdapter View: %s", currentOpenPaperId);

        int papersToKeep = TazSettings.getInstance(this)
                                      .getPrefInt(TazSettings.PREFKEY.AUTODELETE_VALUE, 0);
        if (papersToKeep > 0) {
            Cursor deletePapersCursor = this.getContentResolver()
                                            .query(Paper.CONTENT_URI,
                                                   null,
                                                   Paper.Columns.ISDOWNLOADED + "=1 AND " + Paper.Columns.IMPORTED + "!=1 AND " + Paper.Columns.KIOSK + "!=1",
                                                   null,
                                                   Paper.Columns.DATE + " DESC");
            try {
                int counter = 0;
                while (deletePapersCursor.moveToNext()) {
                    if (counter >= papersToKeep) {
                        Paper deletePaper = new Paper(deletePapersCursor);
                        Timber.d("PaperId: %s (currentOpen:%s)", deletePaper.getId(), currentOpenPaperId);
                        if (!deletePaper.getId()
                                        .equals(currentOpenPaperId)) {
                            boolean safeToDelete = true;
                            String bookmarksJsonString = deletePaper.getStoreValue(this, Paper.STORE_KEY_BOOKMARKS);
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
                                deletePaper.delete(this);
                            }
                        }
                    }
                    counter++;
                }
            } finally {
                deletePapersCursor.close();
            }
        }
    }

    private NSDictionary callPlist(String startDate, String endDate, int retry) {
        HttpUrl url;
        if (!TextUtils.isEmpty(startDate) && !TextUtils.isEmpty(endDate)) {
            url = HttpUrl.parse(String.format(BuildConfig.PLISTARCHIVURL, startDate, endDate));
        } else {
            url = HttpUrl.parse(BuildConfig.PLISTURL);
        }
        while (retry > 0) {
            okhttp3.Call call = OkHttp3Helper.getInstance(this)
                                             .getCall(url,
                                                      RequestHelper.getInstance(this)
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
                retry--;
            }
        }
        return null;
    }

    public void handlePlist(NSDictionary root) {
        Publication publication = new Publication(root);

        Cursor pubCursor = this.getContentResolver()
                               .query(Publication.CONTENT_URI,
                                      null,
                                      Publication.Columns.ISSUENAME + " LIKE '" + publication.getIssueName() + "'",
                                      null,
                                      null);

        long publicationId;
        String publicationTitle = publication.getName();
        long validUntil = publication.getValidUntil();
        minDataValidUntil = Math.min(minDataValidUntil, validUntil * 1000);

        try {
            if (pubCursor.moveToNext()) {
                Publication oldPupdata = new Publication(pubCursor);
                publicationId = oldPupdata.getId();

                oldPupdata.setCreated(publication.getCreated());
                oldPupdata.setImage(publication.getImage());
                oldPupdata.setIssueName(publication.getIssueName());
                oldPupdata.setName(publication.getName());
                oldPupdata.setTypeName(publication.getTypeName());
                oldPupdata.setUrl(publication.getUrl());
                oldPupdata.setValidUntil(publication.getValidUntil());

                Uri updateUri = ContentUris.withAppendedId(Publication.CONTENT_URI, publicationId);
                this.getContentResolver()
                    .update(updateUri, oldPupdata.getContentValues(), null, null);
            } else {
                Uri newPublicationUri = this.getContentResolver()
                                            .insert(Publication.CONTENT_URI, publication.getContentValues());
                publicationId = ContentUris.parseId(newPublicationUri);
            }
        } finally {
            pubCursor.close();
        }

        NSObject[] issues = ((NSArray) root.objectForKey(PLIST_KEY_ISSUES)).getArray();
        for (NSObject issue : issues) {
            Paper newPaper = new Paper((NSDictionary) issue);
            newPaper.setPublicationId(publicationId);
            newPaper.setTitle(publicationTitle);
            newPaper.setValidUntil(validUntil);

            Uri bookIdUri = Paper.CONTENT_URI.buildUpon()
                                             .appendPath(newPaper.getBookId())
                                             .build();
            Cursor cursor = this.getContentResolver()
                                .query(bookIdUri, null, /*Paper.Columns.IMPORTED + "=0 AND " + Paper.Columns.KIOSK + "=0"*/
                                       null, null, null);
            try {
                if (cursor.moveToNext()) {
                    Paper oldPaper = new Paper(cursor);
                    if (!newPaper.equals(oldPaper)) {
                        Timber.d("found difference in paper");
                        oldPaper.setImage(newPaper.getImage());
                        boolean reloadImage = !new EqualsBuilder().append(oldPaper.getImageHash(), newPaper.getImageHash())
                                                                  .isEquals();
                        oldPaper.setImageHash(newPaper.getImageHash());
                        if (!oldPaper.isImported() && !oldPaper.isKiosk()) {

                            if (!new EqualsBuilder().append(oldPaper.getLastModified(), newPaper.getLastModified())
                                                    .isEquals()) {
                                oldPaper.setLastModified(newPaper.getLastModified());
                                if (!new EqualsBuilder().append(oldPaper.getFileHash(), newPaper.getFileHash())
                                                        .isEquals() && (oldPaper.isDownloaded() || oldPaper.isDownloading()))
                                    oldPaper.setHasupdate(true);
                            }
                            oldPaper.setLink(newPaper.getLink());
                            oldPaper.setLen(newPaper.getLen());
                            oldPaper.setFileHash(newPaper.getFileHash());
                            oldPaper.setResource(newPaper.getResource());
//                            oldPaper.setResourceFileHash(newPaper.getResourceFileHash());
//                            oldPaper.setResourceUrl(newPaper.getResourceUrl());
//                            oldPaper.setResourceLen(newPaper.getResourceLen());
                            oldPaper.setDemo(newPaper.isDemo());
                            oldPaper.setValidUntil(newPaper.getValidUntil());
                        }
                        if (oldPaper.getPublicationId() == null) {
                            oldPaper.setPublicationId(publicationId);
                        }
                        this.getContentResolver()
                            .update(oldPaper.getContentUri(), oldPaper.getContentValues(), null, null);
                        if (reloadImage) preLoadImage(oldPaper);
                        //setMoveToPaperAtEnd(oldPaper);
                    }
                    newPaper = oldPaper;
                } else {
                    Timber.d("notfound");

                    long newPaperId = ContentUris.parseId(this.getContentResolver()
                                                              .insert(Paper.CONTENT_URI, newPaper.getContentValues()));
                    newPaper.setId(newPaperId);
                    setMoveToPaperAtEnd(newPaper);
                    preLoadImage(newPaper);
                }
                Resource resource = Resource.getWithKey(this, newPaper.getResource());
                if (resource == null) {
                    resource = new Resource((NSDictionary) issue);
                    this.getContentResolver()
                        .insert(Resource.CONTENT_URI, resource.getContentValues());
                }
            } finally {
                cursor.close();
            }


        }
    }

    private final Set<Object> imagePreloadingQueue = new HashSet<>();

    private void preLoadImage(Paper paper) {
        //Timber.v("preloading image %s", url);
        PreloadImageCallback callback = new PreloadImageCallback(paper) {
            @Override
            public void onSuccess(Paper paper) {
                EventBus.getDefault()
                        .post(new CoverDownloadedEvent(paper.getId()));
                imagePreloadingQueue.remove(this);
            }

            @Override
            public void onError(Paper paper) {
                imagePreloadingQueue.remove(this);
            }
        };
        imagePreloadingQueue.add(callback);
        Picasso.with(this)
               .load(paper.getImage())
               .fetch(callback);
    }

    public abstract static class PreloadImageCallback implements Callback {

        final Paper paper;


        protected PreloadImageCallback(Paper paper) {
            this.paper = paper;

        }

        @Override
        public void onSuccess() {
            onSuccess(paper);
        }

        @Override
        public void onError() {
            onError(paper);
        }

        public abstract void onSuccess(Paper paper);

        public abstract void onError(Paper paper);
    }


    private void setMoveToPaperAtEnd(Paper paper) {
        try {
            if (moveToPaperAtEnd == null || paper.getDateInMillis() > moveToPaperAtEnd.getDateInMillis()) {
                moveToPaperAtEnd = paper;
            }
        } catch (ParseException e) {
            Timber.e(e);
            moveToPaperAtEnd = paper;
        }
    }

}
