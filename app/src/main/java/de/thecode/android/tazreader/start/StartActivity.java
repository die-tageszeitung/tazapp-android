package de.thecode.android.tazreader.start;


import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.DeleteTask;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialog.ArchiveDialog;
import de.thecode.android.tazreader.dialog.TcDialog;
import de.thecode.android.tazreader.dialog.TcDialogAdapterList;
import de.thecode.android.tazreader.dialog.TcDialogIndeterminateProgress;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.download.NotificationHelper;
import de.thecode.android.tazreader.download.PaperDownloadFailedEvent;
import de.thecode.android.tazreader.download.PaperDownloadFinishedEvent;
import de.thecode.android.tazreader.download.ResourceDownloadEvent;
import de.thecode.android.tazreader.importer.ImportActivity;
import de.thecode.android.tazreader.migration.MigrationActivity;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.sync.SyncHelper;
import de.thecode.android.tazreader.utils.BaseActivity;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.Log;
import de.thecode.android.tazreader.utils.Utils;

/**
 * Created by mate on 27.01.2015.
 */
public class StartActivity extends BaseActivity implements IStartCallback, TcDialog.TcDialogButtonListener, TcDialog.TcDialogDismissListener, TcDialog.TcDialogCancelListener, TcDialogAdapterList.TcDialogAdapterListListener {

    private static final String DIALOG_FIRST = "dialogFirst";
    //private static final String DIALOG_MISSING_RESOURCE = "dialogMissingResource";
    private static final String DIALOG_RESOURCE_DOWNLOADING = "dialogResourceDownloading";
    private static final String DIALOG_NO_CONNECTION = "dialogNoConnection";
    private static final String DIALOG_MIGRATION_FINISHED = "dialogMigrationFinished";
    private static final String DIALOG_DOWNLOAD_MOBILE = "dialogDownloadMobile";
    private static final String DIALOG_ACCOUNT_ERROR = "dialogAccountError";
    private static final String DIALOG_DOWNLOADMANAGER_ERROR = "dialogDownloadManagerError";
    private static final String DIALOG_ARCHIVE_YEAR = "dialogArchiveYear";
    private static final String DIALOG_ARCHIVE_MONTH = "dialogArchiveMonth";
    private static final String DIALOG_WAIT = "dialogWait";

    private static final String ARGUMENT_RESOURCE_KEY = "resourceKey";
    private static final String ARGUMENT_RESOURCE_URL = "resourceUrl";
    private static final String ARGUMENT_MIGRATED_IDS_JSONARRAY = "migratedIds";
    private static final String ARGUMENT_ARCHIVE_YEAR = "archiveYear";

    private Toolbar toolbar;
    private NavigationDrawerFragment mDrawerFragment;

    RetainDataFragment retainDataFragment;

    NavigationDrawerFragment.NavigationItem userItem;
    NavigationDrawerFragment.NavigationItem libraryItem;
    NavigationDrawerFragment.NavigationItem settingsItem;
    NavigationDrawerFragment.NavigationItem helpItem;
    NavigationDrawerFragment.NavigationItem imprintItem;
    NavigationDrawerFragment.NavigationItem importItem;

