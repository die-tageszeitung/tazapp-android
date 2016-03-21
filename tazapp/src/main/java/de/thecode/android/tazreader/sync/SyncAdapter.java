package de.thecode.android.tazreader.sync;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import com.google.common.base.Strings;

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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.ParserConfigurationException;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.TazReaderApplication;
import de.thecode.android.tazreader.data.FileCacheCoverHelper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.CoverDownloadedEvent;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.start.ScrollToPaperEvent;
import de.thecode.android.tazreader.utils.Connection;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.volley.RequestManager;

/**
 * The type Sync adapter.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final Logger log = LoggerFactory.getLogger(SyncAdapter.class);

    private static final String PLIST_KEY_ISSUES = "issues";


    /**
     * The constant ARG_START_DATE.
     */
    public static final String ARG_START_DATE = "startDate";
    /**
     * The constant ARG_END_DATE.
     */
    public static final String ARG_END_DATE = "endDate";

    /**
     * The M content resolver.
     */
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;

    /**
     * The M cover helper.
     */
    FileCacheCoverHelper mCoverHelper;

    private Paper moveToPaperAtEnd;

    /**
     * Instantiates a new Sync adapter.
     *
     * @param context        the context
     * @param autoInitialize the auto initialize
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        init(context);
    }

    /**
     * Set up the sync adapter. This form of the constructor maintains compatibility with Android 3.0 and later platform versions
     *
     * @param context            the context
     * @param autoInitialize     the auto initialize
     * @param allowParallelSyncs the allow parallel syncs
     */
    @SuppressLint("NewApi")
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        init(context);
    }

    private void init(Context context) {
        //Log.init(context, TAZReader.LOGTAG);
        log.trace("");

        mContentResolver = context.getContentResolver();
        mCoverHelper = new FileCacheCoverHelper(StorageManager.getInstance(context));
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        //Crashlytics.start(mContext);
        log.debug("account: {}, extras: {}, authority: {}, provider: {}, syncResult: {}", account, extras, authority, provider, syncResult);
        EventBus.getDefault()
                .postSticky(new SyncStateChangedEvent(true));

        //If migration is needed dont sync
        if (TazSettings.getPrefInt(getContext(), TazSettings.PREFKEY.PAPERMIGRATEFROM, 0) != 0) return;
        //if (TazSettings.getPrefBoolean(getContext(), TazSettings.PREFKEY.PAPERMIGRATERUNNING, false)) return;

        String url = getContext().getString(R.string.plist);

        if (extras != null) {
            if (extras.containsKey(ARG_START_DATE) && extras.containsKey(ARG_END_DATE)) {
                String startDate = extras.getString(ARG_START_DATE);
                String endDate = extras.getString(ARG_END_DATE);
                url = String.format(getContext().getString(R.string.plistArchiv), startDate, endDate);
            }
        }


        Long currentOpenPaperId = TazSettings.getPrefLong(getContext(), TazSettings.PREFKEY.LASTOPENPAPER, -1L);
        log.debug("+++++++ TazSettings: Current Paper SyncAdapter View: {}", currentOpenPaperId);

        // If ForceSync it's now done
        TazSettings.setPref(getContext(), TazSettings.PREFKEY.FORCESYNC, false);

        // AutoDelete
        if (TazSettings.getPrefBoolean(getContext(), TazSettings.PREFKEY.AUTODELETE, false)) {

            int papersToKeep = TazSettings.getPrefInt(getContext(), TazSettings.PREFKEY.AUTODELETE_VALUE, 0);
            if (papersToKeep > 0) {
                Cursor deletePapersCursor = getContext().getContentResolver()
                                                        .query(Paper.CONTENT_URI, null, Paper.Columns.ISDOWNLOADED + "=1 AND " + Paper.Columns.IMPORTED + "!=1 AND " + Paper.Columns.KIOSK + "!=1", null, Paper.Columns.DATE + " DESC");
                try {
                    int counter = 0;
                    while (deletePapersCursor.moveToNext()) {
                        if (counter >= papersToKeep) {
                            Paper deletePaper = new Paper(deletePapersCursor);
                            log.debug("PaperId: {} (currentOpen:{})", deletePaper.getId(), currentOpenPaperId);
                            if (!deletePaper.getId()
                                            .equals(currentOpenPaperId)) {
                                boolean safeToDelete = true;
                                String bookmarksJsonString = deletePaper.getStoreValue(getContext(), ReaderActivity.STORE_KEY_BOOKMARKS);
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
                                    deletePaper.delete(getContext());
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
                RequestManager.getInstance()
                              .doRequest()
                              .add(request);
                //                Volley.newRequestQueue(getContext())
                //                      .add(request);
                //VolleySingleton.getInstance(getContext()).addToRequestQueue(request);
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
            Crashlytics.getInstance().core.setString("SyncResponse", response);
            Crashlytics.getInstance().core.logException(syncExeption);
        }

        // Missing cover images for imported
        Cursor importedPaperCursor = getContext().getContentResolver()
                                                 .query(Paper.CONTENT_URI, null, Paper.TABLE_NAME + "." + Paper.Columns.IMAGE + " IS NULL", null, null);
        try {
            while (importedPaperCursor.moveToNext()) {
                Paper importedPaper = new Paper(importedPaperCursor);

                if (Strings.isNullOrEmpty(importedPaper.getImage())) {
                    try {
                        String importedPaperPlistUrl = Uri.parse(getContext().getString(R.string.plist))
                                                          .buildUpon()
                                                          .appendQueryParameter("start", importedPaper.getDate())
                                                          .appendQueryParameter("end", importedPaper.getDate())
                                                          .build()
                                                          .toString();
                        RequestFuture<String> future = RequestFuture.newFuture();
                        StringRequest request = new StringRequest(Request.Method.GET, importedPaperPlistUrl, future, future);
                        request.setShouldCache(false);
                        //                        Volley.newRequestQueue(getContext())
                        //                              .add(request);
                        RequestManager.getInstance()
                                      .doRequest()
                                      .add(request);
                        String imageResponse = future.get(30, TimeUnit.SECONDS);
                        NSDictionary root = (NSDictionary) PropertyListParser.parse(new ByteArrayInputStream(imageResponse.getBytes("UTF-8")));
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
        Boolean autoloadSuccess = false;

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_YEAR, 1); // <--
        Date tomorrow = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Cursor tomorrowCursor = getContext().getContentResolver()
                                            .query(Paper.CONTENT_URI, null, Paper.Columns.DATE + " LIKE '" + sdf.format(tomorrow) + "'", null, null);
        try {
            if (tomorrowCursor.moveToNext()) {
                // Ausgabe von morgen ist da
                log.debug("Ausgabe von morgen veröffentlicht");
                Paper tomorrowPaper = new Paper(tomorrowCursor);
                if (TazSettings.getPrefBoolean(getContext(), TazSettings.PREFKEY.AUTOLOAD, false)) {
                    if (!tomorrowPaper.isDownloaded()) {
                        boolean connectionCheck = false;
                        if (TazSettings.getPrefBoolean(getContext(), TazSettings.PREFKEY.AUTOLOAD_WIFI, true)) {
                            if (Connection.getConnectionType(getContext()) >= Connection.CONNECTION_FAST) connectionCheck = true;
                        } else {
                            if (Connection.getConnectionType(getContext()) >= Connection.CONNECTION_MOBILE_ROAMING) connectionCheck = true;
                        }
                        if (connectionCheck) {

                            AccountHelper accountHelper = new AccountHelper(getContext(), account);
                            if (accountHelper.isAuthenticated()) {
                                //DownloadHelper downloadHelper = new DownloadHelper(getContext());
                                try {
                                    DownloadManager.getInstance(getContext())
                                                   .enquePaper(tomorrowPaper.getId());
                                    autoloadSuccess = true;
                                } catch (IllegalArgumentException | DownloadManager.DownloadNotAllowedException | Paper.PaperNotFoundException | AccountHelper.CreateAccountException ignored) {
                                }
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

        if (TazSettings.getPrefBoolean(getContext(), TazSettings.PREFKEY.AUTOLOAD, false)) {
            if (autoloadSuccess) TazReaderApplication.registerAutoDownload(getContext(), true);
            else TazReaderApplication.registerAutoDownload(getContext(), false);
        }


        EventBus.getDefault()
                .postSticky(new SyncStateChangedEvent(false));

        if (moveToPaperAtEnd != null) {
            EventBus.getDefault()
                    .post(new ScrollToPaperEvent(moveToPaperAtEnd.getId()));
            moveToPaperAtEnd = null;
        }
    }

    private void downloadImage(Paper paper) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        //Download Image
        RequestFuture<Bitmap> imageFuture = RequestFuture.newFuture();
        ImageRequest imageRequest = new ImageRequest(paper.getImage(), imageFuture, 0, 0, null, imageFuture);
        RequestManager.getInstance()
                      .doRequest()
                      .add(imageRequest);
        //        VolleySingleton.getInstance(getContext())
        //                       .addToRequestQueue(imageRequest);
        Bitmap bitmap = imageFuture.get(30, TimeUnit.SECONDS);
        if (mCoverHelper.save(bitmap, paper.getImageHash())) {
            if (paper.getId() != null) {
                EventBus.getDefault()
                        .post(new CoverDownloadedEvent(paper.getId()));
            }
        }


    }

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

        Cursor pubCursor = getContext().getContentResolver()
                                       .query(Publication.CONTENT_URI, null, Publication.Columns.ISSUENAME + " LIKE '" + publication.getIssueName() + "'", null, null);

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
                getContext().getContentResolver()
                            .update(updateUri, oldPupdata.getContentValues(), null, null);
            } else {
                Uri newPublicationUri = getContext().getContentResolver()
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
            Cursor cursor = getContext().getContentResolver()
                                        .query(bookIdUri, null, /*Paper.Columns.IMPORTED + "=0 AND " + Paper.Columns.KIOSK + "=0"*/ null, null, null);
            try {
                if (cursor.moveToNext()) {
                    Paper oldPaper = new Paper(cursor);
                    if (!comparePaper(newPaper, oldPaper)) {
                        log.debug("found difference in paper");
                        //Uri idUri = ContentUris.withAppendedId(Paper.CONTENT_URI, oldPaper.getId());
                        oldPaper.setImage(newPaper.getImage());
                        if (!equalsHelper(newPaper.getImageHash(), oldPaper.getImageHash())) {
                            mCoverHelper.delete(oldPaper.getImageHash());

                            //downloadImage(newPaper);
                        }
                        oldPaper.setImageHash(newPaper.getImageHash());
                        if (!oldPaper.isImported() && !oldPaper.isKiosk()) {

                            if (!equalsHelper(oldPaper.getLastModified(), newPaper.getLastModified())) {
                                oldPaper.setLastModified(newPaper.getLastModified());
                                if (!equalsHelper(oldPaper.getFileHash(), newPaper.getFileHash()) && (oldPaper.isDownloaded() || oldPaper.isDownloading()))
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
                        updatedPapers.add(oldPaper);
                        //                    getContext().getContentResolver()
                        //                            .update(idUri, oldPaper.getContentValues(), null, null);
                    }
                    //if (!mCoverHelper.exists(oldPaper.getImageHash())) downloadImage(oldPaper);
                } else {
                    log.debug("notfound");

                    //                Uri newPaperUri = getContext().getContentResolver()
                    //                                          .insert(Paper.CONTENT_URI, newPaper.getContentValues());
                    //                long newPaperId = ContentUris.parseId(newPaperUri);
                    //downloadImage(newPaper);
                    insertPapers.add(newPaper);

                }
            } finally {
                cursor.close();
            }


        }
        for (Paper insertPaper : insertPapers) {
            long newPaperId = ContentUris.parseId(getContext().getContentResolver()
                                                              .insert(Paper.CONTENT_URI, insertPaper.getContentValues()));
            insertPaper.setId(newPaperId);
            setMoveToPaperAtEnd(insertPaper);
        }

        for (Paper updatedPaper : updatedPapers) {
            getContext().getContentResolver()
                        .update(updatedPaper.getContentUri(), updatedPaper.getContentValues(), null, null);
            setMoveToPaperAtEnd(updatedPaper);
        }

        List<Paper> imagePapers = new ArrayList<>(insertPapers);
        imagePapers.addAll(updatedPapers);
        for (Paper imagePaper : imagePapers) {
            if (!mCoverHelper.exists(imagePaper.getImageHash())) downloadImage(imagePaper);
        }


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
