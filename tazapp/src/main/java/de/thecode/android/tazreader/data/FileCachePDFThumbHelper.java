package de.thecode.android.tazreader.data;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;

import de.thecode.android.tazreader.utils.StorageManager;

public class FileCachePDFThumbHelper extends FileCacheImageHelper {

    public static final String SUB = "pdfthumbs";

    public FileCachePDFThumbHelper(StorageManager storage, String key) {
        super(storage, SUB + "/" + key);
    }

    @SuppressLint("NewApi")
    @Override
    public CompressFormat getBitmapCompressFormat() {
        if (Build.VERSION.SDK_INT >= 14)
            return Bitmap.CompressFormat.WEBP;
        else
            return Bitmap.CompressFormat.JPEG;
    }

    @Override
    public int getBitmapCompressQuality() {
        return 60;
    }

}
