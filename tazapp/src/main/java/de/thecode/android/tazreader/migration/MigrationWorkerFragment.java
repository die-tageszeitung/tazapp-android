package de.thecode.android.tazreader.migration;

import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.StorageManager;

/**
 * Created by mate on 21.04.2015.
 */
public class MigrationWorkerFragment extends BaseFragment {

    private static final Logger log = LoggerFactory.getLogger(MigrationWorkerFragment.class);

    private MigrationWorkerCallback callback;
    Context applicationContext;

    public MigrationWorkerFragment() {

    }


    public static MigrationWorkerFragment findOrCreateWorkerFragment(FragmentManager fm, String tag, MigrationWorkerCallback callback) {
        MigrationWorkerFragment fragment = (MigrationWorkerFragment) fm.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = new MigrationWorkerFragment();
            fragment.callback = callback;

            fm.beginTransaction()
              .add(fragment, tag)
              .commit();
        } else fragment.callback = callback;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.debug("");
        setRetainInstance(true);
        applicationContext = getActivity().getApplicationContext();
        checkForMigration();

        //if (callback!=null) callback.onWorkerFragmentCreate();
    }


    public void checkForMigration() {
        int migrateFrom = TazSettings.getPrefInt(applicationContext, TazSettings.PREFKEY.PAPERMIGRATEFROM, Integer.parseInt(String.valueOf(BuildConfig.VERSION_CODE)
                                                                                                                                  .substring(1)));
        log.debug("{}", migrateFrom);

        if (migrateFrom < 32) {
            migrateTo32();
        }
        //
        //
        //
        //        while (migrateFrom < Log.getVersionCode())
        //        {
        //            if (migrateFrom < 32) {
        //                migrateTo32();
        //                migrateFrom = 32;
        //
        //            }
        ////            else if (migrateFrom < ??){              //Copy to migrate
        ////              migrateTo??();
        ////              migrateFrom = ??;
        ////            }
        //            else {
        //                migrateFrom++;
        //            }
        //        }
        //
        //        TazSettings.removePref(applicationContext, TazSettings.PREFKEY.PAPERMIGRATEFROM);
        //        if (callback != null) callback.onMigrationFinished();
    }

    public void migrateTo32() {

        log.debug("");
        new AsyncTaskWithExecption<Void, Void, List<File>>() {

            @Override
            protected void onPreExecute() {
                if (callback != null) callback.onMigrationStart(32);
            }

            @Override
            public List<File> doInBackgroundWithException(Void... params) throws Exception {
                List<File> filesToImport = new ArrayList<>();
                log.info("Migrating to version 32 ...");

                StorageManager storage = StorageManager.getInstance(applicationContext);

                log.info("Deleting old temp dir");
                File oldTemp = storage.get("temp");
                if (oldTemp.exists()) FileUtils.deleteQuietly(oldTemp);//Utils.deleteDir(oldTemp);
               log.info("...finished");

                log.info("Deleting Cache");
                if (storage.getCache(null)
                           .exists()) FileUtils.deleteQuietly(storage.getCache(null));//Utils.deleteDirContent(storage.getCache(null));
                log.info("...finished");

                List<Store> storeList = Store.getAllStores(applicationContext);
                for (Store store : storeList) {
                    log.info("Migrating Store {}", store.getKey());
                    if (store.getKey()
                             .endsWith("/" + ReaderActivity.STORE_KEY_BOOKMARKS)) {
                        log.info("Migrating Bookmarks");
                        try {
                            JSONArray newBookmarksArray = new JSONArray();
                            JSONObject bookmarksJson = new JSONObject(store.getValue());
                            JSONArray bookMarkNames = bookmarksJson.names();
                            for (int i = 0; i < bookMarkNames.length(); i++) {
                                String bookmarkName = bookMarkNames.getString(i);
                                if (bookmarksJson.getBoolean(bookmarkName)) {
                                    bookmarkName = bookmarkName.replace(".xhtml", ".html");
                                    newBookmarksArray.put(bookmarkName);
                                }
                            }
                            Store.saveValueForKey(applicationContext, store.getKey(), newBookmarksArray.toString());
                            log.info("...finished");
                        } catch (JSONException e) {
                            log.error("Error during migration, bookmarks will be deleted, sorry");
                            Store.deleteKey(applicationContext, store.getKey());
                        }

                    } else {
                        log.info("...deleting");
                        Store.deleteKey(applicationContext, store.getKey());
                    }
                }


                FileUtils.deleteQuietly(storage.getCache(null));//Utils.deleteDirContent(storage.getCache(null));

                File externalFilesDir = applicationContext.getExternalFilesDir(null);
                File externalStorageDir = null;
                if (externalFilesDir != null) externalStorageDir = externalFilesDir.getParentFile();
                File paperDir = new File(externalStorageDir, "paper");
                Cursor cursor = applicationContext.getContentResolver()
                                                  .query(Paper.CONTENT_URI, null, null, null, null);
                try {
                    while (cursor.moveToNext()) {
                        Paper paper = new Paper(cursor);
                        String filename = cursor.getString(cursor.getColumnIndex("filename"));
                        try {
                            File paperFile = new File(paperDir, filename);
                            if (!paper.isDownloaded() || !paperFile.exists()) throw new IllegalStateException();

                            File outputFile;
                            do {
                                outputFile = new File(storage.getImportCache(), String.valueOf(System.currentTimeMillis()));
                            } while (outputFile.exists());
                            FileInputStream is = new FileInputStream(paperFile);
                            OutputStream os = new FileOutputStream(outputFile);
                            final int buffer_size = 1024;
                            byte[] bytes = new byte[buffer_size];
                            for (; ; ) {
                                int count = is.read(bytes, 0, buffer_size);
                                if (count == -1) break;
                                os.write(bytes, 0, count);
                            }
                            is.close();
                            os.close();
                            filesToImport.add(outputFile);
                            //noinspection ResultOfMethodCallIgnored
                            paperFile.delete();


                        } catch (NullPointerException | IllegalStateException e) {
                            applicationContext.getContentResolver()
                                              .delete(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), null, null);
                        }


                    }
                } finally {
                    cursor.close();
                }

                FileUtils.deleteQuietly(paperDir);//Utils.deleteDir(paperDir);
                log.info("... migration finished!");
                while (callback == null) {
                    //wait for callback before returning
                }
                return filesToImport;
            }

            @Override
            protected void onPostError(Exception exception) {
                log.error("",exception);
                if (callback != null) callback.onMigrationError(exception);
            }

            @Override
            protected void onPostSuccess(List<File> filesToImport) {
                log.debug("");
                TazSettings.setPref(applicationContext, TazSettings.PREFKEY.PAPERMIGRATEFROM, 32);
                if (callback != null) callback.onMigrationFinished(32, filesToImport);
            }
        }.execute();


    }


    public interface MigrationWorkerCallback {

        public void onMigrationStart(int toVersionNumber);

        public void onMigrationFinished(int toVersionNumber, List<File> importFiles);

        public void onMigrationError(Exception e);
    }
}
