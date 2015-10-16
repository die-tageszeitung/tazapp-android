package de.thecode.android.tazreader.utils;

import android.content.Context;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import de.thecode.android.tazreader.data.FileCacheCoverHelper;
import de.thecode.android.tazreader.data.FileCachePDFThumbHelper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.secure.HashHelper;


public class StorageManager {
    
    public static final String TEMP = "temp";
    public static final String PAPER = "paper";
    public static final String RESOURCE = "resource";
    
    private static final String DOWNLOAD = "download";
    private static final String IMPORT = "import";
    
    private static StorageManager instance;

    public static StorageManager getInstance(Context context) {
        if (instance == null)
            instance = new StorageManager(context.getApplicationContext());
        return instance;
    }

    private Context mContext;

    private StorageManager(Context context) {
        mContext = context;
    }
    
    public File get(String type)
    {
        File result= mContext.getExternalFilesDir(type);
        if (result != null) //noinspection ResultOfMethodCallIgnored
            result.mkdirs();
        return result;
    }


    public File getCache(String subDir)
    {
        File result= mContext.getExternalCacheDir();
        if (subDir != null)
            result = new File(result,subDir);
        if (result != null) //noinspection ResultOfMethodCallIgnored
            result.mkdirs();
        return result;        
    }
    
    public File getDownloadCache() {
        return getCache(DOWNLOAD);
    }

    public File getImportCache() {return getCache(IMPORT);}


    public File getDownloadFile(Paper paper) {
            try {
                return getDownloadFile(HashHelper.getHash(paper.getBookId(),HashHelper.UTF_8,HashHelper.SHA_1));
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                Log.e(e);
            }
        return null;
    }
    public File getDownloadFile(Resource resource) {
        return getDownloadFile(resource.getKey());
    }

    private File getDownloadFile(String key) {
        return new File(getDownloadCache(),key);
    }

    public File getPaperDirectory(Paper paper)
    {
        return new File(get(PAPER),paper.getBookId());
    }

    public File getResourceDirectory(String key) {return new File(get(RESOURCE),key);}

    public void deletePaperDir(Paper paper) {
        if (getPaperDirectory(paper).exists())
            FileUtils.deleteQuietly(getPaperDirectory(paper));
//        Utils.deleteDir(getPaperDirectory(paper));
        new FileCachePDFThumbHelper(this, paper.getFileHash()).deleteDir();
        if (paper.isImported() || paper.isKiosk()) {
            new FileCacheCoverHelper(this).delete(paper.getImageHash());
        }
    }

    public void deleteResourceDir(String key)  {
        File dir = getResourceDirectory(key);
        if (dir.exists())
            FileUtils.deleteQuietly(getResourceDirectory(key));
        //Utils.deleteDir(getResourceDirectory(key));
    }



}
