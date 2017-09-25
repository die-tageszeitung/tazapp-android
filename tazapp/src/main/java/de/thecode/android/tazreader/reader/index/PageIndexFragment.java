package de.thecode.android.tazreader.reader.index;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.artifex.mupdfdemo.MuPDFCore.Cookie;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.FileCachePDFThumbHelper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Paper.Plist.Book;
import de.thecode.android.tazreader.data.Paper.Plist.Category;
import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.data.Paper.Plist.Page.Article;
import de.thecode.android.tazreader.data.Paper.Plist.Page.Geometry;
import de.thecode.android.tazreader.data.Paper.Plist.Source;
import de.thecode.android.tazreader.data.Paper.Plist.TopLink;
import de.thecode.android.tazreader.reader.IReaderCallback;
import de.thecode.android.tazreader.reader.page.TAZMuPDFCore;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.StorageManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PageIndexFragment extends BaseFragment {

    List<IIndexItem>         index;
    Paper                    paper;
    PageIndexRecylerAdapter  adapter;
    LruCache<String, Bitmap> mMemoryCache;

    RecyclerView mRecyclerView;

    int mThumbnailImageHeight;
    int mThumbnailImageWidth;

    Bitmap mPlaceHolderBitmap;

    Bitmap mCurrentArticleOverlay;

    String mCurrentKey;

    IPageIndexViewHolderClicks mRecyclerViewClickListener;

    FileCachePDFThumbHelper mPdfThumbHelper;

    IReaderCallback mReaderCallback;


    public PageIndexFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //setRetainInstance(true);
        final int maxMemory = (int) (Runtime.getRuntime()
                                            .maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            @SuppressLint("NewApi")
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        init(mReaderCallback.getPaper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.reader_pageindex, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        // mRecyclerView.addItemDecoration(new DividerItemDecoration(getResources()
        // .getDrawable(android.R.drawable.divider_horizontal_dim_dark)));

        mRecyclerViewClickListener = new IPageIndexViewHolderClicks() {

            @Override
            public void onItemClick(int position) {
                PageIndexFragment.this.onItemClick(position);
            }
        };

        mRecyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IReaderCallback) mReaderCallback = (IReaderCallback)context;
        else throw new RuntimeException(context.toString() + " must implement " + IReaderCallback.class.getSimpleName());


        mThumbnailImageHeight = context.getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.pageindex_thumbnail_image_height) - (2 * context.getResources()
                                                                                                         .getDimensionPixelSize(
                                                                                                                 R.dimen.pageindex_padding));
        mThumbnailImageWidth = context.getResources()
                                       .getDimensionPixelSize(
                                               R.dimen.pageindex_thumbnail_image_width) - (2 * context.getResources()
                                                                                                       .getDimensionPixelSize(
                                                                                                               R.dimen.pageindex_padding));

        mPlaceHolderBitmap = Bitmap.createBitmap(mThumbnailImageWidth, mThumbnailImageHeight, Bitmap.Config.ARGB_8888);
        mPlaceHolderBitmap.eraseColor(getResources().getColor(R.color.pageindex_loadingpage_bitmapbackground));
    }


    private void init(Paper paper) {
        Timber.d("initialising PageIndexFragment with paper: %s", paper);
        index = new ArrayList<>();
        for (Source source : paper.getPlist()
                                  .getSources()) {
            index.add(source);
            for (Book book : source.getBooks()) {
                for (Category category : book.getCategories()) {
                    index.addAll(category.getPages());
                }
            }
        }
        this.paper = paper;

        adapter = new PageIndexRecylerAdapter();
    }

    public void updateCurrentPosition(String key) {
        Timber.d("key: %s", key);

        IIndexItem indexItem = paper.getPlist()
                                    .getIndexItem(key);
        Page page = null;
        if (indexItem != null) {
            switch (indexItem.getType()) {
                case PAGE:
                    page = (Page) indexItem;
                    break;
                case ARTICLE:
                    page = ((Article) indexItem).getRealPage();
                    break;
                case TOPLINK:
                    page = ((TopLink) indexItem).getPage();
                    break;
            }
            if (page != null) {
                mCurrentKey = page.getKey();

                float x1 = 0;
                float x2 = 1F;
                float y1 = 0;
                float y2 = 1F;

                for (Geometry geometry : page.getGeometries()) {


                    if (geometry.getLink()
                                .equals(key)) {
                        if (x1 == 0 || geometry.getX1() < x1) x1 = geometry.getX1();
                        if (y1 == 0 || geometry.getY1() < y1) y1 = geometry.getY1();
                        if (x2 == 1F || geometry.getX2() > x2) x2 = geometry.getX2();
                        if (y2 == 1F || geometry.getY2() > y2) y2 = geometry.getY2();
                    }
                }
                makeOverlayBitmap(x1, y1, x2, y2);

                int where = adapter.getPosition(page);
                if (where != -1 && mRecyclerView != null) {
                    mRecyclerView.scrollToPosition(where);
                    adapter.notifyDataSetChanged();
                    mRecyclerView.invalidate();
                }
                return;
            }
        }
        mCurrentKey = null;
        adapter.notifyDataSetChanged();


    }

    private void makeOverlayBitmap(float x1, float y1, float x2, float y2) {
        try {
            Timber.d("x1: %s, y1: %s, x2: %s, y2: %s, mThumbnailImageWidth: %s, mThumbnailImageHeight: %s", x1, y1, x2, y2,
                      mThumbnailImageWidth, mThumbnailImageHeight);
            mCurrentArticleOverlay = Bitmap.createBitmap(mThumbnailImageWidth, mThumbnailImageHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mCurrentArticleOverlay);
            Paint paint = new Paint();
            int padding = getResources().getDimensionPixelSize(R.dimen.pageindex_thumbnail_current_borderwidth);
            float halfPadding = ((float) padding) / 2;
            paint.setColor(ContextCompat.getColor(getContext(),R.color.pageindex_overlay_color));
            paint.setAlpha(128);
            paint.setStrokeWidth(padding);
            paint.setStyle(Paint.Style.STROKE);

            float dx1 = (x1 * mThumbnailImageWidth) - halfPadding;
            if (dx1 < halfPadding) dx1 = halfPadding;

            float dy1 = (y1 * mThumbnailImageHeight) - halfPadding;
            if (dy1 < halfPadding) dy1 = halfPadding;

            float dx2 = (x2 * mThumbnailImageWidth) + halfPadding;
            if (dx2 > mThumbnailImageWidth - halfPadding) dx2 = mThumbnailImageWidth - halfPadding;

            float dy2 = (y2 * mThumbnailImageHeight) + halfPadding;
            if (dy2 > mThumbnailImageHeight - halfPadding) dy2 = mThumbnailImageHeight - halfPadding;

            //Log.d(dx1, dy1, dx2, dy2);

            canvas.drawRect(dx1, dy1, dx2, dy2, paint);
        } catch (IllegalStateException e) {

        }
    }


    private static boolean cancelPotentialWork(String key, ImageView imageView) {
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

    private void loadBitmap(String key, ImageView imageView) {
        if (cancelPotentialWork(key, imageView)) {

            final Bitmap bitmap = getBitmapFromMemCache(key);

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(getActivity().getResources(), mPlaceHolderBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(key);
            }
        }
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

    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;
        String key;
        Cookie cookie;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            key = params[0];
            return getBitmap(key, mThumbnailImageWidth, mThumbnailImageHeight);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                    addBitmapToMemoryCache(key, bitmap);
                }
            }
        }

        public Bitmap getBitmap(String key, int width, int height) {
            if (mPdfThumbHelper == null) {
                mPdfThumbHelper = new FileCachePDFThumbHelper(StorageManager.getInstance(getActivity()), paper.getFileHash());
            }
            File imageFile = mPdfThumbHelper.getFile(key);
            Timber.d("imagefile %s", imageFile.getName());
            if (imageFile.exists()) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, width, height);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            } else {
                try {
                    Bitmap lq = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                    File paperDirectory = StorageManager.getInstance(getActivity())
                                                        .getPaperDirectory(paper);
                    TAZMuPDFCore core = new TAZMuPDFCore(getActivity(), new File(paperDirectory, key).getAbsolutePath());
                    core.countPages();
                    core.setPageSize(core.getPageSize(0));
                    cookie = core.new Cookie();

                    core.drawPage(lq, 0, width, height, 0, 0, width, height, cookie);

                    mPdfThumbHelper.save(lq, key);
                    core.onDestroy();

                    return lq;

                } catch (Exception e) {
                    Timber.e(e);
                }

            }
            return null;
        }
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

    private static class AsyncDrawable extends BitmapDrawable {

        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        Bitmap bitmap = mMemoryCache.get(key);
        if (bitmap == null) Timber.d("did not find key: %s in memcache", key);
        else Timber.d("found key: %s in memcache", key);
        return bitmap;
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public void onItemClick(int position) {
        Timber.d("position: %s", position);
        IIndexItem item = adapter.getItem(position);
        mReaderCallback.onLoad(item.getKey());
    }

    private class PageIndexRecylerAdapter extends RecyclerView.Adapter<Viewholder> {

        @Override
        public int getItemViewType(int position) {
            return index.get(position)
                        .getType()
                        .ordinal();
        }

        @Override
        public int getItemCount() {
            if (index != null) return index.size();
            return 0;
        }

        public IIndexItem getItem(int position) {
            // Log.v();
            if (index != null) return index.get(position);
            return null;
        }

        public int getPosition(String key) {
            int result = -1;


            for (IIndexItem indexItem : index) {
                if (indexItem.getKey()
                             .equals(key)) {
                    result = index.indexOf(indexItem);
                    break;
                }
            }
            Timber.d("key: %s %s", key, result);
            return result;
        }

        public int getPosition(IIndexItem item) {
            return index.indexOf(item);
        }

        @SuppressWarnings("incomplete-switch")
        @Override
        public void onBindViewHolder(Viewholder viewholder, int position) {
            IIndexItem item = index.get(position);
            switch (IIndexItem.Type.values()[viewholder.getItemViewType()]) {
                case SOURCE:
                    ((SourceViewholder) viewholder).text.setText(item.getTitle());
                    break;
                case PAGE:
                    loadBitmap(((Page) item).getKey(), ((PageViewholder) viewholder).image);
                    if (item.getKey()
                            .equals(mCurrentKey)) {
                        //((PageViewholder) viewholder).image.setBackgroundColor(getResources().getColor(R.color.pageindex_current_border));
                        ((PageViewholder) viewholder).articelOverlayImage.setVisibility(View.VISIBLE);
                        if (mCurrentArticleOverlay != null)
                            ((PageViewholder) viewholder).articelOverlayImage.setImageBitmap(mCurrentArticleOverlay);
                        else ((PageViewholder) viewholder).articelOverlayImage.setVisibility(View.GONE);
                    } else {
                        //((PageViewholder) viewholder).image.setBackgroundColor(Color.TRANSPARENT);
                        ((PageViewholder) viewholder).articelOverlayImage.setVisibility(View.GONE);
                    }
                    break;
            }
        }

        @SuppressWarnings("incomplete-switch")
        @Override
        public Viewholder onCreateViewHolder(ViewGroup parent, int itemType) {
            View v;
            switch (IIndexItem.Type.values()[itemType]) {
                case SOURCE:
                    v = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.reader_pageindex_source, parent, false);
                    return new SourceViewholder(v);

                case PAGE:
                    v = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.reader_pageindex_page, parent, false);
                    return new PageViewholder(v);
            }
            return null;
        }

    }

    private class Viewholder extends RecyclerView.ViewHolder implements View.OnClickListener {


        public Viewholder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {

            if (mRecyclerViewClickListener != null) mRecyclerViewClickListener.onItemClick(getPosition());
        }


    }

    private class SourceViewholder extends Viewholder {

        TextView text;

        public SourceViewholder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.text);
        }
    }

    private class PageViewholder extends Viewholder {

        ImageView image;
        ImageView articelOverlayImage;

        public PageViewholder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);
            articelOverlayImage = (ImageView) itemView.findViewById(R.id.articelOverlayImage);
        }

        @Override
        public void onClick(View v) {
            mReaderCallback.closeDrawers();
            super.onClick(v);
        }
    }

    public interface IPageIndexViewHolderClicks {
        public void onItemClick(int position);
    }


}
