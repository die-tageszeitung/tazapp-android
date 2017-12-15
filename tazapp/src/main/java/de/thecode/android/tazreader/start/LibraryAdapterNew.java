package de.thecode.android.tazreader.start;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.download.DownloadManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by mate on 10.02.2015.
 */
public class LibraryAdapterNew extends RecyclerView.Adapter<LibraryAdapterNew.ViewHolder> {

    private List<Paper> data;
    private List<Paper> selected;

    private static final String PAYLOAD_SELECTION = "SELECTION";

    //    int                           mCoverImageHeight;
//    int                           mCoverImageWidth;
    //Bitmap mPlaceHolderBitmap;
//    Context                       mContext;
//    WeakReference<IStartCallback> callback;
    OnItemClickListener     mClickListener;
    OnItemLongClickListener mLongClickListener;

    DownloadManager downloadHelper;

    LibraryInteractionListener listener;

    LibraryItemListener itemListener = new LibraryItemListener() {
        @Override
        public void onClick(int adapterPosition) {
            if (listener != null) listener.onClick(data.get(adapterPosition));
        }

        @Override
        public void onLongClick(int adapterPosition) {
            if (listener != null) listener.onLongClick(data.get(adapterPosition));
        }
    };

    public void update(List<Paper> newData) {
        data = newData;
        notifyDataSetChanged();
    }


