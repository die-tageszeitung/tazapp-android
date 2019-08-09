package de.thecode.android.tazreader.data;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;

import de.thecode.android.tazreader.utils.StorageManager;

public class FileCachePDFThumbHelper extends FileCacheImageHelper {

    private static final String SUB = "pdfthumbs";

    public FileCachePDFThumbHelper(StorageManager storage, String key) {
        super(storage, SUB + "/" + key);
    }

    @SuppressLint("NewApi")
    @Override
    public CompressFormat getBitmapCompressFormat() {
        return Bitmap.CompressFormat.JPEG;
    }

}
