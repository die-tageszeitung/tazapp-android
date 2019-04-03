package de.thecode.android.tazreader.start.library;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperWithDownloadState;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialognew.DownloadInfoDialog;
import de.thecode.android.tazreader.start.DrawerStateChangedEvent;
import de.thecode.android.tazreader.start.StartBaseFragment;
import de.thecode.android.tazreader.start.StartViewModel;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.extendedasyncdiffer.ExtendedAdapterListUpdateCallback;
import de.thecode.android.tazreader.widget.AutofitRecyclerView;
import de.thecode.android.tazreader.worker.SyncWorker;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 */
public class LibraryFragment extends StartBaseFragment {
    //    LibraryAdapter                adapter;
    NewLibraryAdapter  adapter;
    SwipeRefreshLayout swipeRefresh;

    ActionMode actionMode;

    boolean isSyncing;

    private AutofitRecyclerView  recyclerView;
    private FloatingActionButton fabArchive;

    private StartViewModel startViewModel;

    private TazSettings.OnPreferenceChangeListener demoModeChangedListener = new TazSettings.OnPreferenceChangeListener<Boolean>() {
        @Override
        public void onPreferenceChanged(Boolean value) {
            onDemoModeChanged(value);
        }
    };

    public LibraryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startViewModel = ViewModelProviders.of(getActivity())
                                           .get(StartViewModel.class);

        adapter = new NewLibraryAdapter(startViewModel.getPaperMetaDataMap(), new NewLibraryAdapter.OnItemClickListener() {
            @Override
            public void onClick(PaperWithDownloadState paper, int position) {

                Timber.d("position: %s, paper: %s", position, paper);
                if (actionMode != null) {
                    adapter.toggleSelection(paper, position);
                    //onLongClick(paper, position);
                } else {

                    //TODO CHECK STATES
                    switch (paper.getDownloadState()) {
                        case READY:
                            getStartActivity().openReader(paper.getBookId());
                            break;
                        case NONE:
                            getStartActivity().startDownload(paper);
                            break;
                        default:


                            if (getFragmentManager() != null) {
                                DownloadInfoDialog.Companion.newInstance(paper).show(getFragmentManager(), DownloadInfoDialog.DIALOG_TAG);
                            }
                            break;
                    }

                }

            }

//            @Override
//            public void onLongClick(Paper libraryPaper, int position) {
////                startViewModel.getLibraryPaperLiveData()
////                              .toggleSelection(libraryPaper.getBookId());
//                setActionMode();
//            }

            @Override
            public void onSelectionChanged() {
                if (actionMode != null) {
                    if (adapter.getPaperMetaData()
                               .getSelectedCount() == 0) actionMode.finish();
                    else actionMode.invalidate();
                } else {
                    setActionMode();
                }
            }
        }, new ExtendedAdapterListUpdateCallback.OnFirstInsertedListener() {
            @Override
            public void onFinished(int firstInserted) {

                try {
                    recyclerView.smoothScrollToPosition(firstInserted);
                } catch (Exception e) {
                    Timber.w(e, "Can't scroll, don't worry!");
                }
            }
        });

        startViewModel.getLivePapers()
                      .observe(this, new Observer<List<PaperWithDownloadState>>() {
                          @Override
                          public void onChanged(@Nullable List<PaperWithDownloadState> libraryPapers) {
//                              if (actionMode != null) {
//                                  if (startViewModel.getLibraryPaperLiveData()
//                                                    .getSelectionSize() == 0) actionMode.finish();
//                                  else
//                                      actionMode.invalidate();
//                              }
                              adapter.submitList(libraryPapers);
                          }
                      });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.start_library, container, false);

        swipeRefresh = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefresh.setColorSchemeResources(R.color.refresh_color_1, R.color.refresh_color_2);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                hideFab();
                SyncWorker.scheduleJobImmediately(true);
                //SyncHelper.requestSync(getContext());
            }
        });


        recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

//        adapter = new LibraryAdapter(getActivity(), null, getCallback());
//        adapter.setHasStableIds(true);

        fabArchive = view.findViewById(R.id.fabArchive);
        fabArchive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStartActivity().callArchive();
            }
        });

        showFab();

//        adapter.setOnItemClickListener(this);
//        adapter.setOnItemLongClickListener(this);
//        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
        });


        getStartActivity().onUpdateDrawer(this);
        //getLoaderManager().initLoader(0, null, this);


        //ActionMode enabling after view because of theme bugs
        view.post(new Runnable() {
            @Override
            public void run() {
                if (startViewModel.isActionMode()) setActionMode();
            }
        });

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WorkManager.getInstance().getWorkInfosByTagLiveData(SyncWorker.TAG)
                   .observe(this, new Observer<List<WorkInfo>>() {
                       @Override
                       public void onChanged(@Nullable List<WorkInfo> workStatuses) {
                           boolean isSyncRunning = false;
                           if (workStatuses != null) {
                               for (WorkInfo workStatus : workStatuses) {
                                   //Timber.i("%s",workStatus);
                                   isSyncRunning = workStatus.getState() == WorkInfo.State.RUNNING;
                                   if (isSyncRunning) break;
                               }
                               if (isSyncRunning != swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(isSyncRunning);
                               if (isSyncRunning) hideFab();
                               else showFab();
                           }
                       }
                   });
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
//        Timber.d("%s", TazSettings.getInstance(getActivity())
//                                   .getPrefBoolean(TazSettings.PREFKEY.FORCESYNC, false));
//        if (TazSettings.getInstance(getActivity())
//                       .getPrefBoolean(TazSettings.PREFKEY.FORCESYNC, false)) {
//
//            SyncHelper.requestSync(getContext());
//        }
    }

    @Override
    public void onPause() {
        startViewModel.removeOpenPaperIdAfterDownload();
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        TazSettings.getInstance(getContext())
                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.DEMOMODE, demoModeChangedListener);
        EventBus.getDefault()
                .register(this);
    }

    @Override
    public void onStop() {
        TazSettings.getInstance(getContext())
                   .removeOnPreferenceChangeListener(demoModeChangedListener);
        EventBus.getDefault()
                .unregister(this);
        super.onStop();
    }

    private void onDemoModeChanged(boolean demoMode) {
        showFab();
        //getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onDestroyView() {

//        int firstVisible = recyclerView.findFirstVisibleItemPosition();
//        int lastVisible = recyclerView.findLastVisibleItemPosition();
//        for (int i = firstVisible; i <= lastVisible; i++) {
//            LibraryAdapter.ViewHolder vh = (LibraryAdapter.ViewHolder) recyclerView.findViewHolderForLayoutPosition(i);
//            if (vh != null) {
//                EventBus.getDefault()
//                        .unregister(vh);
//            }
//        }
//        adapter.destroy();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
//        adapter.removeClickLIstener();
        startViewModel.setPaperMetaDataMap(adapter.getPaperMetaData());
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
    }