    public LibraryAdapterNew(LibraryInteractionListener listener) {
        this.listener = listener;
        selected = new ArrayList<>();
        //super(context, cursor);

//        EventBus.getDefault()
//                .register(this);

//        mContext = context;
//        this.callback = new WeakReference<IStartCallback>(callback);
//
//        downloadHelper = DownloadManager.getInstance(context);

//        mClickListener = clickListener;
//        mLongClickListener = longClickListener;

//        mCoverImageHeight = context.getResources()
//                                   .getDimensionPixelSize(R.dimen.cover_image_height);
//        mCoverImageWidth = context.getResources()
//                                  .getDimensionPixelSize(R.dimen.cover_image_width);
        // mPlaceHolderBitmap = bitmapFromResource(context, R.drawable.dummy, mCoverImageWidth, mCoverImageHeight);
    }

//    private boolean hasCallback() {
//        return callback.get() != null;
//    }
//
//    public IStartCallback getCallback() {
//        return callback.get();
//    }

//    public void destroy() {
//        EventBus.getDefault()
//                .unregister(this);
//    }

//    public List<Long> getSelected() {
//        if (hasCallback()) return getCallback().getRetainData()
//                                               .getSelectedInLibrary();
//        return null;
//    }

//    public void select(long bookId) {
//        select(getItemPosition(bookId));
//    }
//
//    public void deselect(long bookId) {
//        deselect(getItemPosition(bookId));
//    }
//
//    public void select(int position) {
//
//        long bookId = getItemId(position);
//        //Log.v(position, bookId);
//
//        if (!isSelected(bookId)) getSelected().add(bookId);
//
//        notifyItemChanged(position);
//    }
//
//    public void deselect(int position) {
//        long bookId = getItemId(position);
//        //Log.v(position, bookId);
//        if (isSelected(bookId)) getSelected().remove(bookId);
//        notifyItemChanged(position);
//    }
//
//    public boolean isSelected(long bookId) {
//        return getSelected().contains(bookId);
//    }
//
//    public void selectAll() {
//        for (int i = 0; i < getItemCount(); i++) {
//            select(i);
//        }
//    }
//
//    public void deselectAll() {
//        for (int i = 0; i < getItemCount(); i++) {
//            deselect(i);
//        }
//    }
//
//
//    public void selectionInvert() {
//        for (int i = 0; i < getItemCount(); i++) {
//            long bookId = getItemId(i);
//            if (isSelected(bookId)) deselect(i);
//            else select(i);
//        }
//    }
//
//    public void selectNotLoaded() {
//        Cursor cursor = getCursor();
//
//        if (cursor != null) {
//            for (int i = 0; i < getItemCount(); i++) {
//                if (cursor.moveToPosition(i)) {
//                    Paper paper = new Paper(cursor);
//                    if (!(paper.isDownloaded() || paper.isDownloading())) {
//                        if (!isSelected(paper.getId())) select(paper.getId());
//                    }
//                }
//            }
//        }
//    }

//    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
//        this.mClickListener = onItemClickListener;
//    }
//
//    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
//        this.mLongClickListener = onItemLongClickListener;
//    }
//
//    public void removeClickLIstener() {
//        this.mLongClickListener = null;
//        this.mClickListener = null;
//    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onPaperDownloadFailed(PaperDownloadFailedEvent event) {
//        try {
//            notifyItemChanged(getItemPosition(event.getBookId()));
//        } catch (IllegalStateException e) {
//            Timber.w(e);
//        }
//    }


    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }


    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position, List<Object> payloads) {
        Paper paper = data.get(position);
        if (payloads.isEmpty() || payloads.contains(PAYLOAD_SELECTION)) {
            if (selected.contains(paper)) viewHolder.selected.setVisibility(View.VISIBLE);
            else viewHolder.selected.setVisibility(View.INVISIBLE);
        }
        if (payloads.isEmpty()) onBindViewHolder(viewHolder, position);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Paper paper = data.get(position);
        try {
            viewHolder.date.setText(paper.getDate(DateFormat.MEDIUM));
        } catch (ParseException e) {
            viewHolder.date.setText(e.getMessage());
        }
        Picasso.with(viewHolder.image.getContext())
               .load(paper.getImage())
               .placeholder(R.drawable.dummy)
               .networkPolicy(NetworkPolicy.OFFLINE)
               .into(viewHolder.image, new MissingCoverCallback(viewHolder.image, paper) {
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

        if (paper.isDownloading()) {
            viewHolder.wait.setVisibility(View.VISIBLE);
        } else {
            viewHolder.wait.setVisibility(View.GONE);
        }

        if (paper.isKiosk()) {
            viewHolder.badge.setText(R.string.string_badge_kiosk);
            viewHolder.badge.setVisibility(View.VISIBLE);
        } else viewHolder.badge.setVisibility(View.GONE);


        try {
            viewHolder.card.setContentDescription(paper.getDate(DateFormat.LONG));
        } catch (ParseException e) {
            Timber.e(e);
        }

    }

    public void changeSelection(Paper paper) {
        if (!selected.contains(paper)) selected.add(paper);
        else selected.remove(paper);
        notifyItemChanged(data.indexOf(paper),PAYLOAD_SELECTION);
    }

    public void select(List<Paper> paperList) {
        deselectAll();
        if (paperList == null) return;
        for (Paper paper : paperList) {
            changeSelection(paper);
        }
    }

    public void deselectAll() {
        List<Paper> copyOfSelected = new ArrayList<>(selected);
        for (Paper aSelected : copyOfSelected) {
            changeSelection(aSelected);
        }
    }

    public void selectAll() {
        for (Paper paper : data) {
            if (!selected.contains(paper)) changeSelection(paper);
        }
    }

    public void invertSelection() {
        for (Paper paper : data) {
            changeSelection(paper);
        }
    }


    public void selectNotDownloaded() {
        for (Paper paper : data) {
            if (!selected.contains(paper) && !(paper.isDownloading() || paper.isDownloaded())) changeSelection(paper);
        }
    }


    public List<Paper> getSelectedPapers() {
        return selected;
    }

    public Paper getItemForBookId(@NonNull String bookId) {
        for (Paper paper : data) {
            if (bookId.equals(paper.getBookId())) {
                return paper;
            }
        }
        return null;
    }

    public int getPositionForPaper(@NonNull Paper paper) {
        return data.indexOf(paper);
    }

    public int getPositionForBookId(@NonNull String bookId) {
        return getPositionForPaper(getItemForBookId(bookId));
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.start_library_item, parent, false), itemListener);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        CardView            card;
        TextView            date;
        TextView            badge;
        ImageView           image;
        ProgressBar         wait;
        ProgressBar         progress;
        FrameLayout         selected;
        //        private Paper _paper;
//        DownloadManager.DownloadProgressThread downloadProgressThread;
        LibraryItemListener listener;

        public ViewHolder(View itemView, final LibraryItemListener itemListener) {
            super(itemView);
            listener = itemListener;
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
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listener != null) listener.onLongClick(getAdapterPosition());
                    return true;
                }
            });
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onClick(getAdapterPosition());
                }
            });
            ViewCompat.setImportantForAccessibility(badge, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
            ViewCompat.setImportantForAccessibility(date, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }


//        public void setPaper(Paper paper) {
//            if (paper != _paper) {
//                if (downloadProgressThread != null) downloadProgressThread.interrupt();
//                this._paper = paper;
//                if (paper.isDownloaded()) setProgress(100);
//                else {
//                    if (paper.isDownloading()) {
//                        DownloadManager.DownloadState downloadState = downloadHelper.getDownloadState(paper.getDownloadId());
//                        setDownloadProgress(downloadState.getDownloadProgress());
//                        downloadProgressThread = downloadHelper.new DownloadProgressThread(paper.getDownloadId(), paper.getId());
//                        downloadProgressThread.start();
//                    } else setProgress(0);
//                }
//
//            }
//        }


        private void setProgress(int value) {
            progress.setProgress(100 - value);
        }

        private void setDownloadProgress(int value) {
            setProgress((value * 50) / 100);
        }

        private void setUnzipProgress(int value) {
            setProgress(((value * 50) / 100) + 50);
        }

//        @Subscribe(threadMode = ThreadMode.MAIN)
//        public void onDownloadProgress(DownloadProgressEvent event) {
//            if (_paper != null) {
//                if (_paper.getId() == event.getBookId()) {
//                    setDownloadProgress(event.getProgress());
//                }
//            }
//        }
//
//        @Subscribe(threadMode = ThreadMode.MAIN)
//        public void onUnzipProgress(UnzipProgressEvent event) {
//            if (_paper != null) {
//                if (_paper.getId() == event.getBookId()) {
//                    setUnzipProgress(event.getProgress());
//                }
//            }
//        }
    }

    public interface LibraryInteractionListener {
        void onClick(Paper paper);

        void onLongClick(Paper paper);
    }

    public interface LibraryItemListener {
        void onClick(int adapterPosition);

        void onLongClick(int adapterPosition);
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View v, int position);
    }

    private static abstract class MissingCoverCallback implements Callback {

        final ImageView imageView;
        final Paper     paper;

        protected MissingCoverCallback(ImageView imageView, Paper paper) {
            this.paper = paper;
            this.imageView = imageView;
        }

        @Override
        public void onError() {
            onError(imageView, paper);
        }

        @Override
        public void onSuccess() {
            onSuccess(paper);
        }

        public abstract void onSuccess(Paper paper);

        public abstract void onError(ImageView imageView, Paper paper);
    }

}




