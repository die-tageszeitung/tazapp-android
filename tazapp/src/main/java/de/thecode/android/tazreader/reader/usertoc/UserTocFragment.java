package de.thecode.android.tazreader.reader.usertoc;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
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

    CustomToolbar     toolbar;
    UserTocAdapter    newAdapter;
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
                                        .setExpanded(clickedItem.getKey(), !clickedItem.areChildsVisible());
                } else {
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

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

        CustomToolbar toolbar2 = view.findViewById(R.id.toolbar2);
        toolbar2.setItemColor(ContextCompat.getColor(inflater.getContext(), R.color.toolbar_foreground_color));
        toolbar2.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
        toolbar2.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(getActivity()));
        toolbar2.inflateMenu(R.menu.reader_index_main);
        toolbar2.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.toolbar_settings:
                        new SettingsDialog.Builder().setPositiveButton()
                                                    .setPadding(0)
                                                    .buildSupport()
                                                    .show(getFragmentManager(), ReaderActivity.TAG_FRAGMENT_DIALOG_SETTING);
                        getReaderActivity().closeDrawers();
                        break;
                }
                return true;
        });


        setFilterBookmarksToolbarItems(getReaderViewModel().getUserTocLiveData()
                                                           .isFilterBookmarks());

        mRecyclerView = view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        layoutManager = new SnapLayoutManager(inflater.getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).colorResId(R.color.index_divider)
                                                                                                 .sizeResId(R.dimen.index_divider_size)
                                                                                                 .marginResId(R.dimen.index_divider_margin)
                                                                                                 .showLastDivider()
                                                                                                 .build());
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
                            .observe(this, userTocResultWrapper -> {
                                if (userTocResultWrapper != null) {
                                    newAdapter.submitList(userTocResultWrapper.getList());
                                    if (userTocResultWrapper.getScrollToPosition() != -1) {
                                        layoutManager.setSnapPreference(userTocResultWrapper.isCenterScroll() ? SnapLayoutManager.SNAP_TO_CENTER : SnapLayoutManager.SNAP_TO_START);
                                        mRecyclerView.smoothScrollToPosition(userTocResultWrapper.getScrollToPosition());
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

    private void setFilterBookmarksToolbarItems(boolean bool) {
        getReaderViewModel().getUserTocLiveData()
                            .setFilterBookmarks(bool);
        Menu menu = toolbar.getMenu();
        MenuItem menuItemOn = menu.findItem(R.id.toolbar_bookmark_on);
        MenuItem menuItemOff = menu.findItem(R.id.toolbar_bookmark_off);
        menuItemOn.setVisible(!bool);
        menuItemOff.setVisible(bool);
    }
}
