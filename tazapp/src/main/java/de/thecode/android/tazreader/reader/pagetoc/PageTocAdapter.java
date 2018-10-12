package de.thecode.android.tazreader.reader.pagetoc;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.TazListAdapter;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.List;

/**
 * Created by mate on 22.03.18.
 */
public class PageTocAdapter extends TazListAdapter<PageTocItem, PageTocAdapter.ViewHolder> {

    public static final short PAYLOAD_OVERLAY = 1;
    //        private ITocItem currentItem;
    private final IPageIndexViewHolderClicks mRecyclerViewClickListener;

    public PageTocAdapter(IPageIndexViewHolderClicks clickListener) {
        super(new PageIndexDiffCallback());
        this.mRecyclerViewClickListener = clickListener;
    }


//        public void markAsCurrent(ITocItem tocItem) {
//            Page page = null;
//            if (tocItem != null) {
//                if (tocItem instanceof Page) page = (Page) tocItem;
//                else if (tocItem instanceof Page.Article) page = ((Page.Article) tocItem).getRealPage();
//                else if (tocItem instanceof Paper.Plist.TopLink) page = ((Paper.Plist.TopLink) tocItem).getPage();
//                if (page != null) {
//                    Handler handler = new Handler();
//                    Page finalPage = page;
//                    new Thread(() -> {
//                        float x1 = 0;
//                        float x2 = 1F;
//                        float y1 = 0;
//                        float y2 = 1F;
//                        for (Page.Geometry geometry : finalPage.getGeometries()) {
//                            if (geometry.getLink()
//                                        .equals(tocItem.getKey())) {
//                                if (x1 == 0 || geometry.getX1() < x1) x1 = geometry.getX1();
//                                if (y1 == 0 || geometry.getY1() < y1) y1 = geometry.getY1();
//                                if (x2 == 1F || geometry.getX2() > x2) x2 = geometry.getX2();
//                                if (y2 == 1F || geometry.getY2() > y2) y2 = geometry.getY2();
//                            }
//                        }
//                        makeOverlayBitmap(x1, y1, x2, y2);
//                        handler.post(() -> {
//                            ITocItem oldCurrentItem = currentItem;
//                            currentItem = finalPage;
//                            int oldPos = indexOf(oldCurrentItem);
//                            int pos = indexOf(currentItem);
//
//                            if (oldPos != -1) {
//                                notifyItemChanged(oldPos, PAYLOAD_OVERLAY);
//                            }
//                            if (pos != -1) notifyItemChanged(pos, PAYLOAD_OVERLAY);
//                            mRecyclerView.smoothScrollToPosition(pos);
//
//                        });
//                    }).start();
//
//                    //mRecyclerView.center(position);
//                }
//            }
//        }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getIndexItem()
                                .getType()
                                .ordinal();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        PageTocItem item = getItem(position);
        switch (ITocItem.Type.values()[holder.getItemViewType()]) {
            case SOURCE:
                onBindSourceViewHolder((SourceViewHolder) holder, item, position);
                break;
            case PAGE:
                onBindPageViewHolder((PageViewHolder) holder, item, position, payloads);
                break;
        }
    }

    private void onBindSourceViewHolder(SourceViewHolder viewholder, PageTocItem item, int position) {
        viewholder.text.setText(item.getIndexItem()
                                    .getTitle());
    }

    private void onBindPageViewHolder(PageViewHolder viewholder, PageTocItem item, int position, List<Object> payloads) {
//        Timber.d("binding: %s hasBitmap:%b",item.getKey(),item.hasOverlayBitmap());
        if (payloads.isEmpty()) {
            loadBitmap((Paper.Plist.Page) item.getIndexItem(), viewholder.image);
        }
        if (payloads.contains(PAYLOAD_OVERLAY) || payloads.isEmpty()) {
            if (item.hasOverlayBitmap()) {
                viewholder.articelOverlayImage.setVisibility(View.VISIBLE);
                viewholder.articelOverlayImage.setImageBitmap(item.getOverlayBitmap());
            } else {
                viewholder.articelOverlayImage.setVisibility(View.GONE);
            }
        }

//                if (page.equals(currentItem) && mCurrentArticleOverlay != null) {
//                    viewholder.articelOverlayImage.setVisibility(View.VISIBLE);
//                    viewholder.articelOverlayImage.setImageBitmap(mCurrentArticleOverlay);
//                } else {
//                    viewholder.articelOverlayImage.setVisibility(View.GONE);
//                }
//
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


    private void loadBitmap(Paper.Plist.Page page, ImageView imageView) {
        if (BitmapWorkerTask.cancelPotentialWork(page.getKey(), imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView, page);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(imageView.getResources(), task.getPlaceHolderBitmap(), task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute();
        }
    }


    public interface IPageIndexViewHolderClicks {
        void onItemClick(int position);
    }

    public static class PageIndexDiffCallback extends DiffUtil.ItemCallback<PageTocItem> {

        @Override
        public boolean areItemsTheSame(PageTocItem oldItem, PageTocItem newItem) {
            return oldItem.getKey()
                          .equals(newItem.getKey());
        }

        @Override
        public boolean areContentsTheSame(PageTocItem oldItem, PageTocItem newItem) {
            return new EqualsBuilder().append(oldItem.hasOverlayBitmap(), newItem.hasOverlayBitmap())
                                      .build();
        }

        @Override
        public Object getChangePayload(PageTocItem oldItem, PageTocItem newItem) {
            return PAYLOAD_OVERLAY;
        }
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        IPageIndexViewHolderClicks clickListener;

        public ViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView);
            this.clickListener = clickListener;
            itemView.setOnClickListener(v -> ViewHolder.this.clickListener.onItemClick(getAdapterPosition()));

        }
    }

    private static class SourceViewHolder extends ViewHolder {

        TextView text;

        public SourceViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView, clickListener);
            text = itemView.findViewById(R.id.text);
        }
    }

    private static class PageViewHolder extends ViewHolder {

        ImageView image;
        ImageView articelOverlayImage;

        public PageViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView, clickListener);
            image = itemView.findViewById(R.id.image);
            articelOverlayImage = itemView.findViewById(R.id.articelOverlayImage);
        }
    }
}
