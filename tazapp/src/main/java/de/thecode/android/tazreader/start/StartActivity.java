package de.thecode.android.tazreader.start;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.LruCache;

import de.greenrobot.event.EventBus;
import de.mateware.dialog.Dialog;
import de.mateware.dialog.DialogAdapterList;
import de.mateware.dialog.DialogIndeterminateProgress;
import de.mateware.dialog.listener.DialogAdapterListListener;
import de.mateware.dialog.listener.DialogButtonListener;
import de.mateware.dialog.listener.DialogCancelListener;
import de.mateware.dialog.listener.DialogDismissListener;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.DeleteTask;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialog.ArchiveDialog;
import de.thecode.android.tazreader.dialog.ArchiveEntry;
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
import de.thecode.android.tazreader.utils.Connection;
import de.thecode.android.tazreader.utils.Orientation;
import de.thecode.android.tazreader.widget.CustomToolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by mate on 27.01.2015.
 */
public class StartActivity extends BaseActivity
        implements IStartCallback, DialogButtonListener, DialogDismissListener, DialogCancelListener, DialogAdapterListListener {

    private static final Logger log = LoggerFactory.getLogger(StartActivity.class);

    private static final String DIALOG_FIRST                 = "dialogFirst";
    private static final String DIALOG_USER_REENTER          = "dialogUserReenter";
    //private static final String DIALOG_MISSING_RESOURCE = "dialogMissingResource";
    private static final String DIALOG_RESOURCE_DOWNLOADING  = "dialogResourceDownloading";
    private static final String DIALOG_NO_CONNECTION         = "dialogNoConnection";
    private static final String DIALOG_MIGRATION_FINISHED    = "dialogMigrationFinished";
    private static final String DIALOG_DOWNLOAD_MOBILE       = "dialogDownloadMobile";
    private static final String DIALOG_ACCOUNT_ERROR         = "dialogAccountError";
    private static final String DIALOG_DOWNLOADMANAGER_ERROR = "dialogDownloadManagerError";
    private static final String DIALOG_DOWNLOAD_ERROR        = "dialogDownloadManagerError";
    private static final String DIALOG_ARCHIVE_YEAR          = "dialogArchiveYear";
    private static final String DIALOG_ARCHIVE_MONTH         = "dialogArchiveMonth";
    private static final String DIALOG_WAIT                  = "dialogWait";

    private static final String ARGUMENT_RESOURCE_KEY           = "resourceKey";
    private static final String ARGUMENT_RESOURCE_URL           = "resourceUrl";
    private static final String ARGUMENT_MIGRATED_IDS_JSONARRAY = "migratedIds";
    private static final String ARGUMENT_ARCHIVE_YEAR           = "archiveYear";

    private CustomToolbar            toolbar;
    private NavigationDrawerFragment mDrawerFragment;

    RetainDataFragment retainDataFragment;

    NavigationDrawerFragment.NavigationItem userItem;
    NavigationDrawerFragment.NavigationItem libraryItem;
    NavigationDrawerFragment.NavigationItem settingsItem;
    NavigationDrawerFragment.NavigationItem helpItem;
    NavigationDrawerFragment.NavigationItem imprintItem;
    NavigationDrawerFragment.NavigationItem importItem;

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

        if (TazSettings.getInstance(this)
                       .getPrefInt(TazSettings.PREFKEY.PAPERMIGRATEFROM, 0) != 0) {
            Intent migrationIntent = new Intent(this, MigrationActivity.class);
            migrationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(migrationIntent);
            return;
        }
        TazSettings.getInstance(this)
                   .removePref(TazSettings.PREFKEY.PAPERMIGRATEFROM);

        Orientation.setActivityOrientationFromPrefs(this);


        retainDataFragment = RetainDataFragment.findOrCreateRetainFragment(getSupportFragmentManager(), this);


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

        setContentView(R.layout.activity_start);

        toolbar = (CustomToolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setItemColor(ContextCompat.getColor(this, R.color.toolbar_foreground_color));

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        updateTitle();

        mDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(
                R.id.fragment_navigation_drawer);
        mDrawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);

        userItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_account), R.drawable.ic_account,
                                                               LoginFragment.class);
        importItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_import), R.drawable.ic_file_folder,
                                                                 ImportFragment.class);
        importItem.setAccessibilty(false);
        libraryItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_library), R.drawable.ic_library,
                                                                  LibraryFragment.class);
        settingsItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_settings), R.drawable.ic_settings,
                                                                   SettingsFragment.class);
        settingsItem.setAccessibilty(false);
        helpItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_help), R.drawable.ic_help,
                                                               HelpFragment.class);
        helpItem.setAccessibilty(false);
        imprintItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_imprint), R.drawable.ic_imprint,
                                                                  ImprintFragment.class);


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
        if (getSupportFragmentManager().findFragmentById(R.id.content_frame) == null)
            mDrawerFragment.simulateClick(libraryItem, false);


        //        showMigrationDownloadDialog();


        if (TazSettings.getInstance(this)
                       .getPrefBoolean(TazSettings.PREFKEY.FISRTSTART, true)) {
            TazSettings.getInstance(this)
                       .setPref(TazSettings.PREFKEY.FISRTSTART, false);
            TazSettings.getInstance(this)
                       .setPref(TazSettings.PREFKEY.USERMIGRATIONNOTIFICATION, true);
            firstStartDialog();
        } else if (!TazSettings.getInstance(this)
                               .getPrefBoolean(TazSettings.PREFKEY.USERMIGRATIONNOTIFICATION, false)) {
            new Dialog.Builder().setCancelable(false)
                                .setMessage(R.string.dialog_user_reenter)
                                .setPositiveButton(R.string.dialog_understood)
                                .buildSupport()
                                .show(getSupportFragmentManager(), DIALOG_USER_REENTER);
        }

        SyncHelper.requestSync(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        log.debug("intent: {}", intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        log.debug("requestCode: {}, resultCode: {}, data: {}", requestCode, resultCode, data);
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
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, fragment);
                //                if (firstFragmentLoaded) ft.addToBackStack(null);
                //                else firstFragmentLoaded = true;
                //ft.addToBackStack(item.getTarget().getSimpleName());
                ft.commit();
                getRetainData().addToNavBackstack(item);
            } catch (InstantiationException e) {
                log.error("", e);
            } catch (IllegalAccessException e) {
                log.error("", e);
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
        Paper downloadPaper = new Paper(this, paperId);
        if (!downloadPaper.isDownloading()) {
            switch (Connection.getConnectionType(this)) {
                case Connection.CONNECTION_NOT_AVAILABLE:
                    showNoConnectionDialog();
                    break;
                case Connection.CONNECTION_MOBILE:
                case Connection.CONNECTION_MOBILE_ROAMING:
                    addToDownloadQueue(paperId);
                    if (retainDataFragment.allowMobileDownload) startDownloadQueue();
                    else showMobileConnectionDialog();
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
                Paper paper = new Paper(this, paperId);
                try {
                    DownloadManager.getInstance(this)
                                   .enquePaper(paperId);
                } catch (IllegalArgumentException e) {
                    showDownloadManagerErrorDialog();
                } catch (DownloadManager.DownloadNotAllowedException e) {
                    showDownloadErrorDialog(paper.getTitelWithDate(this), getString(R.string.message_download_not_allowed), e);
                } catch (DownloadManager.NotEnoughSpaceException e) {
                    showDownloadErrorDialog(paper.getTitelWithDate(this), getString(R.string.message_not_enough_space), e);
                }
            } catch (Paper.PaperNotFoundException e) {
                showDownloadErrorDialog(String.valueOf(paperId), getString(R.string.message_paper_not_found), e);
            }


            retainDataFragment.downloadQueue.remove(0);
        }
    }


    private void firstStartDialog() {
        new Dialog.Builder().setMessage(R.string.dialog_first)
                            .setPositiveButton()
                            .setNeutralButton(R.string.drawer_account)
                            .buildSupport()
                            .show(getSupportFragmentManager(), DIALOG_FIRST);
    }

    public void showNoConnectionDialog() {
        new Dialog.Builder().setMessage(R.string.dialog_noconnection)
                            .setPositiveButton()
                            .buildSupport()
                            .show(getSupportFragmentManager(), DIALOG_NO_CONNECTION);
    }

    private void showMobileConnectionDialog() {

        new Dialog.Builder().setMessage(R.string.dialog_mobile_download)
                            .setPositiveButton(R.string.yes)
                            .setNegativeButton()
                            .setNeutralButton(R.string.dialog_wifi)
                            .buildSupport()
                            .show(getSupportFragmentManager(), DIALOG_DOWNLOAD_MOBILE);
    }

    private void showAccountErrorDialog() {
        new Dialog.Builder().setMessage(R.string.dialog_account_error)
                            .setPositiveButton()
                            .setCancelable(false)
                            .buildSupport()
                            .show(getSupportFragmentManager(), DIALOG_ACCOUNT_ERROR);
    }

    private void showDownloadManagerErrorDialog() {
        new Dialog.Builder().setMessage(R.string.dialog_downloadmanager_error)
                            .setPositiveButton()
                            .buildSupport()
                            .show(getSupportFragmentManager(), DIALOG_DOWNLOADMANAGER_ERROR);
    }

    private void showArchiveYearPicker() {
        Calendar cal = Calendar.getInstance();
        ArrayList<ArchiveEntry> years = new ArrayList<>();
        for (int year = cal.get(Calendar.YEAR); year >= 2011; year--) {
            years.add(new ArchiveEntry(year));
        }


        new ArchiveDialog.Builder().setEntries(years)
                                   .setTitle(R.string.dialog_archive_year_title)
                                   .setCancelable(true)
                                   .buildSupport()
                                   .show(getSupportFragmentManager(), DIALOG_ARCHIVE_YEAR);
    }

    private void showArchiveMonthPicker(int year) {
        log.debug("year: {}", year);
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int maxMonth = 11;
        if (year == currentYear) {
            maxMonth = cal.get(Calendar.MONTH);
        }
        ArrayList<ArchiveEntry> months = new ArrayList<>();
        for (int i = maxMonth; i >= 0; i--) {
            months.add(new ArchiveEntry(i, new DateFormatSymbols().getMonths()[i]));
        }

        Bundle bundle = new Bundle();
        bundle.putInt(ARGUMENT_ARCHIVE_YEAR, year);

        new ArchiveDialog.Builder().setEntries(months)
                                   .setTitle(R.string.dialog_archive_month_title)
                                   .setCancelable(true)
                                   .addBundle(bundle)
                                   .buildSupport()
                                   .show(getSupportFragmentManager(), DIALOG_ARCHIVE_MONTH);
    }

    private void showDownloadErrorDialog(String downloadTitle, String extraMessage, Exception exception) {
        StringBuilder message = new StringBuilder(String.format(getString(R.string.dialog_error_download), downloadTitle));
        if (!TextUtils.isEmpty(extraMessage)) message.append("\n\n")
                                                     .append(extraMessage);
        if (exception != null && BuildConfig.DEBUG) message.append("\n\n")
                                                           .append(exception);

        new Dialog.Builder().setMessage(message.toString())
                            .setPositiveButton()
                            .buildSupport()
                            .show(getSupportFragmentManager(), DIALOG_DOWNLOADMANAGER_ERROR);
    }


    @Override
    public void toggleWaitDialog(String tag) {
        DialogFragment dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(tag);
        if (dialog == null) new DialogIndeterminateProgress.Builder().setCancelable(false)
                                                                     .setMessage("Bitte warten...")
                                                                     .buildSupport()
                                                                     .show(getSupportFragmentManager(), tag);
        else dialog.dismiss();
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
                log.trace("start reader for paper: {}", openPaper);
                Intent intent = new Intent(this, ReaderActivity.class);
                intent.putExtra(ReaderActivity.KEY_EXTRA_PAPER_ID, id);
                startActivity(intent);
            } else {
                switch (Connection.getConnectionType(this)) {
                    case Connection.CONNECTION_NOT_AVAILABLE:
                        showNoConnectionDialog();
                        break;
                    default:
                        toggleWaitDialog(DIALOG_WAIT + openPaper.getBookId());
                        //DownloadHelper downloadHelper = new DownloadHelper(this);
                        try {
                            DownloadManager.getInstance(this)
                                           .enqueResource(openPaper);
                            retainDataFragment.openPaperWaitingForRessource = id;
                        } catch (DownloadManager.NotEnoughSpaceException e) {
                            showDownloadErrorDialog(getString(R.string.message_resourcedownload_error),
                                                    getString(R.string.message_not_enough_space), e);
                        }

                }
            }
        } catch (Paper.PaperNotFoundException e) {
            log.error("", e);
        }
    }

    public void updateTitle() {
        StringBuilder titleBuilder = new StringBuilder(getString(getApplicationInfo().labelRes));
        if (AccountHelper.getInstance(this)
                         .isDemoMode()) {
            titleBuilder.append(" ")
                        .append(getString(R.string.demo_titel_appendix));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleBuilder.toString());
        }
    }

    @Override
    public void requestSync(Calendar start, Calendar end) {
        if (start != null && end != null) {
            SyncHelper.requestSync(this, start, end);
            return;
        }
        SyncHelper.requestSync(this);
    }

    public void onEventMainThread(PaperDownloadFinishedEvent event) {
        log.debug("event: {}", event);
        if (retainDataFragment.useOpenPaperafterDownload) {
            if (event.getPaperId() == retainDataFragment.openPaperIdAfterDownload) {
                openReader(retainDataFragment.openPaperIdAfterDownload);
            }
        }
    }

    public void onEventMainThread(PaperDownloadFailedEvent event) {
        try {
            Paper paper = new Paper(this, event.getPaperId());
            showDownloadErrorDialog(paper.getTitelWithDate(this), null, event.getException());
            NotificationHelper.cancelDownloadErrorNotification(this, event.getPaperId());
        } catch (Paper.PaperNotFoundException e) {
            e.printStackTrace();
        }
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
            if (which == Dialog.BUTTON_NEUTRAL) mDrawerFragment.simulateClick(userItem, true);
        } else if (DIALOG_USER_REENTER.equals(tag)) {
            if (which == Dialog.BUTTON_POSITIVE) {
                TazSettings.getInstance(this)
                           .setPref(TazSettings.PREFKEY.USERMIGRATIONNOTIFICATION, true);
                mDrawerFragment.simulateClick(userItem, true);
            }
        } else if (DIALOG_MIGRATION_FINISHED.equals(tag)) {
            if (which == Dialog.BUTTON_POSITIVE) {
                try {
                    JSONArray json = new JSONArray(arguments.getString(ARGUMENT_MIGRATED_IDS_JSONARRAY));
                    for (int i = 0; i < json.length(); i++) {
                        startDownload(json.getLong(i));
                    }
                } catch (JSONException | Paper.PaperNotFoundException e) {
                    log.warn("", e);

                }

            }
        } else if (DIALOG_DOWNLOAD_MOBILE.equals(tag)) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    retainDataFragment.allowMobileDownload = true;
                    startDownloadQueue();
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    retainDataFragment.downloadQueue.clear();
                    break;
                case Dialog.BUTTON_NEUTRAL:
                    retainDataFragment.downloadQueue.clear();
                    startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                    break;
            }
        } else if (DIALOG_ACCOUNT_ERROR.equals(tag)) {
            finish();
        } else if (ImportFragment.DIALOG_PERMISSION_WRITE.equals(tag) && which == Dialog.BUTTON_POSITIVE) {
            Fragment contentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (contentFragment != null && contentFragment instanceof ImportFragment) {
                ((ImportFragment) contentFragment).startWithPermissionCheck();
            } else {
                onBackPressed();
            }
        }
    }


    @Override
    public void onDialogAdapterListClick(String tag, DialogAdapterList.DialogAdapterListEntry entry, Bundle arguments) {
        if (DIALOG_ARCHIVE_YEAR.equals(tag)) {
            if (getResources().getBoolean(R.bool.archive_monthly)) {
                showArchiveMonthPicker(((ArchiveEntry) entry).getNumber());
            } else {
                int year = ((ArchiveEntry) entry).getNumber();
                Calendar startCal = Calendar.getInstance();
                Calendar endCal = Calendar.getInstance();
                startCal.set(year, Calendar.JANUARY, 1);
                endCal.set(year, Calendar.DECEMBER, 31);
                requestSync(startCal, endCal);
            }
        } else if (DIALOG_ARCHIVE_MONTH.equals(tag)) {
            int year = arguments.getInt(ARGUMENT_ARCHIVE_YEAR);
            int month = ((ArchiveEntry) entry).getNumber();
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
            retainDataFragment.downloadQueue.clear();
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
        List<Long> downloadQueue       = new ArrayList<>();
        boolean    allowMobileDownload = false;
        private long    openPaperIdAfterDownload     = -1;
        private long    openPaperWaitingForRessource = -1;
        private boolean useOpenPaperafterDownload    = true;
        List<NavigationDrawerFragment.NavigationItem> navBackstack = new ArrayList<>();
        WeakReference<IStartCallback> callback;

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
            fragment.callback = new WeakReference<>(callback);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        private boolean hasCallback() {
            return callback.get() != null;
        }

        private IStartCallback getCallback() {
            return callback.get();
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
            if (hasCallback()) getCallback().toggleWaitDialog(DIALOG_WAIT + "delete");
            new DeleteTask(getActivity()) {

                @Override
                protected void onPostError(Exception exception) {
                    log.error("", exception);

                    if (hasCallback()) getCallback().toggleWaitDialog(DIALOG_WAIT + "delete");
                }

                @Override
                protected void onPostSuccess(Void aVoid) {
                    if (hasCallback()) getCallback().toggleWaitDialog(DIALOG_WAIT + "delete");
                }
            }.execute(ids);
        }
    }
}
