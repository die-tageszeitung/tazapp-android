package de.thecode.android.tazreader.data;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import de.thecode.android.tazreader.utils.Utils;
import de.thecode.android.tazreader.utils.StorageManager;

public abstract class FileCacheImageHelper {
    
    private static final Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
    private static final int quality = 80;

    private File mCacheDir ;
    
    
    public FileCacheImageHelper(StorageManager storage,String subDir) {
        //mContext = context;
        //ExternalStorage storage = new ExternalStorage(context);
        mCacheDir = storage.getCache(subDir);
    }

    public boolean save(Bitmap bitmap, String hash) throws IOException
    {
        return save(bitmap, hash, 0, 0);
    }

    public boolean save(Bitmap bitmap, String hash, int width, int height) throws IOException
    {
        if (mCacheDir.exists())
        {
            File imageFile = new File(mCacheDir, hash + "." + getFileEndingForBitmapCompressFormat());
            if (imageFile.exists())
                //noinspection ResultOfMethodCallIgnored
                imageFile.delete();
            if (imageFile.createNewFile())
            {
                OutputStream fOut = null;
                fOut = new FileOutputStream(imageFile);

                if (height != 0 || width != 0)
                {
                    if (width != 0 && height == 0)
                        height = bitmap.getHeight() * width / bitmap.getWidth();
                    if (height != 0 && width == 0)
                        width = bitmap.getWidth() * height / bitmap.getHeight();
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                }
                bitmap.compress(getBitmapCompressFormat(), quality, fOut);
                fOut.flush();
                fOut.close();
                return true;
            }
        }
        return false;
    }
    
    public Bitmap.CompressFormat getBitmapCompressFormat() {
        return format;
    }
    
    @SuppressLint("NewApi")
    public String getFileEndingForBitmapCompressFormat() {
        switch (getBitmapCompressFormat()) {
            case JPEG:
                return Bitmap.CompressFormat.JPEG.name().toLowerCase(Locale.getDefault());
            case PNG:
                return Bitmap.CompressFormat.PNG.name().toLowerCase(Locale.getDefault());
            case WEBP:
                return Bitmap.CompressFormat.WEBP.name().toLowerCase(Locale.getDefault());
        }
        return null;
    }
    
    public int getBitmapCompressQuality() {
        return quality;
    }

    public boolean delete(String hash)
    {
        File image = new File(mCacheDir, hash + "." + getFileEndingForBitmapCompressFormat());
        if (image.exists())
            return image.delete();
        return false;
    }
    
    public boolean deleteDir() {
        if (mCacheDir != null)
        {
            if (mCacheDir.exists())
                return Utils.deleteDir(mCacheDir);
        }
        return false;
    }

    public boolean exists(String hash)
    {
        return getFile(hash).exists();
    }

    public File getFile(String hash)
    {
        return new File(mCacheDir, hash + "." + getFileEndingForBitmapCompressFormat());
    }

}