    AccountHelper mAccountHelper;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault()
                .register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault()
                .unregister(this);
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TazSettings.getPrefInt(this, TazSettings.PREFKEY.PAPERMIGRATEFROM, 0) != 0) {
            Intent migrationIntent = new Intent(this, MigrationActivity.class);
            migrationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(migrationIntent);
            return;
        }
        TazSettings.removePref(this, TazSettings.PREFKEY.PAPERMIGRATEFROM);

        Utils.setActivityOrientationFromPrefs(this);


        retainDataFragment = RetainDataFragment.findOrCreateRetainFragment(getFragmentManager(), this);


        if (retainDataFragment.getCache() == null) {
            final int maxMemory = (int) (Runtime.getRuntime()
                                                .maxMemory() / 1024);
            final int cacheSize = maxMemory / 8;
            retainDataFragment.cache = new LruCache<String, Bitmap>(cacheSize) {

                @SuppressLint("NewApi")
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
        }

        try {
            mAccountHelper = new AccountHelper(this);
        } catch (AccountHelper.CreateAccountException e) {
            showAccountErrorDialog();
        }

        setContentView(R.layout.activity_start);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar()!= null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        updateTitle();

        mDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        mDrawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);

        userItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_account), R.drawable.ic_account, LoginFragment.class);
        importItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_import), R.drawable.ic_file_folder, ImportFragment.class);
        libraryItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_library), R.drawable.ic_library, LibraryFragment.class);
        settingsItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_settings), R.drawable.ic_settings, SettingsFragment.class);
        helpItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_help), R.drawable.ic_help, HelpFragment.class);
        imprintItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_imprint), R.drawable.ic_imprint, ImprintFragment.class);


        mDrawerFragment.addItem(libraryItem);
        mDrawerFragment.addDividerItem();
        mDrawerFragment.addItem(userItem);
        mDrawerFragment.addItem(importItem);
        mDrawerFragment.addItem(helpItem);
        mDrawerFragment.addDividerItem();
        mDrawerFragment.addItem(imprintItem);
        mDrawerFragment.addItem(settingsItem);


        //has to be after adding useritem
        //updateUserInDrawer();

        //initial Fragment with colored navdrawer
        if (getFragmentManager().findFragmentById(R.id.content_frame) == null) mDrawerFragment.simulateClick(libraryItem, false);


        //        showMigrationDownloadDialog();


        if (TazSettings.getPrefBoolean(this, TazSettings.PREFKEY.FISRTSTART, true)) {
            TazSettings.setPref(this, TazSettings.PREFKEY.FISRTSTART, false);
            firstStartDialog();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(requestCode, resultCode, data);
        if (requestCode == ImportActivity.REQUEST_CODE_IMPORT_ACTIVITY) {
            if (resultCode == RESULT_OK) {

            }
        }
    }

    //boolean firstFragmentLoaded = false;

    @Override
    public void loadFragment(NavigationDrawerFragment.NavigationItem item) {
        if (item.getTarget() != null) {
            //noinspection TryWithIdenticalCatches
            try {
                Fragment fragment = item.getTarget()
                                        .newInstance();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, fragment);
                //                if (firstFragmentLoaded) ft.addToBackStack(null);
                //                else firstFragmentLoaded = true;
                //ft.addToBackStack(item.getTarget().getSimpleName());
                ft.commit();
                getRetainData().addToNavBackstack(item);
            } catch (InstantiationException e) {
                Log.e(e);
            } catch (IllegalAccessException e) {
                Log.e(e);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (getRetainData().getNavBackstack()
                           .size() > 1) {
            getRetainData().getNavBackstack()
                           .remove(getRetainData().getNavBackstack()
                                                  .size() - 1);
            NavigationDrawerFragment.NavigationItem backstackItem = getRetainData().getNavBackstack()
                                                                                   .get(getRetainData().getNavBackstack()
                                                                                                       .size() - 1);
            loadFragment(backstackItem);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public RetainDataFragment getRetainData() {
        return retainDataFragment;
    }

    @Override
    public AccountHelper getAccountHelper() {
        return mAccountHelper;
    }

    @Override
    public void loginFinished() {
        //        updateUserInDrawer();
        updateTitle();
        mDrawerFragment.simulateClick(libraryItem, false);
    }

    @Override
    public void logoutFinished() {
        updateTitle();
        //        updateUserInDrawer();
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    //    public void updateUserInDrawer() {
    //        if (mAccountHelper.isAuthenticated()) userItem.setText(mAccountHelper.getUser());
    //        else userItem.setText(getString(R.string.drawer_no_account));
    //        mDrawerFragment.handleChangedItem(userItem);
    //    }

    @Override
    public void enableDrawer(boolean bool) {
        mDrawerFragment.setEnabled(bool);
    }


    @Override
    public void onUpdateDrawer(Fragment fragment) {
        mDrawerFragment.setActive(fragment.getClass());
    }

    @Override
    public void startDownload(long paperId) throws Paper.PaperNotFoundException {
        Paper downloadPaper = new Paper(this,paperId);
        if (!downloadPaper.isDownloading()) {
            switch (Utils.getConnectionType(this)) {
                case Utils.CONNECTION_NOT_AVAILABLE:
                    showNoConnectionDialog();
                    break;
                case Utils.CONNECTION_MOBILE:
                case Utils.CONNECTION_MOBILE_ROAMING:
                    showMobileConnectionDialog();
                    addToDownloadQueue(paperId);
                    break;
                default:
                    addToDownloadQueue(paperId);
                    startDownloadQueue();
            }
        }
        //        //TODO DIALOG
        //        //startDownload(paper.getId());
        //        DownloadHelper mDownloadHelper = new DownloadHelper(this);
        //        try {
        //            mDownloadHelper.enquePaper(paperId);
        //        } catch (IllegalArgumentException e) {
        //            // errorDownloadManagerDialog();
        //        }
    }

    private void addToDownloadQueue(long paperId) {
        retainDataFragment.downloadQueue.add(paperId);
        retainDataFragment.setOpenPaperIdAfterDownload(paperId);
    }

    private void startDownloadQueue() {
        //DownloadHelper mDownloadHelper = new DownloadHelper(this);
        while (retainDataFragment.downloadQueue.size() > 0) {
            long paperId = retainDataFragment.downloadQueue.get(0);
            try {
                DownloadManager.getInstance(this).enquePaper(paperId);
            } catch (IllegalArgumentException | Paper.PaperNotFoundException | AccountHelper.CreateAccountException e) {
                showDownloadManagerErrorDialog();
            } catch (DownloadManager.DownloadNotAllowedException e) {

            }
            retainDataFragment.downloadQueue.remove(0);
        }
    }


    private void firstStartDialog() {
        new TcDialog().withMessage(R.string.dialog_first)
                      .withPositiveButton()
                      .withNeutralButton(R.string.drawer_account)
                      .show(getFragmentManager(), DIALOG_FIRST);
    }

    public void showNoConnectionDialog() {
        new TcDialog().withMessage(R.string.dialog_noconnection)
                      .withPositiveButton()
                      .show(getFragmentManager(), DIALOG_NO_CONNECTION);
    }

    private void showMobileConnectionDialog() {
        if (!retainDataFragment.downloadMobileDialog) {
            retainDataFragment.downloadMobileDialog = true;
            new TcDialog().withMessage(R.string.dialog_mobile_download)
                          .withPositiveButton(R.string.yes)
                          .withNegativeButton()
                          .withNeutralButton(R.string.dialog_wifi)
                          .show(getFragmentManager(), DIALOG_DOWNLOAD_MOBILE);
        }
    }

    private void showAccountErrorDialog() {
        new TcDialog().withMessage(R.string.dialog_account_error)
                      .withPositiveButton()
                      .withCancelable(false)
                      .show(getFragmentManager(), DIALOG_ACCOUNT_ERROR);
    }

    private void showDownloadManagerErrorDialog() {
        new TcDialog().withMessage(R.string.dialog_downloadmanager_error)
                      .withPositiveButton()
                      .show(getFragmentManager(), DIALOG_DOWNLOADMANAGER_ERROR);
    }


    private void showArchiveYearPicker() {
        Calendar cal = Calendar.getInstance();
        ArrayList<ArchiveDialog.ArchiveEntry> years = new ArrayList<>();
        for (int year = cal.get(Calendar.YEAR); year >= 2011; year--) {
            years.add(new ArchiveDialog.ArchiveEntry(year));
        }


        new ArchiveDialog().withEntries(years)
                           .withTitle(R.string.dialog_archive_year_title)
                           .withCancelable(true)
                           .show(getFragmentManager(), DIALOG_ARCHIVE_YEAR);
    }

    private void showArchiveMonthPicker(int year) {
        Log.d(year);
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int maxMonth = 11;
        if (year == currentYear) {
            maxMonth = cal.get(Calendar.MONTH);
        }
        ArrayList<ArchiveDialog.ArchiveEntry> months = new ArrayList<>();
        for (int i = maxMonth; i >= 0; i--) {
            months.add(new ArchiveDialog.ArchiveEntry(i, new DateFormatSymbols().getMonths()[i]));
        }

        Bundle bundle = new Bundle();
        bundle.putInt(ARGUMENT_ARCHIVE_YEAR, year);

        new ArchiveDialog().withEntries(months)
                           .withTitle(R.string.dialog_archive_month_title)
                           .withCancelable(true)
                           .withBundle(bundle)
                           .show(getFragmentManager(), DIALOG_ARCHIVE_MONTH);
    }

    private void showDownloadErrorDialog(long paperId, Exception exception) {
        try {
            Paper paper = new Paper(this, paperId);
            StringBuilder message = new StringBuilder(String.format(getString(R.string.dialog_error_download), paper.getTitelWithDate(this)));
            if (exception != null && BuildConfig.DEBUG) message.append("\n\n")
                                                               .append(exception);

            new TcDialog().withMessage(message.toString())
                          .withPositiveButton()
                          .show(getFragmentManager(), DIALOG_DOWNLOADMANAGER_ERROR);
        } catch (Paper.PaperNotFoundException e) {
            Log.e(e);
        }
    }



    @Override
    public void toggleWaitDialog(String tag) {
        DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(tag);
        if (dialog == null)
            new TcDialogIndeterminateProgress().withCancelable(false)
                                               .withMessage("Bitte warten...")
                                               .show(getFragmentManager(), tag);
        else
            dialog.dismiss();
    }

    @Override
    public void callArchive() {
        showArchiveYearPicker();
    }

    @Override
    public void openReader(long id) {

        Paper openPaper;
        try {
            openPaper = new Paper(this, id);
            Resource paperResource = new Resource(this, openPaper.getResource());

            if (paperResource.isDownloaded()) {
                Log.t("Start reader for paper:", openPaper);
                Intent intent = new Intent(this, ReaderActivity.class);
                intent.putExtra(ReaderActivity.KEY_EXTRA_PAPER_ID, id);
                startActivity(intent);
            } else {
                switch (Utils.getConnectionType(this)) {
                    case Utils.CONNECTION_NOT_AVAILABLE:
                        showNoConnectionDialog();
                        break;
                    default:
                        toggleWaitDialog(DIALOG_WAIT + openPaper.getBookId());
                        //DownloadHelper downloadHelper = new DownloadHelper(this);
                        DownloadManager.getInstance(this).enqueResource(openPaper);
                        retainDataFragment.openPaperWaitingForRessource = id;
                }
            }
        } catch (Paper.PaperNotFoundException e) {
            Log.e(e);
        }
    }

    public void updateTitle() {
        StringBuilder titleBuilder = new StringBuilder(getString(getApplicationInfo().labelRes));
        if (!mAccountHelper.isAuthenticated()) {
            titleBuilder.append(" ").append(getString(R.string.demo_titel_appendix));
        }
        if (getSupportActionBar()!= null) {
            getSupportActionBar().setTitle(titleBuilder.toString());
        }
    }

    @Override
    public void requestSync(Calendar start, Calendar end) {
        if (start != null && end != null) {
            SyncHelper.requestManualSync(this, start, end);
            return;
        }
        SyncHelper.requestManualSync(this);
    }

    public void onEventMainThread(PaperDownloadFinishedEvent event) {
        Log.d(event);
        if (retainDataFragment.useOpenPaperafterDownload) {
            if (event.getPaperId() == retainDataFragment.openPaperIdAfterDownload) {
                openReader(retainDataFragment.openPaperIdAfterDownload);
            }
        }
    }

    public void onEventMainThread(PaperDownloadFailedEvent event) {
        showDownloadErrorDialog(event.getPaperId(), event.getException());
        NotificationHelper.cancelDownloadErrorNotification(this, event.getPaperId());
    }

    public void onEventMainThread(ResourceDownloadEvent event) {
        if (retainDataFragment.openPaperWaitingForRessource != -1) {
            try {
                Paper waitingPaper = new Paper(this, retainDataFragment.openPaperWaitingForRessource);
                if (event.getKey()
                         .equals(waitingPaper.getResource())) {
                    toggleWaitDialog(DIALOG_WAIT + waitingPaper.getBookId());
                    long paperId = retainDataFragment.openPaperWaitingForRessource;
                    retainDataFragment.openPaperWaitingForRessource = -1;
                    if (event.isSuccessful()) {
                        openReader(paperId);
                    }
                }
            } catch (Paper.PaperNotFoundException e) {
                retainDataFragment.openPaperWaitingForRessource = -1;
            }
        }
    }


    @Override
    public void onDialogClick(String tag, Bundle arguments, int which) {
        if (DIALOG_FIRST.equals(tag)) {
            if (which == TcDialog.BUTTON_NEUTRAL) mDrawerFragment.simulateClick(userItem, true);
        } else if (DIALOG_MIGRATION_FINISHED.equals(tag)) {
            if (which == TcDialog.BUTTON_POSITIVE) {
                try {
                    JSONArray json = new JSONArray(arguments.getString(ARGUMENT_MIGRATED_IDS_JSONARRAY));
                    for (int i = 0; i < json.length(); i++) {
                        startDownload(json.getLong(i));
                    }
                } catch (JSONException | Paper.PaperNotFoundException e) {
                    Log.w(e);
                }

            }
        } else if (DIALOG_DOWNLOAD_MOBILE.equals(tag)) {
            switch (which) {
                case TcDialog.BUTTON_POSITIVE:
                    startDownloadQueue();
                    break;
                case TcDialog.BUTTON_NEGATIVE:
                    retainDataFragment.downloadQueue.clear();
                    break;
                case TcDialog.BUTTON_NEUTRAL:
                    retainDataFragment.downloadQueue.clear();
                    startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                    break;
            }
        } else if (DIALOG_ACCOUNT_ERROR.equals(tag)) {
            finish();
        }
    }


    @Override
    public void onDialogAdapterListClick(String tag, TcDialogAdapterList.TcDialogAdapterListEntry entry, Bundle arguments) {
        if (DIALOG_ARCHIVE_YEAR.equals(tag)) {
            showArchiveMonthPicker(((ArchiveDialog.ArchiveEntry) entry).getNumber());
        } else if (DIALOG_ARCHIVE_MONTH.equals(tag)) {
            int year = arguments.getInt(ARGUMENT_ARCHIVE_YEAR);
            int month = ((ArchiveDialog.ArchiveEntry) entry).getNumber();
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.set(year, month, 1);
            int lastDayOfMont = startCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            endCal.set(year, month, lastDayOfMont);
            requestSync(startCal, endCal);
        }
    }

    @Override
    public void onDialogDismiss(String tag, Bundle arguments) {
        if (DIALOG_DOWNLOAD_MOBILE.equals(tag)) {
            retainDataFragment.downloadMobileDialog = false;
        }
    }

    @Override
    public void onDialogCancel(String tag, Bundle arguments) {
        if (DIALOG_DOWNLOAD_MOBILE.equals(tag)) {
            retainDataFragment.downloadQueue.clear();
        }
    }




    public static class RetainDataFragment extends BaseFragment {

        private static final String TAG = "RetainDataFragment";
        public LruCache<String, Bitmap> cache;
        public List<Long> selectedInLibrary = new ArrayList<>();
        boolean actionMode;
        List<Long> downloadQueue = new ArrayList<>();
        boolean downloadMobileDialog = false;
        private long openPaperIdAfterDownload = -1;
        private long openPaperWaitingForRessource = -1;
        private boolean useOpenPaperafterDownload = true;
        List<NavigationDrawerFragment.NavigationItem> navBackstack = new ArrayList<>();
        IStartCallback callback;

        public RetainDataFragment() {
        }

        public static RetainDataFragment findOrCreateRetainFragment(FragmentManager fm, IStartCallback callback) {
            RetainDataFragment fragment = (RetainDataFragment) fm.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = new RetainDataFragment();
                fm.beginTransaction()
                  .add(fragment, TAG)
                  .commit();
            }
            fragment.callback = callback;
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        public LruCache<String, Bitmap> getCache() {
            return cache;
        }

        public List<Long> getSelectedInLibrary() {
            return selectedInLibrary;
        }

        public boolean isActionMode() {
            return actionMode;
        }

        public void setActionMode(boolean actionMode) {
            this.actionMode = actionMode;
        }


        public void setOpenPaperIdAfterDownload(long paperId) {
            if (openPaperIdAfterDownload == -1) {
                openPaperIdAfterDownload = paperId;
                useOpenPaperafterDownload = true;
            } else {
                useOpenPaperafterDownload = false;
            }
        }

        public void removeOpenPaperIdAfterDownload() {
            openPaperIdAfterDownload = -1;
            useOpenPaperafterDownload = true;
        }

        public void addToNavBackstack(NavigationDrawerFragment.NavigationItem item) {
            if (navBackstack.contains(item)) navBackstack.remove(item);
            navBackstack.add(item);
        }

        public List<NavigationDrawerFragment.NavigationItem> getNavBackstack() {
            return navBackstack;
        }

        public void deletePaper(Long... ids) {
            callback.toggleWaitDialog(DIALOG_WAIT+"delete");
            new DeleteTask(getActivity()){

                @Override
                protected void onPostError(Exception exception) {
                    Log.e(exception);
                    callback.toggleWaitDialog(DIALOG_WAIT+"delete");
                }

                @Override
                protected void onPostSuccess(Void aVoid) {
                    callback.toggleWaitDialog(DIALOG_WAIT+"delete");
                }
            }.execute(ids);
        }
    }
}
