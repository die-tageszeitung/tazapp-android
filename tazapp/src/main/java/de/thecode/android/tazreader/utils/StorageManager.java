package de.thecode.android.tazreader.utils;

import android.content.Context;
import android.content.ContextWrapper;

import de.thecode.android.tazreader.data.FileCachePDFThumbHelper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.secure.HashHelper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import timber.log.Timber;


public class StorageManager extends ContextWrapper {

    public static final String TEMP     = "temp";
    public static final String PAPER    = "paper";
    public static final String RESOURCE = "resource";

    private static final String DOWNLOAD = "download";
    private static final String IMPORT   = "import";
    private static final String APPUPDATE   = "appUpdate";

    private static StorageManager instance;

    public static StorageManager getInstance(Context context) {
        if (instance == null) instance = new StorageManager(context.getApplicationContext());
        return instance;
    }

    private StorageManager(Context context) {
        super(context);
        createNoMediaFileInDir(getCache(null));
        createNoMediaFileInDir(get(null));
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

    public File get(String type) {
        File result = getExternalFilesDir(type);
        if (result != null) //noinspection ResultOfMethodCallIgnored
            result.mkdirs();
        return result;
    }


    public File getCache(String subDir) {
        File result = getExternalCacheDir();
        if (result != null) {
            if (subDir != null) result = new File(result, subDir);
            result.mkdirs();

        }
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

    private File getDownloadFile(String key) {
        File downloadCache = getDownloadCache();
        if (downloadCache != null) return new File(getDownloadCache(), key);
        return null;
    }

    public File getPaperDirectory(Paper paper) {
        return new File(get(PAPER), paper.getBookId());
    }

    public File getResourceDirectory(String key) {
        return new File(get(RESOURCE), key);
    }

    public File getResourceDirectory(Resource resource) {
        if (resource == null) return null;
        return getResourceDirectory(resource.getKey());
    }


    public void deletePaperDir(Paper paper) {
        if (getPaperDirectory(paper).exists()) FileUtils.deleteContentsAndDir(getPaperDirectory(paper));
//        Utils.deleteDir(getPaperDirectory(paper));
        new FileCachePDFThumbHelper(this, paper.getFileHash()).deleteDir();
    }

    public void deleteResourceDir(String key) {
        File dir = getResourceDirectory(key);
        if (dir.exists()) FileUtils.deleteContentsAndDir(getResourceDirectory(key));
        //Utils.deleteDir(getResourceDirectory(key));
    }


}
