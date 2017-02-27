package de.thecode.android.tazreader.reader.index;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import de.thecode.android.tazreader.reader.HelpActivity;
import de.thecode.android.tazreader.reader.IReaderCallback;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.reader.SettingsDialog;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.TintHelper;
import de.thecode.android.tazreader.widget.CustomToolbar;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class IndexFragment extends BaseFragment {

    private static final String ARGUMENT_BOOKMARKFILTER = "bookmarkfilter";

    CustomToolbar toolbar;
    List<IIndexItem> index = new ArrayList<>();
    IndexRecyclerViewAdapter adapter;

    int bookmarkColorActive;
    int bookmarkColorNormal;

    boolean mShowSubtitles;

    IIndexViewHolderClicks mClickListener;
    RecyclerView mRecyclerView;

    IReaderCallback mReaderCallback;

    public IndexFragment() {

    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mReaderCallback = (IReaderCallback) activity;

        bookmarkColorNormal = ContextCompat.getColor(activity, R.color.index_bookmark_off);
        bookmarkColorActive = ContextCompat.getColor(activity, R.color.index_bookmark_on);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new IndexRecyclerViewAdapter();
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
                        adapter.buildPositions();
                        break;
                    case R.id.toolbar_bookmark_on:
                        setFilterBookmarksToolbarItems(true);
                        adapter.buildPositions();
                        break;
                    case R.id.toolbar_expand:
                        adapter.expand();
                        break;
                    case R.id.toolbar_collapse:
                        adapter.collapse();
                        break;
                    case R.id.toolbar_index_short:
                        setIndexVerbose(false);
                        break;
                    case R.id.toolbar_index_full:
                        setIndexVerbose(true);
                        break;
                    case R.id.toolbar_index_help:
                        Intent helpIntent = new Intent(getActivity(), HelpActivity.class);
                        startActivity(helpIntent);
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
                getActivity().finish();
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

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).colorResId(R.color.index_divider)
                                                                                                 .sizeResId(R.dimen.index_divider_size)
                                                                                                 .marginResId(R.dimen.index_divider_margin)
                                                                                                 .showLastDivider()
                                                                                                 .build());
        mRecyclerView.setAdapter(adapter);

        setIndexVerbose(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.CONTENTVERBOSE, true));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.buildPositions();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ARGUMENT_BOOKMARKFILTER, mReaderCallback.isFilterBookmarks());
        super.onSaveInstanceState(outState);
    }

    public void init(Paper paper) {
        Timber.d("paper: %s", paper);
        index.clear();

        for (Source source : paper.getPlist()
                                  .getSources()) {
            index.add(source);
            for (Book book : source.getBooks()) {
                for (Category category : book.getCategories()) {
                    index.add(category);
                    if (category.hasIndexChilds()) index.addAll(category.getIndexChilds());
                }
            }
        }

        for (TopLink toplink : paper.getPlist()
                                    .getToplinks()) {
            index.add(toplink);
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
        item.setIndexChildsVisible(!item.areIndexChildsVisible());
        adapter.buildPositions();
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
            adapter.notifyDataSetChanged();
            adapter.buildPositions();
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
        TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.CONTENTVERBOSE, bool);
        Menu menu = toolbar.getMenu();
        MenuItem menuItemFull = menu.findItem(R.id.toolbar_index_full);
        MenuItem menuItemShort = menu.findItem(R.id.toolbar_index_short);
        menuItemFull.setVisible(!bool);
        menuItemShort.setVisible(bool);
    }

    private class IndexRecyclerViewAdapter extends RecyclerView.Adapter<Viewholder> {

        List<IIndexItem> positions = new ArrayList<>();
        //        HashMap<String,Integer> itemPositions = new HashMap<>();
        //        HashMap<Integer,String> itemOrder = new HashMap<>();

        public IndexRecyclerViewAdapter() {


        }

        void buildPositions() {
            positions.clear();
            boolean filter = false;
            //Workaround
            if (mReaderCallback != null) filter = mReaderCallback.isFilterBookmarks();
            if (index != null) {
                if (!filter) {
                    for (IIndexItem indexItem : index) {
                        if (indexItem.isVisible()) positions.add(indexItem);
                    }
                } else {
                    for (IIndexItem indexItem : index) {
                        if (indexItem.isVisible()) {
                            if (indexItem.isBookmarked()) positions.add(indexItem);
                            else if (indexItem.hasBookmarkedChilds()) positions.add(indexItem);
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }

        public void expand() {
            for (IIndexItem indexItem : index) {
                indexItem.setIndexChildsVisible(true);
            }
            buildPositions();
        }

        public void collapse() {
            mRecyclerView.stopScroll();
            for (IIndexItem indexItem : index) {
                indexItem.setIndexChildsVisible(false);
            }
            buildPositions();
        }

        @Override
        public int getItemViewType(int position) {
            return positions.get(position)
                            .getType()
                            .ordinal();
        }

        @Override
        public int getItemCount() {
            int count = 0;
            if (positions != null) count = positions.size();
            return count;
        }

        public IIndexItem getItem(int position) {
            // Log.v();
            if (positions != null) return positions.get(position);
            return null;
        }

        public int getPosition(String key) {

            int result = -1;
            for (IIndexItem indexItem : positions) {
                if (indexItem.getKey()
                             .equals(key)) {
                    result = positions.indexOf(indexItem);
                    break;
                }
            }
            Timber.d("key: %s %s", key, result);
            return result;
        }

        public int getPosition(IIndexItem item) {
            return positions.indexOf(item);
        }

        @SuppressWarnings("incomplete-switch")
        @Override
        public void onBindViewHolder(Viewholder viewholder, int position) {
            IIndexItem item = positions.get(position);

            if (!item.isLink() && item.getKey()
                                      .equals(mReaderCallback.getCurrentKey()))
                viewholder.setCurrent(true);
            else viewholder.setCurrent(false);


            switch (IIndexItem.Type.values()[viewholder.getItemViewType()]) {
                case SOURCE:
                    ((SourceViewholder) viewholder).title.setText(item.getTitle());
                    if (item.areIndexChildsVisible())
                        ((SourceViewholder) viewholder).image.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_remove_24dp));
                    else
                        ((SourceViewholder) viewholder).image.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_add_24dp));
                    break;
                case CATEGORY:
                    ((CategoryViewholder) viewholder).title.setText(item.getTitle());
                    if (item.areIndexChildsVisible())
                        ((CategoryViewholder) viewholder).image.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_remove_24dp));
                    else
                        ((CategoryViewholder) viewholder).image.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_add_24dp));
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
                        layoutParams.topMargin = getActivity().getResources()
                                                              .getDimensionPixelOffset(R.dimen.reader_bookmark_offset_active);
                    } else {
                        TintHelper.tintDrawable(((ArticleViewholder) viewholder).bookmark.getDrawable(), bookmarkColorNormal);
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
        TextView title;
    }

    private class CategoryViewholder extends Viewholder {

        public CategoryViewholder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            image = (ImageView) itemView.findViewById(R.id.image);

        }

        ImageView image;
        TextView title;
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
            ((FrameLayout) itemView.findViewById(R.id.bookmarkClickLayout)).setOnClickListener(this);
        }

        public void onClick(View v) {
            if (mClickListener != null) {
                if (v.getId() == R.id.bookmarkClickLayout)
                    mClickListener.onBookmarkClick(getPosition());
                else {
                    mReaderCallback.closeDrawers();
                    super.onClick(v);
                }
            }
        }

        ImageView bookmark;
        TextView title;
        TextView subtitle;

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

    String mCurrentKey = null;

    public void updateCurrentPosition(String key) {
        Timber.d("key: %s", key);

        if (mCurrentKey != null) {
            IIndexItem lastitem = mReaderCallback.getPaper()
                                                 .getPlist()
                                                 .getIndexItem(mCurrentKey);
            if (lastitem != null) {
                int where = adapter.getPosition(lastitem);
                adapter.notifyItemChanged(where);
            }
        }

        IIndexItem item = mReaderCallback.getPaper()
                                         .getPlist()
                                         .getIndexItem(key);

        if (item != null) {
            int where = adapter.getPosition(item);
            if (where != -1 && mRecyclerView != null) {
                mRecyclerView.scrollToPosition(where);
                adapter.notifyItemChanged(where);
            }
        }

        mCurrentKey = key;

    }

    public interface IIndexViewHolderClicks {
        public void onItemClick(int position);

        public void onBookmarkClick(int position);
    }

}
