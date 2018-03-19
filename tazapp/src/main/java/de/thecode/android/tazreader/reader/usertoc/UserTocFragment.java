package de.thecode.android.tazreader.reader.usertoc;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.reader.ReaderBaseFragment;
import de.thecode.android.tazreader.reader.SettingsDialog;
import de.thecode.android.tazreader.widget.CustomToolbar;
import de.thecode.android.tazreader.widget.SnapLayoutManager;

public class UserTocFragment extends ReaderBaseFragment {

    CustomToolbar toolbar;
    UserTocAdapter newAdapter;
    RecyclerView      mRecyclerView;
    SnapLayoutManager layoutManager;

    public UserTocFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newAdapter = new UserTocAdapter(new UserTocAdapter.UserTocAdapterClickListener() {
            @Override
            public void onItemClick(int adapterPosition) {
                UserTocItem clickedItem = newAdapter.getItem(adapterPosition);
                if (clickedItem.getIndexItem() instanceof Paper.Plist.Category) {
                    getReaderViewModel().getUserTocLiveData()
                                        .toogleExpantion(clickedItem.getKey());
                    getReaderViewModel().getUserTocLiveData().publish();
                }
                else {
                    getReaderActivity().closeDrawers();
                    getReaderActivity().loadContentFragment(clickedItem.getKey());
                }
            }

            @Override
            public void onBookmarkClick(int adapterPosition) {
                UserTocItem clickedItem = newAdapter.getItem(adapterPosition);
                getReaderActivity().onBookmarkClick(clickedItem.getIndexItem());
                newAdapter.notifyItemChanged(adapterPosition);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.reader_index, container, false);
        toolbar = view.findViewById(R.id.toolbar_article);
        toolbar.setItemColor(ContextCompat.getColor(inflater.getContext(), R.color.toolbar_foreground_color));
        toolbar.inflateMenu(R.menu.reader_index);
        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.toolbar_bookmark_off:
                    setFilterBookmarksToolbarItems(false);
                    break;
                case R.id.toolbar_bookmark_on:
                    setFilterBookmarksToolbarItems(true);
                    break;
                case R.id.toolbar_expand:
                    getReaderViewModel().setExpanded(true);
                    break;
                case R.id.toolbar_collapse:
                    getReaderViewModel().setExpanded(false);
                    break;
                case R.id.toolbar_index_short:
                    getReaderViewModel().setIndexVerbose(false);
                    break;
                case R.id.toolbar_index_full:
                    getReaderViewModel().setIndexVerbose(true);
                    break;
                case R.id.toolbar_index_help:
                    if (getReaderActivity() != null) {
                        getReaderActivity().onShowHelp();
                    }
                    break;
            }
            return true;
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
                        getReaderActivity().closeDrawers();
                        break;
                }
                return true;
            }
        });


        setFilterBookmarksToolbarItems(getReaderViewModel().isFilterBookmarks());

        mRecyclerView = view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        //mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        layoutManager = new SnapLayoutManager(inflater.getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).colorResId(R.color.index_divider)
                                                                                                 .sizeResId(R.dimen.index_divider_size)
                                                                                                 .marginResId(R.dimen.index_divider_margin)
                                                                                                 .showLastDivider()
                                                                                                 .build());
//        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setAdapter(newAdapter);

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getReaderViewModel().getIndexVerboseLiveData()
                            .observe(this, aBoolean -> {
                                newAdapter.setShowSubtitles(aBoolean != null ? aBoolean : true);
                                Menu menu = toolbar.getMenu();
                                MenuItem menuItemFull = menu.findItem(R.id.toolbar_index_full);
                                MenuItem menuItemShort = menu.findItem(R.id.toolbar_index_short);
                                menuItemFull.setVisible(!aBoolean);
                                menuItemShort.setVisible(aBoolean);
                            });
        getReaderViewModel().getUserTocLiveData()
                            .observe(this, userTocItems -> {
                                newAdapter.submitList(userTocItems);
                            });

        getReaderViewModel().getCurrentKeyLiveData()
                            .observe(this, item -> {
                                if (item != null) {
                                    UserTocItem userTocItem;
                                    if (item instanceof Paper.Plist.Page) {
                                        layoutManager.setSnapPreference(SnapLayoutManager.SNAP_TO_START);
                                        userTocItem = getReaderViewModel().getUserTocLiveData()
                                                                          .getUserTocItemForKey(item.getIndexParent()
                                                                                                    .getKey());
                                    } else {
                                        layoutManager.setSnapPreference(SnapLayoutManager.SNAP_TO_CENTER);
                                        userTocItem = getReaderViewModel().getUserTocLiveData()
                                                                          .getUserTocItemForKey(item.getKey());
                                    }
                                    if (userTocItem != null) {
                                        int oldPosition = newAdapter.indexOf(newAdapter.getCurrentItem());
                                        newAdapter.setCurrentItem(userTocItem);
                                        int position = newAdapter.indexOf(userTocItem);
                                        if (oldPosition != -1) newAdapter.notifyItemChanged(oldPosition);
                                        if (position != -1) {
                                            newAdapter.notifyItemChanged(position);
                                            mRecyclerView.postDelayed(new Runnable() {
                                                int pos;

                                                @Override
                                                public void run() {
                                                    mRecyclerView.smoothScrollToPosition(pos);
                                                }

                                                private Runnable setPos(int pos) {
                                                    this.pos = pos;
                                                    return this;
                                                }
                                            }.setPos(position), 1000);
                                        }
                                    }
                                }
                            });
    }

    @Override
    public void onDestroyView() {
        getReaderViewModel().getIndexVerboseLiveData()
                            .removeObservers(this);
        super.onDestroyView();
    }

    public void onBookmarkChange(String key) {
        UserTocItem tocItem = getReaderViewModel().getUserTocLiveData().getUserTocItemForKey(key);
        if (tocItem != null) {
            int pos = newAdapter.indexOf(tocItem);
            if (pos != -1) newAdapter.notifyItemChanged(pos);
        }
    }

    public void setFilterBookmarksToolbarItems(boolean bool) {
        getReaderViewModel().setFilterBookmarks(bool);
        Menu menu = toolbar.getMenu();
        MenuItem menuItemOn = menu.findItem(R.id.toolbar_bookmark_on);
        MenuItem menuItemOff = menu.findItem(R.id.toolbar_bookmark_off);
        menuItemOn.setVisible(!bool);
        menuItemOff.setVisible(bool);
    }
}
