package de.thecode.android.tazreader.sync;

import com.google.common.base.Strings;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.analytics.AnalyticsWrapper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.CoverDownloadedEvent;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.download.NotificationHelper;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.start.ScrollToPaperEvent;
import de.thecode.android.tazreader.utils.Connection;
import de.thecode.android.tazreader.volley.RequestManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Mate on 11.02.2017.
 */

public class SyncService extends IntentService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private static final String PLIST_KEY_ISSUES = "issues";

    public static final String ARG_START_DATE = "startDate";
    public static final String ARG_END_DATE   = "endDate";

    //FileCacheCoverHelper mCoverHelper;

    private Paper moveToPaperAtEnd;

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

        String url = BuildConfig.PLISTURL;

        if (intent.hasExtra(ARG_START_DATE) && intent.hasExtra(ARG_END_DATE)) {
            String startDate = intent.getStringExtra(ARG_START_DATE);
            String endDate = intent.getStringExtra(ARG_END_DATE);
            url = String.format(BuildConfig.PLISTARCHIVURL, startDate, endDate);
        }


        Long currentOpenPaperId = TazSettings.getInstance(this)
                                             .getPrefLong(TazSettings.PREFKEY.LASTOPENPAPER, -1L);
        log.debug("+++++++ TazSettings: Current Paper SyncAdapter View: {}", currentOpenPaperId);

        // If ForceSync it's now done
        TazSettings.getInstance(this)
                   .setPref(TazSettings.PREFKEY.FORCESYNC, false);

        // AutoDelete
        if (TazSettings.getInstance(this)
                       .getPrefBoolean(TazSettings.PREFKEY.AUTODELETE, false)) {

            int papersToKeep = TazSettings.getInstance(this)
                                          .getPrefInt(TazSettings.PREFKEY.AUTODELETE_VALUE, 0);
            if (papersToKeep > 0) {
                Cursor deletePapersCursor = this.getContentResolver()
                                                .query(Paper.CONTENT_URI, null,
                                                       Paper.Columns.ISDOWNLOADED + "=1 AND " + Paper.Columns.IMPORTED + "!=1 AND " + Paper.Columns.KIOSK + "!=1",
                                                       null, Paper.Columns.DATE + " DESC");
                try {
                    int counter = 0;
                    while (deletePapersCursor.moveToNext()) {
                        if (counter >= papersToKeep) {
                            Paper deletePaper = new Paper(deletePapersCursor);
                            log.debug("PaperId: {} (currentOpen:{})", deletePaper.getId(), currentOpenPaperId);
                            if (!deletePaper.getId()
                                            .equals(currentOpenPaperId)) {
                                boolean safeToDelete = true;
                                String bookmarksJsonString = deletePaper.getStoreValue(this, ReaderActivity.STORE_KEY_BOOKMARKS);
                                if (!Strings.isNullOrEmpty(bookmarksJsonString)) {
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


        int errorCounter = 0;
        int retry = 5;
        boolean pListSuccess = false;
        String response = "";
        Exception syncExeption = null;
        while (!pListSuccess && errorCounter < retry) {
            try {
                RequestFuture<String> future = RequestFuture.newFuture();
                StringRequest request = new StringRequest(Request.Method.GET, url, future, future);
                request.setShouldCache(false);
                RequestManager.getInstance(this)
                              .add(request);
                //                Volley.newRequestQueue(this)
                //                      .add(request);
                //VolleySingleton.getInstance(this).addToRequestQueue(request);
                response = future.get(30, TimeUnit.SECONDS);


                NSDictionary root = (NSDictionary) PropertyListParser.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));

                handlePlist(root);

                pListSuccess = true;


            } catch (IOException | PropertyListFormatException | ParseException | SAXException | ParserConfigurationException | InterruptedException | TimeoutException | ExecutionException e) {
                syncExeption = e;
                errorCounter++;
                log.error("", e);
            }
        }

        if (!pListSuccess) {
            AnalyticsWrapper.getInstance()
                            .logData("SyncResponse", response);
            AnalyticsWrapper.getInstance()
                            .logException(syncExeption);
        }

        // Missing cover images for imported
        Cursor importedPaperCursor = this.getContentResolver()
                                         .query(Paper.CONTENT_URI, null,
                                                Paper.TABLE_NAME + "." + Paper.Columns.IMAGE + " IS NULL", null, null);
        try {
            while (importedPaperCursor.moveToNext()) {
                Paper importedPaper = new Paper(importedPaperCursor);

                if (Strings.isNullOrEmpty(importedPaper.getImage())) {
                    try {
                        String importedPaperPlistUrl = Uri.parse(BuildConfig.PLISTURL)
                                                          .buildUpon()
                                                          .appendQueryParameter("start", importedPaper.getDate())
                                                          .appendQueryParameter("end", importedPaper.getDate())
                                                          .build()
                                                          .toString();
                        RequestFuture<String> future = RequestFuture.newFuture();
                        StringRequest request = new StringRequest(Request.Method.GET, importedPaperPlistUrl, future, future);
                        request.setShouldCache(false);
                        //                        Volley.newRequestQueue(this)
                        //                              .add(request);
                        RequestManager.getInstance(this)
                                      .add(request);
                        String imageResponse = future.get(30, TimeUnit.SECONDS);
                        NSDictionary root = (NSDictionary) PropertyListParser.parse(
                                new ByteArrayInputStream(imageResponse.getBytes("UTF-8")));
                        handlePlist(root);
                    } catch (IOException | PropertyListFormatException | ParseException | SAXException | ParserConfigurationException | InterruptedException | TimeoutException | ExecutionException e) {
                        log.error("", e);
                    }

                }
            }
        } finally {
            importedPaperCursor.close();
        }


        // AutoUpdate und Download der nächsten Ausgabe
        boolean tomorrowPaperExists = false;

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_YEAR, 1); // <--
        Date tomorrow = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Cursor tomorrowCursor = this.getContentResolver()
                                    .query(Paper.CONTENT_URI, null, Paper.Columns.DATE + " LIKE '" + sdf.format(tomorrow) + "'",
                                           null, null);
        try {
            if (tomorrowCursor.moveToNext()) {
                // Ausgabe von morgen ist da
                tomorrowPaperExists = true;
                log.debug("Ausgabe von morgen veröffentlicht");
                Paper tomorrowPaper = new Paper(tomorrowCursor);
                if (TazSettings.getInstance(this)
                               .getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false)) {
                    if (!tomorrowPaper.isDownloaded()) {
                        boolean connectionCheck = false;
                        if (TazSettings.getInstance(this)
                                       .getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD_WIFI, true)) {
                            if (Connection.getConnectionType(this) >= Connection.CONNECTION_FAST) connectionCheck = true;
                        } else {
                            if (Connection.getConnectionType(this) >= Connection.CONNECTION_MOBILE_ROAMING)
                                connectionCheck = true;
                        }
                        if (connectionCheck) {
                            try {
                                DownloadManager.getInstance(this)
                                               .enquePaper(tomorrowPaper.getId());
                            } catch (IllegalArgumentException | DownloadManager.DownloadNotAllowedException | Paper.PaperNotFoundException ignored) {
                            } catch (DownloadManager.NotEnoughSpaceException e) {
                                NotificationHelper.showDownloadErrorNotification(this, this.getString(
                                        R.string.message_not_enough_space), tomorrowPaper.getId());
                            }
                        }
                    }
                }
            } else {
                // Ausgabe von morgen ist noch nicht da
                log.debug("Ausgabe von morgen noch nicht veröffentlicht");
            }
        } finally {
            tomorrowCursor.close();
        }

//        if (TazSettings.getInstance(this).getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false)) {
//            if (autoloadSuccess) TazReaderApplication.registerAutoDownload(this, true);
//            else TazReaderApplication.registerAutoDownload(this, false);
//        }

        if (System.currentTimeMillis() >= TazSettings.getInstance(this)
                                                     .getSyncServiceNextRun()) {
            SyncHelper.setAlarmManager(this, tomorrowPaperExists);
        }


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

//    private void downloadImage(Paper paper) throws InterruptedException, ExecutionException, TimeoutException, IOException {
//        //Download Image
//        RequestFuture<Bitmap> imageFuture = RequestFuture.newFuture();
//        ImageRequest imageRequest = new ImageRequest(paper.getImage(), imageFuture, 0, 0, ImageView.ScaleType.CENTER_INSIDE, null,
//                                                     imageFuture);
//        RequestManager.getInstance(this)
//                      .add(imageRequest);
//        //        VolleySingleton.getInstance(this)
//        //                       .addToRequestQueue(imageRequest);
//        Bitmap bitmap = imageFuture.get(30, TimeUnit.SECONDS);
//        if (mCoverHelper.save(bitmap, paper.getImageHash())) {
//            if (paper.getId() != null) {
//                EventBus.getDefault()
//                        .post(new CoverDownloadedEvent(paper.getId()));
//            }
//        }
//
//
//    }

    /**
     * Handle plist.
     *
     * @param root the root
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException   the execution exception
     * @throws TimeoutException     the timeout exception
     * @throws IOException          the iO exception
     */
    public void handlePlist(NSDictionary root) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Publication publication = new Publication(root);

        Cursor pubCursor = this.getContentResolver()
                               .query(Publication.CONTENT_URI, null,
                                      Publication.Columns.ISSUENAME + " LIKE '" + publication.getIssueName() + "'", null, null);

        long publicationId;
        String publicationTitle = publication.getName();
        long validUntil = publication.getValidUntil();

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


        List<Paper> updatedPapers = new ArrayList<>();
        List<Paper> insertPapers = new ArrayList<>();

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
                                .query(bookIdUri, null, /*Paper.Columns.IMPORTED + "=0 AND " + Paper.Columns.KIOSK + "=0"*/ null,
                                       null, null);
            try {
                if (cursor.moveToNext()) {
                    Paper oldPaper = new Paper(cursor);
                    if (!comparePaper(newPaper, oldPaper)) {
                        log.debug("found difference in paper");
                        //Uri idUri = ContentUris.withAppendedId(Paper.CONTENT_URI, oldPaper.getId());
                        oldPaper.setImage(newPaper.getImage());
//                        if (!equalsHelper(newPaper.getImageHash(), oldPaper.getImageHash())) {
//                            //mCoverHelper.delete(oldPaper.getImageHash());
//
//                            //downloadImage(newPaper);
//                        }
                        oldPaper.setImageHash(newPaper.getImageHash());
                        if (!oldPaper.isImported() && !oldPaper.isKiosk()) {

                            if (!equalsHelper(oldPaper.getLastModified(), newPaper.getLastModified())) {
                                oldPaper.setLastModified(newPaper.getLastModified());
                                if (!equalsHelper(oldPaper.getFileHash(),
                                                  newPaper.getFileHash()) && (oldPaper.isDownloaded() || oldPaper.isDownloading()))
                                    oldPaper.setHasupdate(true);
                            }

                            oldPaper.setLink(newPaper.getLink());
                            oldPaper.setLen(newPaper.getLen());
                            oldPaper.setFileHash(newPaper.getFileHash());
                            oldPaper.setResource(newPaper.getResource());
                            oldPaper.setResourceFileHash(newPaper.getResourceFileHash());
                            oldPaper.setResourceUrl(newPaper.getResourceUrl());
                            oldPaper.setResourceLen(newPaper.getResourceLen());
                            oldPaper.setDemo(newPaper.isDemo());
                            oldPaper.setValidUntil(newPaper.getValidUntil());
                        }
                        if (oldPaper.getPublicationId() == null) {
                            oldPaper.setPublicationId(publicationId);
                        }
                        this.getContentResolver()
                            .update(oldPaper.getContentUri(), oldPaper.getContentValues(), null, null);
                        setMoveToPaperAtEnd(oldPaper);
                        preLoadImage(oldPaper);
                    } else {
                        //TODO check if image is in cache
                        Picasso.with(this)
                               .load(oldPaper.getImage())
                               .networkPolicy(NetworkPolicy.OFFLINE)
                               .fetch(new PreloadImageCallback(oldPaper) {
                                   @Override
                                   public void onSuccess(Paper paper) {
                                   }

                                   @Override
                                   public void onError(Paper paper) {
                                       preLoadImage(paper);
                                   }
                               });
                    }
                } else {
                    log.debug("notfound");

                    long newPaperId = ContentUris.parseId(this.getContentResolver()
                                                              .insert(Paper.CONTENT_URI, newPaper.getContentValues()));
                    newPaper.setId(newPaperId);
                    setMoveToPaperAtEnd(newPaper);
                    preLoadImage(newPaper);
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

    private abstract static class PreloadImageCallback implements Callback {

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
            log.error("", e);
            moveToPaperAtEnd = paper;
        }
    }

    private boolean comparePaper(Paper paper1, Paper paper2) {
        return equalsHelper(paper1.getDate(), paper2.getDate()) &&
                equalsHelper(paper1.getImage(), paper2.getImage()) &&
                equalsHelper(paper1.getImageHash(), paper2.getImageHash()) &&
                equalsHelper(paper1.getLink(), paper2.getLink()) &&
                equalsHelper(paper1.getFileHash(), paper2.getFileHash()) &&
                equalsHelper(paper1.getLen(), paper2.getLen()) &&
                equalsHelper(paper1.getResource(), paper2.getResource()) &&
                equalsHelper(paper1.getResourceFileHash(), paper2.getResourceFileHash()) &&
                equalsHelper(paper1.getResourceLen(), paper2.getResourceLen()) &&
                equalsHelper(paper1.getResourceUrl(), paper2.getResourceUrl()) &&
                equalsHelper(paper1.getLastModified(), paper2.getLastModified()) &&
                equalsHelper(paper1.getBookId(), paper2.getBookId()) &&
                equalsHelper(paper1.isDemo(), paper2.isDemo()) &&
                equalsHelper(paper1.getPublicationId(), paper2.getPublicationId()) &&
                equalsHelper(paper1.getValidUntil(), paper2.getValidUntil());

    }

    private boolean equalsHelper(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

}
