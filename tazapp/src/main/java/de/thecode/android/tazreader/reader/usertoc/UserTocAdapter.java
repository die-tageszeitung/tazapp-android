package de.thecode.android.tazreader.reader.usertoc;

import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.TazListAdapter;
import de.thecode.android.tazreader.utils.TintHelper;

import org.apache.commons.lang3.builder.EqualsBuilder;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by mate on 16.03.18.
 */

public class UserTocAdapter extends TazListAdapter<UserTocItem, UserTocAdapter.ViewHolder> {

    UserTocAdapter(UserTocAdapterClickListener clickListener) {
        super(new UserTocItemCallback());
        this.clickListener = clickListener;
    }

    //TODO
    private boolean showSubtitles = false;

    private UserTocAdapterClickListener clickListener;

    void setShowSubtitles(boolean showSubtitles) {
        this.showSubtitles = showSubtitles;
        if (getItemCount() > 0) notifyDataSetChanged();
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewholder, int position) {
        UserTocItem tocItem = getItem(position);
        ITocItem item = tocItem.getIndexItem();

        if (viewholder.mCurrentMarker != null) {
            viewholder.mCurrentMarker.setVisibility(tocItem.isActive() ? View.VISIBLE : View.INVISIBLE);
        }

        switch (ITocItem.Type.values()[viewholder.getItemViewType()]) {
            case CATEGORY:
                CategoryViewHolder categoryViewHolder = (CategoryViewHolder) viewholder;
                categoryViewHolder.title.setText(item.getTitle());
                ImageView image = categoryViewHolder.image;
                if (tocItem.areChildsVisible())
                    image.setImageDrawable(ContextCompat.getDrawable(image.getContext(), R.drawable.ic_remove_24dp));
                else image.setImageDrawable(ContextCompat.getDrawable(image.getContext(), R.drawable.ic_add_24dp));
                break;
            case ARTICLE:
                onBindArticleViewHolder((ArticleViewHolder) viewholder, (Paper.Plist.Page.Article) item);
                break;
            case TOPLINK:
                ((ToplinkViewHolder) viewholder).title.setText(item.getTitle());
                break;
        }
    }

    private void onBindArticleViewHolder(ArticleViewHolder viewHolder, Paper.Plist.Page.Article item) {

        if (TextUtils.isEmpty(item.getTitle())) {
            viewHolder.title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_emoticon_dead_24dp,0,0,0);
            viewHolder.title.setText("");
        } else {
            viewHolder.title.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
            viewHolder.title.setText(item.getTitle());
        }


        if (TextUtils.isEmpty(item.getSubtitle()) || !showSubtitles)
            viewHolder.subtitle.setVisibility(View.GONE);
        else {
            viewHolder.subtitle.setVisibility(View.VISIBLE);
            viewHolder.subtitle.setText(item.getSubtitle());
        }

        if (TextUtils.isEmpty(item.getAuthor())) {
            viewHolder.author.setVisibility(View.GONE);
        } else {
            viewHolder.author.setVisibility(View.VISIBLE);
            viewHolder.author.setText(item.getAuthor());
        }


        ImageView bookmark = viewHolder.bookmark;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) bookmark.getLayoutParams();
        if (item.isBookmarked()) {
            TintHelper.tintDrawable(bookmark.getDrawable(),
                                    ContextCompat.getColor(bookmark.getContext(), R.color.index_bookmark_on));
            bookmark.setAlpha(1F);
            layoutParams.topMargin = bookmark.getResources()
                                             .getDimensionPixelOffset(R.dimen.reader_bookmark_offset_active);
        } else {

            TypedValue outValue = new TypedValue();
            bookmark.getResources()
                    .getValue(R.dimen.icon_button_alpha, outValue, true);
            float iconButtonAlpha = outValue.getFloat();

            TintHelper.tintDrawable(bookmark.getDrawable(),
                                    ContextCompat.getColor(bookmark.getContext(), R.color.index_bookmark_off));
            bookmark.setAlpha(iconButtonAlpha);
            layoutParams.topMargin = bookmark.getResources()
                                             .getDimensionPixelOffset(R.dimen.reader_bookmark_offset_normal);
        }
        bookmark.setLayoutParams(layoutParams);
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int itemType) {
        switch (ITocItem.Type.values()[itemType]) {
            case CATEGORY:
                return new CategoryViewHolder(LayoutInflater.from(parent.getContext())
                                                            .inflate(R.layout.reader_index_category, parent, false),
                                              clickListener);
            case ARTICLE:
                return new ArticleViewHolder(LayoutInflater.from(parent.getContext())
                                                           .inflate(R.layout.reader_index_article, parent, false), clickListener);
            case TOPLINK:
                return new ToplinkViewHolder(LayoutInflater.from(parent.getContext())
                                                           .inflate(R.layout.reader_index_toplink, parent, false), clickListener);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getIndexItem()
                                .getType()
                                .ordinal();
    }

    private static class UserTocItemCallback extends DiffUtil.ItemCallback<UserTocItem> {

        @Override
        public boolean areItemsTheSame(@NonNull UserTocItem oldItem, @NonNull UserTocItem newItem) {
            return oldItem.getKey()
                          .equals(newItem.getKey());
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserTocItem oldItem, @NonNull UserTocItem newItem) {
            return new EqualsBuilder().append(oldItem.isActive(), newItem.isActive())
                                      .append(oldItem.areChildsVisible(), newItem.areChildsVisible())
                                      .append(oldItem.isBookmarked(), newItem.isBookmarked())
                                      .build();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View                        mCurrentMarker;
        UserTocAdapterClickListener listener;

        ViewHolder(View itemView, UserTocAdapterClickListener clickListener) {
            super(itemView);
            this.listener = clickListener;
            mCurrentMarker = itemView.findViewById(R.id.currentMarker);
            itemView.setOnClickListener(v -> listener.onItemClick(getAdapterPosition()));
        }
    }

    private static class CategoryViewHolder extends ViewHolder {

        CategoryViewHolder(View itemView, UserTocAdapterClickListener clickListener) {
            super(itemView, clickListener);
            title = itemView.findViewById(R.id.title);
            image = itemView.findViewById(R.id.image);

        }

        ImageView image;
        TextView  title;
    }

    private static class ArticleViewHolder extends ViewHolder {

        ImageView bookmark;
        TextView  title;
        TextView  subtitle;
        TextView  author;
        View      bookmarkLayout;

        ArticleViewHolder(View itemView, UserTocAdapterClickListener clickListener) {
            super(itemView, clickListener);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            author = itemView.findViewById(R.id.author);
            bookmark = itemView.findViewById(R.id.bookmark);
            bookmarkLayout = itemView.findViewById(R.id.bookmarkClickLayout);
            bookmarkLayout.setOnClickListener(v -> listener.onBookmarkClick(getAdapterPosition()));
        }
    }

    private static class ToplinkViewHolder extends ViewHolder {

        TextView title;

        ToplinkViewHolder(View itemView, UserTocAdapterClickListener clickListener) {
            super(itemView, clickListener);
            title = itemView.findViewById(R.id.title);
        }
    }

    public interface UserTocAdapterClickListener {
        void onItemClick(int adapterPosition);

        void onBookmarkClick(int adapterPosition);
    }
}
