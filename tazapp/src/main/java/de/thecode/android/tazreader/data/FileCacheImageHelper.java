package de.thecode.android.tazreader.data;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;

import de.thecode.android.tazreader.utils.ExtensionsKt;
import de.thecode.android.tazreader.utils.StorageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import timber.log.Timber;

public abstract class FileCacheImageHelper {

    private static final int quality = 80;

    private File mCacheDir;


    FileCacheImageHelper(StorageManager storage, String subDir) {
        mCacheDir = storage.getCache(subDir);
    }

    public boolean save(Bitmap bitmap, String hash) throws IOException {
        return save(bitmap, hash, 0, 0);
    }

    boolean save(Bitmap bitmap, String hash, int width, int height) throws IOException {
        Timber.d("bitmap: %s, hash: %s, width: %d, height: %d", bitmap, hash, width, height);
        if (mCacheDir.exists()) {
            File imageFile = new File(mCacheDir, hash + "." + getFileEndingForBitmapCompressFormat());
            if (imageFile.exists())
                //noinspection ResultOfMethodCallIgnored
                imageFile.delete();
            if (imageFile.createNewFile()) {
                OutputStream fOut = null;
                fOut = new FileOutputStream(imageFile);

                if (height != 0 || width != 0) {
                    if (width != 0 && height == 0) height = bitmap.getHeight() * width / bitmap.getWidth();
                    if (height != 0 && width == 0) width = bitmap.getWidth() * height / bitmap.getHeight();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) return Bitmap.CompressFormat.WEBP;
        else return Bitmap.CompressFormat.JPEG;
    }

    @SuppressLint("NewApi")
    private String getFileEndingForBitmapCompressFormat() {
        return getBitmapCompressFormat().name().toLowerCase(Locale.getDefault());
    }

    public boolean delete(String hash) {
        File image = getFile(hash);
        if (image != null && image.exists()) return image.delete();
        return false;
    }

    public void deleteDir() {
        if (mCacheDir != null) {
            if (mCacheDir.exists()) ExtensionsKt.deleteQuietly(mCacheDir);
        }
    }

    public boolean exists(String hash) {
        return getFile(hash).exists();
    }

    public File getFile(String hash) {
        File[] files = mCacheDir.listFiles(new ImageNameFileFilter(hash));
        if (files != null && files.length > 0 && files[0] != null) {
            if (files.length > 1) {
                for (int i = 1; i < files.length; i++) {
                    files[i].delete();
                }
            }
            return files[0];
        }
        return new File(mCacheDir, hash + "." + getFileEndingForBitmapCompressFormat());
    }


    private class ImageNameFileFilter implements FilenameFilter {

        private String hash;

        ImageNameFileFilter(String hash) {
            this.hash = hash;
        }

        @Override
        public boolean accept(File dir, String filename) {
            String[] split = filename.split("\\.");
            return split.length > 0 && split[0].equals(hash);
        }
    }

}
