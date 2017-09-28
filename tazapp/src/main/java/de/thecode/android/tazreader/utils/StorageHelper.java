package de.thecode.android.tazreader.utils;

import android.content.Context;

import de.thecode.android.tazreader.data.FileCachePDFThumbHelper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.secure.HashHelper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import timber.log.Timber;


public class StorageHelper {

    public static final  String TEMP     = "temp";
    public static final  String PAPER    = "paper";
    private static final String RESOURCE = "resource";

    private static final String DOWNLOAD = "download";
    private static final String IMPORT   = "import";


    public static File get(Context context, String type) {
        File result = context.getExternalFilesDir(type);
        if (result != null) //noinspection ResultOfMethodCallIgnored
            result.mkdirs();
        return result;
    }


    public static File getCache(Context context, String subDir) {
        File result = context.getExternalCacheDir();
        if (result != null) {
            if (subDir != null) result = new File(result, subDir);
            result.mkdirs();
        }
        return result;
    }

    private static File getDownloadCache(Context context) {
        return getCache(context, DOWNLOAD);
    }

    public static File getImportCache(Context context) {
        return getCache(context, IMPORT);
    }


    public static File getDownloadFile(Context context, Paper paper) {
        try {
            return getDownloadFile(context, HashHelper.getHash(paper.getBookId(), HashHelper.UTF_8, HashHelper.SHA_1));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Timber.e(e, "Error");
        }
        return null;
    }

    public static File getDownloadFile(Context context, Resource resource) {
        return getDownloadFile(context, resource.getKey());
    }

    private static File getDownloadFile(Context context, String key) {
        File downloadCache = getDownloadCache(context);
        if (downloadCache != null) return new File(getDownloadCache(context), key);
        return null;
    }

    public static File getPaperDirectory(Context context, Paper paper) {
        return new File(get(context, PAPER), paper.getBookId());
    }

    public static File getResourceDirectory(Context context, String key) {
        return new File(get(context, RESOURCE), key);
    }

    public static void deletePaperDir(Context context, Paper paper) {
        if (getPaperDirectory(context, paper).exists()) FileUtils.deleteQuietly(getPaperDirectory(context, paper));
//        Utils.deleteDir(getPaperDirectory(paper));
        new FileCachePDFThumbHelper(context, paper.getFileHash()).deleteDir();
    }

    public static void deleteResourceDir(Context context, String key) {
        File dir = getResourceDirectory(context, key);
        if (dir.exists()) FileUtils.deleteQuietly(getResourceDirectory(context, key));
        //Utils.deleteDir(getResourceDirectory(key));
    }


}
