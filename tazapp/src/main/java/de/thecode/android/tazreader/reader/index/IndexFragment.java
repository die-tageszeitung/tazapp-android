package de.thecode.android.tazreader.reader.index;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Paper.Plist.Book;
import de.thecode.android.tazreader.data.Paper.Plist.Category;
import de.thecode.android.tazreader.data.Paper.Plist.Page.Article;
import de.thecode.android.tazreader.data.Paper.Plist.Source;
import de.thecode.android.tazreader.data.Paper.Plist.TopLink;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.IReaderCallback;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.reader.SettingsDialog;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.TintHelper;
import de.thecode.android.tazreader.widget.CustomToolbar;

import org.mightyfrog.widget.CenteringRecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class IndexFragment extends BaseFragment {

    private static final String ARGUMENT_CURRENTKEY = "currentIndexKey";

    CustomToolbar toolbar;
    Map<String, IIndexItem> index = new LinkedHashMap<>();
    IndexRecyclerViewAdapter adapter;

    int   bookmarkColorActive;
    int   bookmarkColorNormal;
    float iconButtonAlpha;

    boolean mShowSubtitles;
    String currentlyMarkedInIndexKey = null;

    IIndexViewHolderClicks mClickListener;
    CenteringRecyclerView  mRecyclerView;

    IReaderCallback mReaderCallback;


    public IndexFragment() {

    }


//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//
//        mReaderCallback = (IReaderCallback) activity;
//
//        bookmarkColorNormal = ContextCompat.getColor(activity, R.color.index_bookmark_off);
//        bookmarkColorActive = ContextCompat.getColor(activity, R.color.index_bookmark_on);
//
//    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IReaderCallback) mReaderCallback = (IReaderCallback) context;
        else throw new RuntimeException(context.toString() + " must implement " + IReaderCallback.class.getSimpleName());

        bookmarkColorNormal = ContextCompat.getColor(context, R.color.index_bookmark_off);
        bookmarkColorActive = ContextCompat.getColor(context, R.color.index_bookmark_on);

        TypedValue outValue = new TypedValue();
        context.getResources()
               .getValue(R.dimen.icon_button_alpha, outValue, true);
        iconButtonAlpha = outValue.getFloat();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(mReaderCallback.getPaper());
        adapter = new IndexRecyclerViewAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                adapter.rebuildPositions();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                adapter.rebuildPositions();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                adapter.rebuildPositions();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                adapter.rebuildPositions();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                adapter.rebuildPositions();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.reader_index, container, false);
        toolbar = (CustomToolbar) view.findViewById(R.id.toolbar_article);
        toolbar.setItemColor(ContextCompat.getColor(inflater.getContext(), R.color.toolbar_foreground_color));
        toolbar.inflateMenu(R.menu.reader_index);
        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_bookmark_off:
                        setFilterBookmarksToolbarItems(false);
                        adapter.notifyDataSetChanged();
                        break;
                    case R.id.toolbar_bookmark_on:
                        setFilterBookmarksToolbarItems(true);
                        adapter.notifyDataSetChanged();
                        break;
                    case R.id.toolbar_expand:
                        TazSettings.getInstance(getContext())
                                   .setIndexAlwaysExpanded(true);
                        expandAll(true);
                        break;
                    case R.id.toolbar_collapse:
                        TazSettings.getInstance(getContext())
                                   .setIndexAlwaysExpanded(false);
                        expandAll(false);
                        break;
                    case R.id.toolbar_index_short:
                        setIndexVerbose(false);
                        break;
                    case R.id.toolbar_index_full:
                        setIndexVerbose(true);
                        break;
                    case R.id.toolbar_index_help:
                        if (mReaderCallback != null) {
                            mReaderCallback.onShowHelp();
                        }
                        break;
                }
                return true;
            }
        });

        CustomToolbar toolbar2 = (CustomToolbar) view.findViewById(R.id.toolbar2);
        toolbar2.setItemColor(ContextCompat.getColor(inflater.getContext(), R.color.toolbar_foreground_color));
        toolbar2.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
        toolbar2.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(getActivity());
            }
        });
        toolbar2.inflateMenu(R.menu.reader_index_main);
        toolbar2.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_settings:
                        // mReaderCallback.showSettingsFragment();
                        new SettingsDialog.Builder().setPositiveButton()
                                                    .setPadding(0)
                                                    .buildSupport()
                                                    .show(getFragmentManager(), ReaderActivity.TAG_FRAGMENT_DIALOG_SETTING);
                        // new SettingsDialogFragment().show(getFragmentManager(), Reader.TAG_FRAGMENT_DIALOG_SETTING);
                        mReaderCallback.closeDrawers();
                        break;
                }
                return true;
            }
        });


        setFilterBookmarksToolbarItems(mReaderCallback.isFilterBookmarks());

        mRecyclerView = (CenteringRecyclerView) view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).colorResId(R.color.index_divider)
                                                                                                 .sizeResId(R.dimen.index_divider_size)
                                                                                                 .marginResId(R.dimen.index_divider_margin)
                                                                                                 .showLastDivider()
                                                                                                 .build());
        mRecyclerView.setAdapter(adapter);
        if (savedInstanceState == null) expandAll(TazSettings.getInstance(getContext())
                                                             .isIndexAlwaysExpanded());
        else {
            currentlyMarkedInIndexKey = savedInstanceState.getString(ARGUMENT_CURRENTKEY);
        }

        setIndexVerbose(TazSettings.getInstance(getActivity())
                                   .getPrefBoolean(TazSettings.PREFKEY.CONTENTVERBOSE, true));

        return view;
    }

    private void expandAll(boolean expand) {
        for (Map.Entry<String, IIndexItem> entry : index.entrySet()) {
            entry.getValue()
                 .setIndexChildsVisible(expand);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //adapter.rebuildPositions();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARGUMENT_CURRENTKEY, currentlyMarkedInIndexKey);
        super.onSaveInstanceState(outState);
    }

    private void init(Paper paper) {
        Timber.d("paper: %s", paper);
        index.clear();
        if (paper != null && paper.getPlist() != null) {
            for (Source source : paper.getPlist()
                                      .getSources()) {
                //index.add(source);
                for (Book book : source.getBooks()) {
                    for (Category category : book.getCategories()) {
                        index.put(category.getKey(), category);
                        if (category.hasIndexChilds()) {
                            for (IIndexItem categoryChild : category.getIndexChilds()) {
                                index.put(categoryChild.getKey(), categoryChild);
                            }
                        }
                    }
                }
            }
        }

        for (TopLink toplink : paper.getPlist()
                                    .getToplinks()) {
            index.put("toplink_" + toplink.getKey(), toplink);
        }
        mClickListener = new IIndexViewHolderClicks() {

            @Override
            public void onItemClick(int position) {
                IndexFragment.this.onItemClick(position);

            }

            @Override
            public void onBookmarkClick(int position) {
                IndexFragment.this.onBookmarkClick(position);
            }
        };
    }

    private void onItemClick(int position) {
        IIndexItem item = adapter.getItem(position);
        boolean areIndexChildsVisible = item.areIndexChildsVisible();
        item.setIndexChildsVisible(!item.areIndexChildsVisible());
        adapter.notifyItemChanged(position);
        if (areIndexChildsVisible) {
            adapter.notifyItemRangeRemoved(position + 1, item.getIndexChildCount());
        } else {
            adapter.notifyItemRangeInserted(position + 1, item.getIndexChildCount());
        }
        mReaderCallback.onLoad(item.getKey());
    }

    private void onBookmarkClick(int position) {
        IIndexItem item = adapter.getItem(position);
        if (item.getType() == IIndexItem.Type.ARTICLE) {
            Article article = (Article) item;
            mReaderCallback.onBookmarkClick(article);
        }
    }

    public void onBookmarkChange(String key) {
        if (mReaderCallback.isFilterBookmarks()) {
            //adapter.rebuildPositions();
            adapter.notifyDataSetChanged();
        } else {
            IIndexItem item = mReaderCallback.getPaper()
                                             .getPlist()
                                             .getIndexItem(key);
            if (item != null) {
                int where = adapter.getPosition(item);
                adapter.notifyItemChanged(where);
            }
        }
    }

    public void setFilterBookmarksToolbarItems(boolean bool) {
        //mFilterBookmarks = bool;
        mReaderCallback.setFilterBookmarks(bool);
        Menu menu = toolbar.getMenu();
        MenuItem menuItemOn = menu.findItem(R.id.toolbar_bookmark_on);
        MenuItem menuItemOff = menu.findItem(R.id.toolbar_bookmark_off);
        menuItemOn.setVisible(!bool);
        menuItemOff.setVisible(bool);
    }

    public void setIndexVerbose(boolean bool) {
        mShowSubtitles = bool;
        adapter.notifyDataSetChanged();
        TazSettings.getInstance(getActivity())
                   .setPref(TazSettings.PREFKEY.CONTENTVERBOSE, bool);
        Menu menu = toolbar.getMenu();
        MenuItem menuItemFull = menu.findItem(R.id.toolbar_index_full);
        MenuItem menuItemShort = menu.findItem(R.id.toolbar_index_short);
        menuItemFull.setVisible(!bool);
        menuItemShort.setVisible(bool);
    }

    private class IndexRecyclerViewAdapter extends RecyclerView.Adapter<Viewholder> {

        List<String> positions = new ArrayList<>();
        //        HashMap<String,Integer> itemPositions = new HashMap<>();
        //        HashMap<Integer,String> itemOrder = new HashMap<>();

        public IndexRecyclerViewAdapter() {


        }


        void rebuildPositions() {
            positions.clear();
            boolean filter = false;
            //Workaround
            if (mReaderCallback != null) filter = mReaderCallback.isFilterBookmarks();
            if (index != null) {
                if (!filter) {
                    for (Map.Entry<String, IIndexItem> entry : index.entrySet()) {
                        if (entry.getValue()
                                 .isVisible()) positions.add(entry.getKey());
                    }
                } else {
                    for (Map.Entry<String, IIndexItem> entry : index.entrySet()) {
                        if (entry.getValue()
                                 .isVisible()) {
                            if (entry.getValue()
                                     .isBookmarked()) positions.add(entry.getKey());
                            else if (entry.getValue()
                                          .hasBookmarkedChilds()) positions.add(entry.getKey());
                        }
                    }
                }
            }
        }


        @Override
        public int getItemViewType(int position) {
            return index.get(positions.get(position))
                        .getType()
                        .ordinal();
        }

        @Override
        public int getItemCount() {
            if (positions != null) {
                return positions.size();
            }
            return 0;
        }

        public IIndexItem getItem(int position) {
            // Log.v();
            if (index != null && positions != null) return index.get(positions.get(position));
            return null;
        }

        public int getPosition(String key) {
            return positions.indexOf(key);
        }

        public int getPosition(IIndexItem item) {
            return getPosition(item.getKey());
        }

        @SuppressWarnings("incomplete-switch")
        @Override
        public void onBindViewHolder(Viewholder viewholder, int position) {
            IIndexItem item = index.get(positions.get(position));

            if (!item.isLink() && item.getKey()
                                      .equals(mReaderCallback.getCurrentKey())) viewholder.setCurrent(true);
            else viewholder.setCurrent(false);


            switch (IIndexItem.Type.values()[viewholder.getItemViewType()]) {
                case SOURCE:
                    ((SourceViewholder) viewholder).title.setText(item.getTitle());
                    if (item.areIndexChildsVisible())
                        ((SourceViewholder) viewholder).image.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                                                                                                         R.drawable.ic_remove_24dp));
                    else ((SourceViewholder) viewholder).image.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                                                                                                          R.drawable.ic_add_24dp));
                    break;
                case CATEGORY:
                    ((CategoryViewholder) viewholder).title.setText(item.getTitle());
                    if (item.areIndexChildsVisible())
                        ((CategoryViewholder) viewholder).image.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                                                                                                           R.drawable.ic_remove_24dp));
                    else ((CategoryViewholder) viewholder).image.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                                                                                                            R.drawable.ic_add_24dp));
                    break;
                case PAGE:
                    ((PageViewholder) viewholder).title.setText(item.getTitle());
                    break;
                case ARTICLE:
                    ((ArticleViewholder) viewholder).title.setText(item.getTitle());

                    if (((Article) item).getSubtitle() == null || "".equals(((Article) item).getSubtitle()) || !mShowSubtitles)
                        ((ArticleViewholder) viewholder).subtitle.setVisibility(View.GONE);
                    else {
                        ((ArticleViewholder) viewholder).subtitle.setVisibility(View.VISIBLE);
                        ((ArticleViewholder) viewholder).subtitle.setText(((Article) item).getSubtitle());
                    }
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) ((ArticleViewholder) viewholder).bookmark.getLayoutParams();
                    if (item.isBookmarked()) {
                        TintHelper.tintDrawable(((ArticleViewholder) viewholder).bookmark.getDrawable(), bookmarkColorActive);
                        ((ArticleViewholder) viewholder).bookmark.setAlpha(1F);
                        layoutParams.topMargin = getActivity().getResources()
                                                              .getDimensionPixelOffset(R.dimen.reader_bookmark_offset_active);
                    } else {
                        TintHelper.tintDrawable(((ArticleViewholder) viewholder).bookmark.getDrawable(), bookmarkColorNormal);
                        ((ArticleViewholder) viewholder).bookmark.setAlpha(iconButtonAlpha);
                        layoutParams.topMargin = getActivity().getResources()
                                                              .getDimensionPixelOffset(R.dimen.reader_bookmark_offset_normal);
                    }
                    ((ArticleViewholder) viewholder).bookmark.setLayoutParams(layoutParams);
                    break;
                case TOPLINK:
                    ((ToplinkViewholder) viewholder).title.setText(item.getTitle());
                    break;
            }
        }

        @SuppressWarnings("incomplete-switch")
        @Override
        public Viewholder onCreateViewHolder(ViewGroup parent, int itemType) {
            switch (IIndexItem.Type.values()[itemType]) {
                case SOURCE:
                    return new SourceViewholder(LayoutInflater.from(parent.getContext())
                                                              .inflate(R.layout.reader_index_source, parent, false));
                case CATEGORY:
                    return new CategoryViewholder(LayoutInflater.from(parent.getContext())
                                                                .inflate(R.layout.reader_index_category, parent, false));
                case PAGE:
                    return new PageViewholder(LayoutInflater.from(parent.getContext())
                                                            .inflate(R.layout.reader_index_page, parent, false));
                case ARTICLE:
                    return new ArticleViewholder(LayoutInflater.from(parent.getContext())
                                                               .inflate(R.layout.reader_index_article, parent, false));
                case TOPLINK:
                    return new ToplinkViewholder(LayoutInflater.from(parent.getContext())
                                                               .inflate(R.layout.reader_index_toplink, parent, false));
            }
            return null;
        }

    }

    private class Viewholder extends RecyclerView.ViewHolder implements View.OnClickListener {

        //IIndexViewHolderClicks mClickListener;
        View mCurrentMarker;


        public Viewholder(View itemView) {
            super(itemView);
            //mClickListener = listener;
            itemView.setOnClickListener(this);
            mCurrentMarker = itemView.findViewById(R.id.currentMarker);
        }

        @Override
        public void onClick(View v) {
            if (mClickListener != null) mClickListener.onItemClick(getPosition());
        }


        public void setCurrent(boolean bool) {
            if (mCurrentMarker != null) {
                if (bool) mCurrentMarker.setVisibility(View.VISIBLE);
                else mCurrentMarker.setVisibility(View.INVISIBLE);
            }
        }

    }

    private class SourceViewholder extends Viewholder {

        public SourceViewholder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            image = (ImageView) itemView.findViewById(R.id.image);
        }

        ImageView image;
        TextView  title;
    }

    private class CategoryViewholder extends Viewholder {

        public CategoryViewholder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            image = (ImageView) itemView.findViewById(R.id.image);

        }

        ImageView image;
        TextView  title;
    }

    private class PageViewholder extends Viewholder {

        public PageViewholder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
        }

        @Override
        public void onClick(View v) {
            mReaderCallback.closeDrawers();
            super.onClick(v);
        }

        TextView title;
    }

    private class ArticleViewholder extends Viewholder {

        public ArticleViewholder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            bookmark = (ImageView) itemView.findViewById(R.id.bookmark);
            itemView.findViewById(R.id.bookmarkClickLayout)
                    .setOnClickListener(this);
        }

        public void onClick(View v) {
            if (mClickListener != null) {
                if (v.getId() == R.id.bookmarkClickLayout) mClickListener.onBookmarkClick(getPosition());
                else {
                    mReaderCallback.closeDrawers();
                    super.onClick(v);
                }
            }
        }

        ImageView bookmark;
        TextView  title;
        TextView  subtitle;

    }

    private class ToplinkViewholder extends Viewholder {

        public ToplinkViewholder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
        }

        @Override
        public void onClick(View v) {
            mReaderCallback.closeDrawers();
            super.onClick(v);
        }

        TextView title;
    }


    public void updateCurrentPosition(String key) {
        Timber.d("key: %s", key);


        boolean alwaysExpanded = TazSettings.getInstance(getContext())
                                            .isIndexAlwaysExpanded();


        if (currentlyMarkedInIndexKey != null) {
            if (currentlyMarkedInIndexKey.equals(key)) return;
            IIndexItem lastitem = index.get(currentlyMarkedInIndexKey);
            if (lastitem != null) {
                int where = adapter.getPosition(lastitem);
                if (where != -1) {
                    adapter.notifyItemChanged(where);
                }
            }
        }

        if (!alwaysExpanded) {
            expandAll(false);
        }

        IIndexItem item = mReaderCallback.getPaper()
                                         .getPlist()
                                         .getIndexItem(key);
        int where = adapter.getPosition(item);

        if (where != -1) {
            adapter.notifyItemChanged(where);
            mRecyclerView.center(where);
        } else {
            IIndexItem parent = item.getIndexParent();
            if (!parent.areIndexChildsVisible()) {
                int parentWhere = adapter.getPosition(parent);
                onItemClick(parentWhere);
            }
            if (item.getType() != IIndexItem.Type.PAGE) {
                where = adapter.getPosition(item);
                if (where != -1) {
                    adapter.notifyItemChanged(where);
                    mRecyclerView.center(where);
                }

            } else {
                where = adapter.getPosition(parent);
                if (where != -1) mRecyclerView.head(where);
            }
        }

        currentlyMarkedInIndexKey = key;

    }

    public interface IIndexViewHolderClicks {
        public void onItemClick(int position);

        public void onBookmarkClick(int position);
    }

}
