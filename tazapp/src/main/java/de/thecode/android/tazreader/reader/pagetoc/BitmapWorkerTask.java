package de.thecode.android.tazreader.reader.pagetoc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.FileCachePDFThumbHelper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.reader.page.TAZMuPDFCore;
import de.thecode.android.tazreader.utils.StorageManager;

import java.io.File;
import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Created by mate on 09.03.18.
 */

public class BitmapWorkerTask extends AsyncTask<Void, Void, Bitmap> {

    private static LruCache<String, Bitmap> memoryCache;

    static {
        final int maxMemory = (int) (Runtime.getRuntime()
                                            .maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {

            @SuppressLint("NewApi")
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private final WeakReference<ImageView> imageViewReference;
    private final String                   key;

    private final int                     mThumbnailImageHeight;
    private final int                     mThumbnailImageWidth;
    private final FileCachePDFThumbHelper pdfThumbHelper;
    private final File                    paperDirectory;

    private static Bitmap placeHolderBitmap = null;

    BitmapWorkerTask(ImageView imageView, Paper.Plist.Page page) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        this.key = page.getKey();
        imageViewReference = new WeakReference<>(imageView);
        Context context = imageView.getContext();
        mThumbnailImageHeight = context.getResources()
                                       .getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_height) - (2 * context.getResources()
                                                                                                                      .getDimensionPixelSize(
                                                                                                                              R.dimen.pageindex_padding));
        mThumbnailImageWidth = context.getResources()
                                      .getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_width) - (2 * context.getResources()
                                                                                                                    .getDimensionPixelSize(
                                                                                                                            R.dimen.pageindex_padding));
        StorageManager storageManager = StorageManager.getInstance(context);
        this.paperDirectory = storageManager.getPaperDirectory(page.getPaper());
        this.pdfThumbHelper = new FileCachePDFThumbHelper(storageManager,
                                                          page.getPaper()
                                                              .fileHash);
        if (placeHolderBitmap == null) {
            placeHolderBitmap = Bitmap.createBitmap(mThumbnailImageWidth, mThumbnailImageHeight, Bitmap.Config.ARGB_8888);
            placeHolderBitmap.eraseColor(imageView.getResources()
                                                  .getColor(R.color.pageindex_loadingpage_bitmapbackground));
        }
    }

    Bitmap getPlaceHolderBitmap() {
        return placeHolderBitmap;
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Void... params) {
        Bitmap bitmap = getBitmapFromCache();
        if (bitmap == null) {
            bitmap = getBitmapFromDiskCache();
            if (bitmap == null) bitmap = getBitmapFromPDF();
            if (bitmap != null) addBitmapToCache(bitmap);
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }

        if (bitmap != null && imageViewReference != null) {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    private Bitmap getBitmapFromCache() {
        Bitmap bitmap = memoryCache.get(key);
        if (bitmap == null) Timber.d("did not find key: %s in memcache", key);
        else Timber.d("found key: %s in memcache", key);
        return bitmap;
    }

    private void addBitmapToCache(Bitmap bitmap) {
        memoryCache.put(key, bitmap);
    }

    private Bitmap getBitmapFromDiskCache() {
        File imageFile = pdfThumbHelper.getFile(key);
        if (imageFile.exists()) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, mThumbnailImageWidth, mThumbnailImageHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        }
        return null;
    }

    private Bitmap getBitmapFromPDF() {
        try {
            Bitmap lq = Bitmap.createBitmap(mThumbnailImageWidth, mThumbnailImageHeight, Bitmap.Config.ARGB_8888);
            if (imageViewReference.get() != null) {


                TAZMuPDFCore core = new TAZMuPDFCore(imageViewReference.get()
                                                                       .getContext(),
                                                     new File(paperDirectory, key).getAbsolutePath());
                core.countPages();
                core.setPageSize(core.getPageSize(0));

                core.drawPage(lq,
                              0,
                              mThumbnailImageWidth,
                              mThumbnailImageHeight,
                              0,
                              0,
                              mThumbnailImageWidth,
                              mThumbnailImageHeight,
                              core.new Cookie());

                pdfThumbHelper.save(lq, key);
                core.onDestroy();
                return lq;
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }


    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        Timber.d("reqWidth: %s, reqHeight: %s, %s %s", reqWidth, reqHeight, width, height);
        return inSampleSize;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    static boolean cancelPotentialWork(String key, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.key;
            // If bitmapData is not yet set or it differs from the new data
            if (!key.equals(bitmapData)) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

}
