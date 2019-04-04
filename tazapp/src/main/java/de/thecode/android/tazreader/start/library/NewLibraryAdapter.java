package de.thecode.android.tazreader.start.library;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import de.thecode.android.tazreader.data.PaperWithDownloadState;
import de.thecode.android.tazreader.sync.PreloadImageCallback;
import de.thecode.android.tazreader.utils.TazListAdapter;
import de.thecode.android.tazreader.utils.extendedasyncdiffer.ExtendedAdapterListUpdateCallback;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static de.thecode.android.tazreader.data.DownloadState.NONE;

public class NewLibraryAdapter extends TazListAdapter<PaperWithDownloadState, NewLibraryAdapter.ViewHolder> {


    private static final String PAYLOAD_PROGRESS = "plProgress";
    private static final String PAYLOAD_SELECTED = "plSelected";
    private static final String PAYLOAD_STATE    = "plState";
    private static final String PAYLOAD_CHANGE   = "plChange";
    private static final String PAYLOAD_OTHER    = "plOther";

    private final ViewHolder.OnClickListener clickListener = new ViewHolder.OnClickListener() {
        @Override
        public void onClick(int position) {
            if (itemClickListener != null) itemClickListener.onClick(getItem(position), position);
        }

        @Override
        public void onLongClick(int position) {
            Paper paper = getItem(position);
            toggleSelection(paper, position);
        }
    };

    private final OnItemClickListener itemClickListener;
    private final PaperMetaData       paperMetaData;

    public NewLibraryAdapter(PaperMetaData paperMetaData, OnItemClickListener itemClickListener,
                             ExtendedAdapterListUpdateCallback.OnFirstInsertedListener firstInsertedListener) {
        super(new LibraryAdapterCallback(), firstInsertedListener);
        this.itemClickListener = itemClickListener;
        this.paperMetaData = paperMetaData;
    }

