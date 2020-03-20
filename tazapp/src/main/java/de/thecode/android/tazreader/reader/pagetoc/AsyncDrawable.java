package de.thecode.android.tazreader.reader.pagetoc;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.lang.ref.WeakReference;

/**
 * Created by mate on 09.03.18.
 */
class AsyncDrawable extends BitmapDrawable {

    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

    AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
        super(res, bitmap);
        bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
    }

    BitmapWorkerTask getBitmapWorkerTask() {
        return bitmapWorkerTaskReference.get();
    }
}
