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
    private final IPageIndexViewHolderClicks mRecyclerViewClickListener;

    public PageTocAdapter(IPageIndexViewHolderClicks clickListener) {
        super(new PageIndexDiffCallback());
        this.mRecyclerViewClickListener = clickListener;
    }


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
    }


    @SuppressWarnings("incomplete-switch")
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int itemType) {
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
        public boolean areItemsTheSame(@NonNull PageTocItem oldItem, @NonNull PageTocItem newItem) {
            return oldItem.getKey()
                          .equals(newItem.getKey());
        }

        @Override
        public boolean areContentsTheSame(@NonNull PageTocItem oldItem, @NonNull PageTocItem newItem) {
            return new EqualsBuilder().append(oldItem.hasOverlayBitmap(), newItem.hasOverlayBitmap())
                                      .build();
        }

        @Override
        public Object getChangePayload(@NonNull PageTocItem oldItem, @NonNull PageTocItem newItem) {
            return PAYLOAD_OVERLAY;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        IPageIndexViewHolderClicks clickListener;

        ViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView);
            this.clickListener = clickListener;
            itemView.setOnClickListener(v -> ViewHolder.this.clickListener.onItemClick(getAdapterPosition()));

        }
    }

    private static class SourceViewHolder extends ViewHolder {

        TextView text;

        SourceViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView, clickListener);
            text = itemView.findViewById(R.id.text);
        }
    }

    private static class PageViewHolder extends ViewHolder {

        ImageView image;
        ImageView articelOverlayImage;

        PageViewHolder(View itemView, IPageIndexViewHolderClicks clickListener) {
            super(itemView, clickListener);
            image = itemView.findViewById(R.id.image);
            articelOverlayImage = itemView.findViewById(R.id.articelOverlayImage);
        }
    }
}
