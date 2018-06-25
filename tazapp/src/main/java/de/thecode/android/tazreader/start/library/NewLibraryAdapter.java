package de.thecode.android.tazreader.start.library;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.sync.PreloadImageCallback;
import de.thecode.android.tazreader.utils.TazListAdapter;
import de.thecode.android.tazreader.utils.extendedasyncdiffer.ExtendedAdapterListUpdateCallback;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;

import timber.log.Timber;

public class NewLibraryAdapter extends TazListAdapter<LibraryPaper, NewLibraryAdapter.ViewHolder> {

    private static final String PAYLOAD_PROGRESS = "plProgress";
    private static final String PAYLOAD_SELECTED = "plSelected";
    private static final String PAYLOAD_STATE    = "plState";
    private static final String PAYLOAD_CHANGE = "plChange";

    private final ViewHolder.OnClickListener clickListener = new ViewHolder.OnClickListener() {
        @Override
        public void onClick(int position) {
            if (itemClickListener != null) itemClickListener.onClick(getItem(position), position);
        }

        @Override
        public void onLongClick(int position) {
            if (itemClickListener != null) itemClickListener.onLongClick(getItem(position), position);
        }
    };

    private final OnItemClickListener itemClickListener;

    public NewLibraryAdapter(OnItemClickListener itemClickListener,
                             ExtendedAdapterListUpdateCallback.OnFirstInsertedListener firstInsertedListener) {
        super(new LibraryAdapterCallback(), firstInsertedListener);
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.start_library_item, parent, false), clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        Timber.d("pos: %d payload: %s", position, payloads);
        if (!payloads.isEmpty()) {
            LibraryPaper libraryPaper = getItem(position);
            bindProgress(holder, libraryPaper);
            bindState(holder, libraryPaper.getPaper());
            bindSelected(holder, libraryPaper);
        } else super.onBindViewHolder(holder, position, payloads);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LibraryPaper libraryPaper = getItem(position);
        bindImage(holder, libraryPaper.getPaper());
        bindBadge(holder, libraryPaper.getPaper());
        bindDate(holder, libraryPaper.getPaper());

        bindProgress(holder, libraryPaper);
        bindState(holder, libraryPaper.getPaper());
        bindSelected(holder, libraryPaper);
    }

    private void bindImage(ViewHolder holder, Paper paper) {
        Picasso.with(holder.image.getContext())
               .load(paper.getImage())
               .placeholder(R.drawable.dummy)
               .networkPolicy(NetworkPolicy.OFFLINE)
               .into(holder.image, new MissingCoverCallback(holder.image, paper) {
                   @Override
                   public void onError(ImageView imageView, Paper paper) {
                       Picasso.with(imageView.getContext())
                              .load(paper.getImage())
                              .placeholder(R.drawable.dummy)
                              .into(imageView);
                   }

                   @Override
                   public void onSuccess(Paper paper) {

                   }
               });
    }

    private void bindBadge(ViewHolder holder, Paper paper) {
        if (paper.isKiosk()) {
            holder.badge.setText(R.string.string_badge_kiosk);
            holder.badge.setVisibility(View.VISIBLE);
        } else holder.badge.setVisibility(View.GONE);

    }

    private void bindDate(ViewHolder holder, Paper paper) {
        try {
            holder.date.setText(paper.getDate(DateFormat.MEDIUM));
        } catch (ParseException e) {
            holder.date.setText(e.getMessage());
        }
        try {
            holder.card.setContentDescription(paper.getDate(DateFormat.LONG));
        } catch (ParseException e) {
            Timber.e(e);
        }

    }

    private void bindProgress(ViewHolder holder, LibraryPaper libraryPaper) {
        holder.progress.setProgress(100 - libraryPaper.getProgress());
    }

    private void bindState(ViewHolder holder, Paper paper) {
        if (paper.isDownloading()) {
            holder.wait.setVisibility(View.VISIBLE);
        } else {
            holder.wait.setVisibility(View.GONE);
        }
    }

    private void bindSelected(ViewHolder holder, LibraryPaper libraryPaper) {
        if (libraryPaper.isSelected()) holder.selected.setVisibility(View.VISIBLE);
        else holder.selected.setVisibility(View.INVISIBLE);
    }


    private static class LibraryAdapterCallback extends DiffUtil.ItemCallback<LibraryPaper> {

        @Override
        public boolean areItemsTheSame(LibraryPaper oldItem, LibraryPaper newItem) {
            return oldItem.getBookId()
                          .equals(newItem.getBookId());
        }

        @Override
        public boolean areContentsTheSame(LibraryPaper oldItem, LibraryPaper newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public Object getChangePayload(LibraryPaper oldItem, LibraryPaper newItem) {
            return PAYLOAD_CHANGE;
//
//            Timber.i("%s %s", oldItem, newItem);
//            if (oldItem.getPaper()
//                       .isDownloading() != newItem.getPaper()
//                                                  .isDownloading()) return PAYLOAD_STATE;
//            if (oldItem.isSelected() != newItem.isSelected()) return PAYLOAD_SELECTED;
//            if (oldItem.getProgress() != newItem.getProgress()) return PAYLOAD_PROGRESS;
//            return null;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        CardView        card;
        TextView        date;
        TextView        badge;
        ImageView       image;
        ProgressBar     wait;
        ProgressBar     progress;
        FrameLayout     selected;
        OnClickListener clickListener;

        public ViewHolder(View itemView, OnClickListener clickListener) {
            super(itemView);
            this.clickListener = clickListener;
            card = itemView.findViewById(R.id.lib_item_card);
            date = itemView.findViewById(R.id.lib_item_date);
            badge = itemView.findViewById(R.id.lib_item_badge);
            image = itemView.findViewById(R.id.lib_item_facsimile);
            wait = itemView.findViewById(R.id.lib_item_wait);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                Drawable wrapDrawable = DrawableCompat.wrap(wait.getIndeterminateDrawable());
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(itemView.getContext(), R.color.library_item_text));
                wait.setIndeterminateDrawable(DrawableCompat.unwrap(wrapDrawable));
            } else {
                wait.getIndeterminateDrawable()
                    .setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.library_item_text),
                                    PorterDuff.Mode.SRC_IN);
            }
            progress = itemView.findViewById(R.id.lib_item_progress);
            selected = itemView.findViewById(R.id.lib_item_selected_overlay);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) clickListener.onClick(getAdapterPosition());
                }
            });
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (clickListener != null) clickListener.onLongClick(getAdapterPosition());
                    return true;
                }
            });
            ViewCompat.setImportantForAccessibility(badge, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
            ViewCompat.setImportantForAccessibility(date, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        public interface OnClickListener {
            void onClick(int position);

            void onLongClick(int position);
        }
    }

    public interface OnItemClickListener {
        void onClick(LibraryPaper libraryPaper, int position);

        void onLongClick(LibraryPaper libraryPaper, int position);
    }


    private static abstract class MissingCoverCallback extends PreloadImageCallback {

        final ImageView imageView;

        protected MissingCoverCallback(ImageView imageView, Paper paper) {
            super(paper);
            this.imageView = imageView;
        }

        @Override
        public void onError(Paper paper) {
            onError(imageView, paper);
        }

        public abstract void onError(ImageView imageView, Paper paper);
    }
}
