package de.thecode.android.tazreader.reader.pagetoc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.reader.ReaderBaseFragment;
import de.thecode.android.tazreader.utils.TazListAdapter;
import de.thecode.android.tazreader.widget.SnapLayoutManager;

import java.util.List;

import timber.log.Timber;

public class PageTocFragment extends ReaderBaseFragment {

    NewPageIndexRecyclerAdapter adapter;

    RecyclerView mRecyclerView;

    int mThumbnailImageHeight;
    int mThumbnailImageWidth;

    Bitmap mPlaceHolderBitmap;

    Bitmap mCurrentArticleOverlay;

    public PageTocFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new NewPageIndexRecyclerAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_pageindex, container, false);
        mRecyclerView = view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        SnapLayoutManager layoutManager = new SnapLayoutManager(inflater.getContext());
        layoutManager.setSnapPreference(SnapLayoutManager.SNAP_TO_CENTER);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mThumbnailImageHeight = view.getContext()
                                    .getResources()
                                    .getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_height) - (2 * view.getContext()
                                                                                                                .getResources()
                                                                                                                .getDimensionPixelSize(
                                                                                                                        R.dimen.pageindex_padding));
        mThumbnailImageWidth = view.getContext()
                                   .getResources()
                                   .getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_width) - (2 * view.getContext()
                                                                                                              .getResources()
                                                                                                              .getDimensionPixelSize(
                                                                                                                      R.dimen.pageindex_padding));

        mPlaceHolderBitmap = Bitmap.createBitmap(mThumbnailImageWidth, mThumbnailImageHeight, Bitmap.Config.ARGB_8888);
        mPlaceHolderBitmap.eraseColor(getResources().getColor(R.color.pageindex_loadingpage_bitmapbackground));

        getReaderViewModel().getPagesTocLiveData()
                            .observe(this, iTocItems -> {
                                adapter.submitList(iTocItems);
                            });

        getReaderViewModel().getCurrentKeyLiveData()
                            .observe(this, iTocItem -> {
                                if (iTocItem != null) {
                                    adapter.markAsCurrent(iTocItem);
                                }
                            });
    }


    private void makeOverlayBitmap(float x1, float y1, float x2, float y2) {
        try {
            Timber.d("x1: %s, y1: %s, x2: %s, y2: %s, mThumbnailImageWidth: %s, mThumbnailImageHeight: %s",
                     x1,
                     y1,
                     x2,
                     y2,
                     mThumbnailImageWidth,
                     mThumbnailImageHeight);
            mCurrentArticleOverlay = Bitmap.createBitmap(mThumbnailImageWidth, mThumbnailImageHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mCurrentArticleOverlay);
            Paint paint = new Paint();
            int padding = getResources().getDimensionPixelSize(R.dimen.pageindex_thumbnail_current_borderwidth);
            float halfPadding = ((float) padding) / 2;
            paint.setColor(ContextCompat.getColor(getContext(), R.color.pageindex_overlay_color));
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

    private void loadBitmap(String key, ImageView imageView) {
        if (BitmapWorkerTask.cancelPotentialWork(key, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView, getReaderViewModel().getPaper(), key);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(imageView.getResources(), mPlaceHolderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute();
        }
    }

    public void onItemClick(int position) {
        Timber.d("position: %s", position);
        ITocItem item = adapter.getItem(position);
        getReaderActivity().loadContentFragment(item.getKey());
        getReaderActivity().closeDrawers();
    }

    private class NewPageIndexRecyclerAdapter extends TazListAdapter<ITocItem, ViewHolder> {

        public static final short PAYLOAD_OVERLAY = 1;
        private ITocItem currentItem;

        IPageIndexViewHolderClicks mRecyclerViewClickListener = new IPageIndexViewHolderClicks() {
            @Override
            public void onItemClick(int position) {
                PageTocFragment.this.onItemClick(position);
            }
        };

        public NewPageIndexRecyclerAdapter() {
            super(new PageIndexDiffCallback());
        }


        public void markAsCurrent(ITocItem tocItem) {
            Page page = null;
            if (tocItem != null) {
                if (tocItem instanceof Page) page = (Page) tocItem;
                else if (tocItem instanceof Page.Article) page = ((Page.Article) tocItem).getRealPage();
                else if (tocItem instanceof Paper.Plist.TopLink) page = ((Paper.Plist.TopLink) tocItem).getPage();
                if (page != null) {
                    Handler handler = new Handler();
                    Page finalPage = page;
                    new Thread(() -> {
                        float x1 = 0;
                        float x2 = 1F;
                        float y1 = 0;
                        float y2 = 1F;
                        for (Page.Geometry geometry : finalPage.getGeometries()) {
                            if (geometry.getLink()
                                        .equals(tocItem.getKey())) {
                                if (x1 == 0 || geometry.getX1() < x1) x1 = geometry.getX1();
                                if (y1 == 0 || geometry.getY1() < y1) y1 = geometry.getY1();
                                if (x2 == 1F || geometry.getX2() > x2) x2 = geometry.getX2();
                                if (y2 == 1F || geometry.getY2() > y2) y2 = geometry.getY2();
                            }
                        }
                        makeOverlayBitmap(x1, y1, x2, y2);
                        handler.post(() -> {
                            ITocItem oldCurrentItem = currentItem;
                            currentItem = finalPage;
                            int oldPos = indexOf(oldCurrentItem);
                            int pos = indexOf(currentItem);

                            if (oldPos != -1) {
                                notifyItemChanged(oldPos, PAYLOAD_OVERLAY);
                            }
                            if (pos != -1) notifyItemChanged(pos, PAYLOAD_OVERLAY);
                            mRecyclerView.smoothScrollToPosition(pos);

                        });
                    }).start();

                    //mRecyclerView.center(position);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).getType()
                                    .ordinal();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
            ITocItem item = getItem(position);
            switch (ITocItem.Type.values()[holder.getItemViewType()]) {
                case SOURCE:
                    onBindSourceViewHolder((SourceViewHolder) holder, (Paper.Plist.Source) item, position);
                    break;
                case PAGE:
                    onBindPageViewHolder((PageViewHolder) holder, (Page) item, position, payloads);
                    break;
            }
        }

        private void onBindSourceViewHolder(SourceViewHolder viewholder, Paper.Plist.Source source, int position) {
            viewholder.text.setText(source.getTitle());
        }

        private void onBindPageViewHolder(PageViewHolder viewholder, Page page, int position, List<Object> payloads) {
            if (payloads.isEmpty()) {
                loadBitmap(page.getKey(), viewholder.image);
            }
            if (payloads.contains(PAYLOAD_OVERLAY) || payloads.isEmpty()) {
                if (page.equals(currentItem) && mCurrentArticleOverlay != null) {
                    viewholder.articelOverlayImage.setVisibility(View.VISIBLE);
                    viewholder.articelOverlayImage.setImageBitmap(mCurrentArticleOverlay);
                } else {
                    viewholder.articelOverlayImage.setVisibility(View.GONE);
                }

            }
        }

        @SuppressWarnings("incomplete-switch")
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int itemType) {
            View v;
            switch (ITocItem.Type.values()[itemType]) {
                case SOURCE:
                    v = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.reader_pageindex_source, parent, false);
                    return new SourceViewHolder(v, mRecyclerViewClickListener);

                case PAGE:
                    v = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.reader_pageindex_page, parent, false);
                    return new PageViewHolder(v, mRecyclerViewClickListener);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            //notUsed
        }
    }

    public static class PageIndexDiffCallback extends DiffUtil.ItemCallback<ITocItem> {

        @Override
        public boolean areItemsTheSame(ITocItem oldItem, ITocItem newItem) {
            return true;
        }

        @Override
        public boolean areContentsTheSame(ITocItem oldItem, ITocItem newItem) {
            return true;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        IPageIndexViewHolderClicks clickListener;

        public ViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView);
            this.clickListener = clickListener;
            itemView.setOnClickListener(v -> ViewHolder.this.clickListener.onItemClick(getAdapterPosition()));

        }
    }

    private class SourceViewHolder extends ViewHolder {

        TextView text;

        public SourceViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView, clickListener);
            text = (TextView) itemView.findViewById(R.id.text);
        }
    }

    private class PageViewHolder extends ViewHolder {

        ImageView image;
        ImageView articelOverlayImage;

        public PageViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView, clickListener);
            image = (ImageView) itemView.findViewById(R.id.image);
            articelOverlayImage = (ImageView) itemView.findViewById(R.id.articelOverlayImage);
        }
    }

    public interface IPageIndexViewHolderClicks {
        public void onItemClick(int position);
    }


}