    public PaperMetaData getPaperMetaData() {
        return paperMetaData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.start_library_item, parent, false), clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloadsParameter) {
        List<Object> payloads = new ArrayList<>();
        for (Object payloadParamObject : payloadsParameter) {
            if (payloadParamObject instanceof List) {
                payloads.addAll((List) payloadParamObject);
            } else payloads.add(payloadParamObject);
        }
        Timber.d("pos: %d payload: %s", position, payloads);
        if (payloads.isEmpty() || payloads.contains(PAYLOAD_OTHER)) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            PaperWithDownloadState libraryPaper = getItem(position);
            if (payloads.contains(PAYLOAD_SELECTED)) bindSelected(holder, libraryPaper);
            if (payloads.contains(PAYLOAD_STATE)) bindState(holder, libraryPaper);
            if (payloads.contains(PAYLOAD_PROGRESS)) bindProgress(holder, libraryPaper);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaperWithDownloadState libraryPaper = getItem(position);
//        paperMetaData.setPosition(libraryPaper.getBookId(), position);
        bindImage(holder, libraryPaper);
        bindBadge(holder, libraryPaper);
        bindDate(holder, libraryPaper);
        bindProgress(holder, libraryPaper);
        bindState(holder, libraryPaper);
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

//        if (paper.isKiosk()) {
//            holder.badge.setText(R.string.string_badge_kiosk);
//            holder.badge.setVisibility(View.VISIBLE);
//        } else
//        if (paper.hasDownloadingState() || paper.hasExtractingState()) {
//            switch (paper.getState()) {
//                case Paper.STATE_DOWNLOADING:
//                    holder.badge.setText("Herunterladen");
//                    break;
//                case Paper.STATE_EXTRACTING:
//                    holder.badge.setText("Entpacken");
//                    break;
//
//            }
//            holder.badge.setVisibility(View.VISIBLE);
//        } else {
//
//        }
        holder.badge.setVisibility(View.GONE);
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

    private void bindProgress(ViewHolder holder, PaperWithDownloadState libraryPaper) {
        holder.progress.setProgress(100 - libraryPaper.getProgress());
    }

    private void bindState(ViewHolder holder, PaperWithDownloadState paper) {
        switch (paper.getDownloadState()) {
            case DOWNLOADING:
            case DOWNLOADED:
                holder.progress.setVisibility(View.VISIBLE);
                holder.stateLayout.setVisibility(View.VISIBLE);
                holder.state.setText(R.string.string_library_item_download_state);
                holder.infoText.setVisibility(View.VISIBLE);
                break;
            case EXTRACTING:
                holder.progress.setVisibility(View.VISIBLE);
                holder.stateLayout.setVisibility(View.VISIBLE);
                holder.state.setText(R.string.string_library_item_extract_state);
                holder.infoText.setVisibility(View.VISIBLE);
                break;
            case CHECKING:
                holder.progress.setVisibility(View.VISIBLE);
                holder.stateLayout.setVisibility(View.VISIBLE);
                holder.state.setText(R.string.string_library_item_checking_state);
                holder.infoText.setVisibility(View.VISIBLE);
                break;
            case READY:
                holder.progress.setVisibility(View.GONE);
                holder.stateLayout.setVisibility(View.GONE);
                holder.infoText.setVisibility(View.GONE);
                break;
            default:
                holder.progress.setVisibility(View.VISIBLE);
                holder.stateLayout.setVisibility(View.GONE);
                holder.infoText.setVisibility(View.GONE);
                break;
        }
    }

    private void bindSelected(ViewHolder holder, Paper libraryPaper) {
        if (paperMetaData.isSelected(libraryPaper.getBookId())) holder.selected.setVisibility(View.VISIBLE);
        else holder.selected.setVisibility(View.INVISIBLE);
    }

    public void toggleSelection(Paper paper, int position) {
        if (paperMetaData.setSelected(paper.getBookId(), !paperMetaData.isSelected(paper.getBookId()))) {
            notifyItemChanged(position, PAYLOAD_SELECTED);
            if (itemClickListener != null) itemClickListener.onSelectionChanged();
        }
    }

    public void selectNone() {
        if (paperMetaData.clearSelected()) notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTED);
    }

    public void selectAll() {
        boolean changed = false;
        for (Paper paper : getHelper().getCurrentList()) {
            if (paperMetaData.setSelected(paper.getBookId(), true)) {
                changed = true;
            }
        }
        if (changed) {
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTED);
            if (itemClickListener != null) itemClickListener.onSelectionChanged();
        }
    }

    public void selectionInverse() {
        boolean changed = false;
        for (Paper paper : getHelper().getCurrentList()) {
            if (paperMetaData.setSelected(paper.getBookId(), !paperMetaData.isSelected(paper.getBookId()))) {
                changed = true;
            }
        }
        if (changed) {
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTED);
            if (itemClickListener != null) itemClickListener.onSelectionChanged();
        }

    }

    public void selectNotDownloadedPapers() {
        boolean changed = false;
        for (PaperWithDownloadState paper : getHelper().getCurrentList()) {
            if (paperMetaData.setSelected(paper.getBookId(), paper.getDownloadState() == NONE)) {
                changed = true;
            }
        }
        if (changed) {
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTED);
            if (itemClickListener != null) itemClickListener.onSelectionChanged();
        }
    }


    private static class LibraryAdapterCallback extends DiffUtil.ItemCallback<PaperWithDownloadState> {

        @Override
        public boolean areItemsTheSame(PaperWithDownloadState oldItem, PaperWithDownloadState newItem) {
            return oldItem.getBookId()
                          .equals(newItem.getBookId());
        }

        @Override
        public boolean areContentsTheSame(PaperWithDownloadState oldItem, PaperWithDownloadState newItem) {
            return oldItem.equals(newItem);
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull PaperWithDownloadState oldItem, @NonNull PaperWithDownloadState newItem) {
            List<Object> payloads = new ArrayList<>();
            if (oldItem.getDownloadState() != newItem.getDownloadState()) payloads.add(PAYLOAD_STATE);
            oldItem.setDownloadState(newItem.getDownloadState());
            if (oldItem.getProgress() != newItem.getProgress()) payloads.add(PAYLOAD_PROGRESS);
            oldItem.setProgress(newItem.getProgress());
            if (!areContentsTheSame(oldItem, newItem)) payloads.add(PAYLOAD_OTHER);
            return payloads;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final CardView        card;
        final TextView        date;
        final TextView        badge;
        final ImageView       image;
        final ProgressBar     progress;
        //        final FrameLayout     overlay;
        final FrameLayout     selected;
        final View            stateLayout;
        final TextView        state;
        final TextView        infoText;
        final OnClickListener listener;


        public ViewHolder(View itemView, OnClickListener clickListener) {
            super(itemView);
            this.listener = clickListener;
            card = itemView.findViewById(R.id.lib_item_card);
            date = itemView.findViewById(R.id.lib_item_date);
            badge = itemView.findViewById(R.id.lib_item_badge);
            image = itemView.findViewById(R.id.lib_item_facsimile);
            stateLayout = itemView.findViewById(R.id.lib_item_state);
            infoText = itemView.findViewById(R.id.info_note);
            state = itemView.findViewById(R.id.lib_item_state_text);
            ProgressBar wait = itemView.findViewById(R.id.lib_item_state_wait);
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
//            overlay = itemView.findViewById(R.id.lib_item_overlay);
            card.setOnClickListener(v -> {
                if (listener != null) listener.onClick(getAdapterPosition());
            });
            card.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(getAdapterPosition());
                return true;
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
        void onClick(PaperWithDownloadState libraryPaper, int position);

        void onSelectionChanged();
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

    public static class PaperMetaData {

        public final List<String> selectedList = new ArrayList<>();
        //        public final Map<String, Integer> progressMap  = new HashMap<>();
//        public final Map<String, Integer> positionMap  = new HashMap<>();

        public int getSelectedCount() {
            return selectedList.size();
        }

        public boolean setSelected(String bookId, boolean selected) {
            boolean changed = selected != isSelected(bookId);
            if (selected) {
                selectedList.add(bookId);
            } else {
                selectedList.remove(bookId);
            }
            return changed;
        }

        public boolean isSelected(String bookId) {
            return selectedList.contains(bookId);
        }

        public boolean clearSelected() {
            if (selectedList.size() == 0) {
                return false;
            } else {
                selectedList.clear();
                return true;
            }
        }

        public String[] getSelected() {
            return selectedList.toArray(new String[0]);
        }

//        public int getProgress(String bookId) {
//            if (progressMap.containsKey(bookId)) return progressMap.get(bookId);
//            else return 0;
//        }
//
//        public boolean setProgress(String bookId, int value) {
//            if (progressMap.containsKey(bookId) && progressMap.get(bookId) == value) return false;
//            progressMap.put(bookId, value);
//            return true;
//        }

//        public void setPosition(String bookId, int position) {
//            positionMap.put(bookId, position);
//        }
//
//        public int getPosition(String bookId) {
//            return positionMap.get(bookId);
//        }


    }

}
