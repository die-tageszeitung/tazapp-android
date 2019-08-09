package de.thecode.android.tazreader.utils;

import android.content.Context;
import android.text.TextUtils;

import de.thecode.android.tazreader.data.FileCachePDFThumbHelper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.secure.HashHelper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import androidx.annotation.Nullable;
import kotlin.io.FilesKt;
import timber.log.Timber;


public class StorageManager {

    public static final String PAPER    = "paper";
    public static final String RESOURCE = "resource";

    private static final String DOWNLOAD  = "download";
    private static final String IMPORT    = "import";
    private static final String APPUPDATE = "appUpdate";
    private static final String LOG       = "logs";

    private static StorageManager instance;

    public static StorageManager getInstance(Context context) {
        if (instance == null) instance = new StorageManager(context.getApplicationContext());
        return instance;
    }

    private File dataFolder;
    private File cacheFolder;

    private StorageManager(Context context) {

        TazSettings settings = TazSettings.getInstance(context);
        settings.<String>addOnPreferenceChangeListener(
                TazSettings.PREFKEY.DATA_FOLDER, changedValue  -> {
                    dataFolder = new File(changedValue);
                    createNoMediaFileInDir(dataFolder);
                });
        String dataFolderPath = settings.getDataFolderPath();
        if (TextUtils.isEmpty(dataFolderPath)) {
            File externalFilesDir = context.getExternalFilesDir(null);
            if (externalFilesDir != null) {
                settings.setDataFolderPath(externalFilesDir.getAbsolutePath());
            }
        } else {
            dataFolder = new File(dataFolderPath);
            createNoMediaFileInDir(dataFolder);
        }

        cacheFolder = context.getExternalCacheDir();
        createNoMediaFileInDir(cacheFolder);

        if (!settings.isWriteLogfile()) ExtensionsKt.deleteQuietly(getLogCache());
    }

    private void createNoMediaFileInDir(File dir) {
        File noMediaFile = new File(dir, ".nomedia");
        if (!noMediaFile.exists()) {
            try {
                if (!noMediaFile.createNewFile()) Timber.w("cannot create file: %s", noMediaFile.getAbsolutePath());
            } catch (IOException e) {
                Timber.w(e);
            }
        }
    }

    public File get(@Nullable String type) {
        if (type == null) return dataFolder;
        File result = new File(dataFolder, type);
        //noinspection ResultOfMethodCallIgnored
        result.mkdirs();
        return result;
    }


    public File getCache(@Nullable String subDir) {
        if (subDir == null) return cacheFolder;
        File result = new File(cacheFolder, subDir);
        //noinspection ResultOfMethodCallIgnored
        result.mkdirs();
        return result;
    }

    public File getDownloadCache() {
        return getCache(DOWNLOAD);
    }

    public File getImportCache() {
        return getCache(IMPORT);
    }

    public File getUpdateAppCache() {
        return getCache(APPUPDATE);
    }

    public File getLogCache() {
        return getCache(LOG);
    }


    public File getDownloadFile(Paper paper) {
        try {
            return getDownloadFile(HashHelper.getHash(paper.getBookId(), HashHelper.UTF_8, HashHelper.SHA_1) + ".paper.zip");
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Timber.e(e, "Error");
        }
        return null;
    }

    public File getDownloadFile(Resource resource) {
        return getDownloadFile(resource.getKey() + ".res.zip");
    }

    public File getDownloadFile(String key) {
        File downloadCache = getDownloadCache();
        if (downloadCache != null) return new File(getDownloadCache(), key);
        return null;
    }

    public File getPaperDirectory(Paper paper) {
        return new File(get(PAPER), paper.getBookId());
    }

    public File getPaperDirectory(String bookId) {
        return new File(get(PAPER), bookId);
    }


    public File getResourceDirectory(String key) {
        return new File(get(RESOURCE), key);
    }

    public File getResourceDirectory(Resource resource) {
        if (resource == null) return null;
        return getResourceDirectory(resource.getKey());
    }


    public void deletePaperDir(Paper paper) {
        if (getPaperDirectory(paper).exists()) ExtensionsKt.deleteQuietly(getPaperDirectory(paper));
        new FileCachePDFThumbHelper(this, paper.fileHash).deleteDir();
    }

    public void deleteResourceDir(String key) {
        File dir = getResourceDirectory(key);
        if (dir.exists()) ExtensionsKt.deleteQuietly(dir);
        FilesKt.deleteRecursively(dir);
    }

}