//    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
//    public void onSyncStateChanged(SyncStateChangedEvent event) {
//        isSyncing = event.isRunning();
//        Timber.d("SyncStateChanged running: %s", isSyncing);
//        if (swipeRefresh.isRefreshing() != event.isRunning()) swipeRefresh.setRefreshing(event.isRunning());
//        if (isSyncing) hideFab();
//        else showFab();
//    }
//
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onCoverDowloaded(CoverDownloadedEvent event) {
//        //TODO onCoverDownloaded
////        try {
////            LibraryAdapter.ViewHolder viewHolder = (LibraryAdapter.ViewHolder) recyclerView.findViewHolderForItemId(event.getPaperId());
////            if (viewHolder != null) viewHolder.image.setTag(null);
//////            adapter.notifyItemChanged(adapter.getItemPosition(event.getPaperId()));
////        } catch (IllegalStateException e) {
////            Timber.w(e);
////        }
//    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDrawerStateChanged(DrawerStateChangedEvent event) {
        Timber.d("event: %s", event.getNewState());
        if (event.getNewState() == DrawerLayout.STATE_IDLE) swipeRefresh.setEnabled(true);
        else swipeRefresh.setEnabled(false);
    }

    private void downloadSelected() {

        new AsyncTaskListener<String, Void>(bookIdsParam -> {
            List<Paper> papersToDownload = startViewModel.getPaperRepository()
                                                         .getPapersWithBookId(bookIdsParam);
            for (Paper paperToDownload : papersToDownload) {
                getStartActivity().startDownload(paperToDownload);
            }
            return null;
        }).execute(adapter.getPaperMetaData()
                          .getSelected());
//        new AsyncTaskListener<String, List<Paper>>(bookIds -> startViewModel.getPaperRepository()
//                                                                            .getPapersWithBookId(bookIds), papers -> {
//            if (papers != null) {
//                for (Paper paper : papers) {
//
//                }
//            }
//        }).execute(startViewModel.getLibraryPaperLiveData()
//                                 .getSelected()
//                                 .toArray(new String[startViewModel.getLibraryPaperLiveData()
//                                                                   .getSelected()
//                                                                   .size()]));

    }


    private void showFab() {
        if (!TazSettings.getInstance(getContext())
                        .isDemoMode()) {
            if (!isSyncing) {
                fabArchive.show();
            }
        } else {
            hideFab();
        }
    }

    private void hideFab() {
        fabArchive.hide();
    }


    public void setActionMode() {
        if (actionMode == null && getActivity() != null) getActivity().startActionMode(new ActionModeCallback());
    }

    class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("mode: %s, menu: %s", mode, menu);
            startViewModel.setActionMode(true);
            getStartActivity().enableDrawer(false);
            actionMode = mode;
            swipeRefresh.setEnabled(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Timber.d("mode: %s, menu: %s", mode, menu);
            menu.clear();
            int countSelected = adapter.getPaperMetaData()
                                       .getSelectedCount();
            mode.setTitle(getString(R.string.string_library_selected, countSelected));
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
            Timber.d("mode: %s, item: %s", mode, item);
            switch (item.getItemId()) {
                case R.id.ic_action_download:
                    downloadSelected();
                    mode.finish();
                    return true;
                case R.id.ic_action_delete:
                    startViewModel.deletePaper(adapter.getPaperMetaData()
                                                      .getSelected());
                    mode.finish();
                    return true;
                case R.id.ic_action_selectnone:
                    mode.finish();
                    return true;
                case R.id.ic_action_selectall:
                    adapter.selectAll();
//                    startViewModel.getLibraryPaperLiveData()
//                                  .selectAll();
                    return true;
                case R.id.ic_action_selectinvert:
                    adapter.selectionInverse();
//                    startViewModel.getLibraryPaperLiveData()
//                                  .invertSelection();
                    return true;
                case R.id.ic_action_selectnotloaded:
                    adapter.selectNotDownloadedPapers();
//                    startViewModel.getLibraryPaperLiveData()
//                                  .selectNotDownloaded();
                    return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
//            adapter.getPaperMetaData().
//            startViewModel.getLibraryPaperLiveData()
//                          .selectNone();
            adapter.selectNone();
            Timber.d("mode: %s", mode);
//            adapter.deselectAll();
            startViewModel.setActionMode(false);
            getStartActivity().enableDrawer(true);
            swipeRefresh.setEnabled(true);
            actionMode = null;
        }

    }

}
