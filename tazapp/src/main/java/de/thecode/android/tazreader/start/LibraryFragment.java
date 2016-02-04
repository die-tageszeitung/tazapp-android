package de.thecode.android.tazreader.start;


import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.melnykov.fab.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.CoverDownloadedEvent;
import de.thecode.android.tazreader.sync.SyncStateChangedEvent;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.widget.AutofitRecyclerView;

/**
 * A simple {@link Fragment} subclass.
 */
public class LibraryFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>, LibraryAdapter.OnItemClickListener, LibraryAdapter.OnItemLongClickListener {
    private static final Logger log = LoggerFactory.getLogger(LibraryFragment.class);
    WeakReference<IStartCallback> callback;
    LibraryAdapter adapter;
    SwipeRefreshLayout swipeRefresh;
    FloatingActionButton fab;

    ActionMode actionMode;

    boolean isSyncing;

    private AutofitRecyclerView recyclerView;

    public LibraryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        log.trace("");
        setHasOptionsMenu(true);

        callback = new WeakReference<>((IStartCallback) getActivity());

        View view = inflater.inflate(R.layout.start_library, container, false);

        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        swipeRefresh.setColorSchemeResources(R.color.color_primary, R.color.color_primary, R.color.color_primary);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                log.debug("");
                if (hasCallback()) getCallback().requestSync(null, null);
            }
        });


        recyclerView = (AutofitRecyclerView) view.findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        //recyclerView.setItemAnimator(new DefaultItemAnimator());
        //recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 5));

        adapter = new LibraryAdapter(getActivity(), null, getCallback());
        adapter.setHasStableIds(true);


        fab = (FloatingActionButton) view.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasCallback()) getCallback().callArchive();
            }
        });
        //fab.attachToRecyclerView(recyclerView);

        showFab();

        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        recyclerView.setAdapter(adapter);

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    AutofitRecyclerView.AutoFitGridLayoutManager lm = ((AutofitRecyclerView) recyclerView).getLayoutManager();
                    int lastCompletlyVisible = lm.findLastCompletelyVisibleItemPosition();
                    int itemCount = lm.getItemCount();
                    if (lastCompletlyVisible == itemCount - 1) {
                        showFab();
                    }
                }

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                boolean isSignificantDelta = Math.abs(dy) > getResources().getDimensionPixelOffset(com.melnykov.fab.R.dimen.fab_scroll_threshold);
                if (isSignificantDelta) {
                    if (dy > 0) {
                        hideFab();
                    } else {
                        showFab();
                    }
                }
            }
        });


        if (hasCallback()) getCallback().onUpdateDrawer(this);
        getLoaderManager().initLoader(0, null, this);


        //ActionMode enabling after view because of theme bugs
        view.post(new Runnable() {
            @Override
            public void run() {
                if (hasCallback()) {

                    if (getCallback().getRetainData()
                                     .isActionMode()) setActionMode();
                }
            }
        });

        setHasOptionsMenu(true);

        return view;
    }

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private IStartCallback getCallback() {
        return callback.get();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.start_library, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ic_action_edit:
                setActionMode();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        log.debug("{}",TazSettings.getPrefBoolean(getActivity(), TazSettings.PREFKEY.FORCESYNC, false));
        if (TazSettings.getPrefBoolean(getActivity(), TazSettings.PREFKEY.FORCESYNC, false)) {

            if (hasCallback()) getCallback().requestSync(null, null);
        }
    }

    @Override
    public void onPause() {
        if (hasCallback()) getCallback().getRetainData()
                                        .removeOpenPaperIdAfterDownload();
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault()
                .registerSticky(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault()
                .unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        log.debug("");
        int firstVisible = recyclerView.findFirstVisibleItemPosition();
        int lastVisible = recyclerView.findLastVisibleItemPosition();
        for (int i = firstVisible; i <= lastVisible; i++) {
            LibraryAdapter.ViewHolder vh = (LibraryAdapter.ViewHolder) recyclerView.findViewHolderForLayoutPosition(i);
            if (vh != null) {
                EventBus.getDefault()
                        .unregister(vh);
            }
        }
        adapter.destroy();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        adapter.removeClickLIstener();
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        log.debug("");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        log.trace("");
        StringBuilder selection = new StringBuilder();
        boolean demo = true;
        if (hasCallback()) {
            demo = !getCallback().getAccountHelper()
                                 .isAuthenticated();
        }

        if (demo) selection.append("(");
        selection.append(Paper.Columns.FULL_VALIDUNTIL)
                 .append(" > ")
                 .append(System.currentTimeMillis() / 1000);

        if (demo) {
            selection.append(" AND ");
            selection.append(Paper.Columns.ISDEMO)
                     .append("=1");
            selection.append(")");
        }
        selection.append(" OR ")
                 .append(Paper.Columns.ISDOWNLOADED)
                 .append("=1");
        selection.append(" OR ")
                 .append(Paper.Columns.DOWNLOADID)
                 .append("!=0");
        selection.append(" OR ")
                 .append(Paper.Columns.IMPORTED)
                 .append("=1");
        selection.append(" OR ")
                 .append(Paper.Columns.HASUPDATE)
                 .append("=1");
        selection.append(" OR ")
                 .append(Paper.Columns.KIOSK)
                 .append("=1");

        return new CursorLoader(getActivity(), Paper.CONTENT_URI, null, selection.toString(), null, Paper.Columns.DATE + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        log.trace("loader: {}, data: {}",loader, data);
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        log.debug("loader: {}",loader);
    }


    public void onEventMainThread(SyncStateChangedEvent event) {
        isSyncing = event.isRunning();
        log.debug("SyncStateChanged running: {}", isSyncing);
        if (swipeRefresh.isRefreshing() != event.isRunning()) swipeRefresh.setRefreshing(event.isRunning());
        if (isSyncing) hideFab();
        else showFab();


    }

    public void onEventMainThread(CoverDownloadedEvent event) {
        try {
            LibraryAdapter.ViewHolder viewHolder = (LibraryAdapter.ViewHolder) recyclerView.findViewHolderForItemId(event.getPaperId());
            if (viewHolder != null) viewHolder.image.setTag(null);
            adapter.notifyItemChanged(adapter.getItemPosition(event.getPaperId()));
        } catch (IllegalStateException e) {
            log.warn("",e);
        }
    }

    public void onEventMainThread(ScrollToPaperEvent event) {
        log.debug("event: {}",event);
        if (recyclerView != null && adapter != null) {
            recyclerView.smoothScrollToPosition(adapter.getItemPosition(event.getPaperId()));
        }
    }

    public void onEventMainThread(DrawerStateChangedEvent event) {
        log.debug("event: {}",event.getNewState());
        if (event.getNewState() == DrawerLayout.STATE_IDLE) swipeRefresh.setEnabled(true);
        else swipeRefresh.setEnabled(false);
    }


    @Override
    public void onItemClick(View v, int position, Paper paper) {
        log.debug("v: {}, position: {}, paper: {}", v, position, paper);
        if (actionMode != null) onItemLongClick(v, position, paper);
        else {
            switch (paper.getState()) {
                case Paper.DOWNLOADED_READABLE:
                case Paper.DOWNLOADED_BUT_UPDATE:
                    //openPlayer(paper.getId());
                    if (hasCallback()) getCallback().openReader(paper.getId());
                    break;
                case Paper.IS_DOWNLOADING:

                    break;

                case Paper.NOT_DOWNLOADED:
                case Paper.NOT_DOWNLOADED_IMPORT:
                    try {
                        if (hasCallback()) getCallback().startDownload(paper.getId());
                    } catch (Paper.PaperNotFoundException e) {
                        log.error("",e);
                    }
                    break;

            }

        }
    }


    @Override
    public boolean onItemLongClick(View v, int position, Paper paper) {
        setActionMode();
        log.debug("v: {}, position: {}, paper: {}",v, position, paper);;
        if (!adapter.isSelected(paper.getId())) selectPaper(paper.getId());
        else deselectPaper(paper.getId());
        return true;
    }


    private void deleteSelected() {
        if (adapter.getSelected() != null && adapter.getSelected()
                                                    .size() > 0) {
            Long[] ids = adapter.getSelected()
                                .toArray(new Long[adapter.getSelected()
                                                         .size()]);
            if (hasCallback()) getCallback().getRetainData()
                                            .deletePaper(ids);
        }
    }

    private void downloadSelected() {
        for (Long paperId : adapter.getSelected()) {
            try {
                Paper paper = new Paper(getActivity(), paperId);
                if (hasCallback()) getCallback().startDownload(paper.getId());
            } catch (Paper.PaperNotFoundException e) {
                log.error("",e);
            }
        }
        adapter.deselectAll();
    }


    private void showFab() {
        if (hasCallback() && getCallback().getAccountHelper()
                                          .isAuthenticated()) {
            if (fab.getVisibility() == View.GONE) {
                fab.setVisibility(View.VISIBLE);
            }
            if (!fab.isVisible() && !isSyncing) fab.show(true);
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    private void hideFab() {
        if (fab.isVisible()) {
            fab.hide(true);
        }
    }


    public void setActionMode() {
        if (actionMode == null) getActivity().startActionMode(new ActionModeCallback());
    }

    public void selectPaper(long paperId) {
        if (adapter != null) adapter.select(paperId);
        if (actionMode != null) actionMode.invalidate();
    }

    public void deselectPaper(long paperId) {
        if (adapter != null) adapter.deselect(paperId);
        if (actionMode != null) actionMode.invalidate();
    }

    class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            log.debug("mode: {}, menu: {}",mode, menu);
            if (hasCallback()) getCallback().getRetainData()
                                            .setActionMode(true);
            if (hasCallback()) getCallback().enableDrawer(false);
            actionMode = mode;
            swipeRefresh.setEnabled(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            log.debug("mode: {}, menu: {}",mode, menu);
            menu.clear();
            int countSelected = adapter.getSelected()
                                       .size();
            mode.setTitle(getActivity().getString(R.string.string_library_selected, countSelected));
            mode.getMenuInflater()
                .inflate(R.menu.start_library_selectmode, menu);
            if (countSelected == 0) {
                menu.findItem(R.id.ic_action_download)
                    .setEnabled(false);
                menu.findItem(R.id.ic_action_delete)
                    .setEnabled(false);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            log.debug("mode: {}, item: {}",mode, item);
            switch (item.getItemId()) {
                case R.id.ic_action_download:
                    downloadSelected();
                    mode.finish();
                    return true;
                case R.id.ic_action_delete:
                    deleteSelected();
                    mode.finish();
                    return true;
                case R.id.ic_action_selectnone:
                    adapter.deselectAll();
                    mode.invalidate();
                    return true;
                case R.id.ic_action_selectall:
                    adapter.selectAll();
                    mode.invalidate();
                    return true;
                case R.id.ic_action_selectinvert:
                    adapter.selectionInvert();
                    mode.invalidate();
                    return true;
                case R.id.ic_action_selectnotloaded:
                    adapter.selectNotLoaded();
                    mode.invalidate();
                    return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            log.debug("mode: {}",mode);
            adapter.deselectAll();
            if (hasCallback()) getCallback().getRetainData()
                                            .setActionMode(false);
            if (hasCallback()) getCallback().enableDrawer(true);
            swipeRefresh.setEnabled(true);
            actionMode = null;
        }

    }

}
