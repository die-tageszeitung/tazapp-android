package de.thecode.android.tazreader.start;


import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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

import de.mateware.dialog.Dialog;
import de.mateware.dialog.DialogAdapterList;
import de.mateware.dialog.DialogIndeterminateProgress;
import de.mateware.dialog.LicenceDialog;
import de.mateware.dialog.licences.Agpl30Licence;
import de.mateware.dialog.licences.Apache20Licence;
import de.mateware.dialog.licences.BsdLicence;
import de.mateware.dialog.licences.MitLicence;
import de.mateware.dialog.listener.DialogAdapterListListener;
import de.mateware.dialog.listener.DialogButtonListener;
import de.mateware.dialog.listener.DialogCancelListener;
import de.mateware.dialog.listener.DialogDismissListener;
import de.mateware.snacky.Snacky;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.DeleteTask;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialog.ArchiveDialog;
import de.thecode.android.tazreader.dialog.ArchiveEntry;
import de.thecode.android.tazreader.dialog.HelpDialog;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.download.PaperDownloadFailedEvent;
import de.thecode.android.tazreader.download.PaperDownloadFinishedEvent;
import de.thecode.android.tazreader.download.ResourceDownloadEvent;
import de.thecode.android.tazreader.importer.ImportActivity;
import de.thecode.android.tazreader.job.SyncJob;
import de.thecode.android.tazreader.migration.MigrationActivity;
import de.thecode.android.tazreader.notifications.NotificationUtils;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.start.viewmodel.StartViewModel;
import de.thecode.android.tazreader.sync.SyncErrorEvent;
import de.thecode.android.tazreader.utils.BaseActivity;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.Connection;
import de.thecode.android.tazreader.widget.CustomToolbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import timber.log.Timber;

/**
 * Created by mate on 27.01.2015.
 */
public class StartActivity extends BaseActivity
        implements IStartCallback, DialogButtonListener, DialogDismissListener, DialogCancelListener, DialogAdapterListListener {

    //private static final String DIALOG_FIRST                 = "dialogFirst";
    private static final String DIALOG_USER_REENTER          = "dialogUserReenter";
    //private static final String DIALOG_MISSING_RESOURCE = "dialogMissingResource";
    private static final String DIALOG_RESOURCE_DOWNLOADING  = "dialogResourceDownloading";
    private static final String DIALOG_NO_CONNECTION         = "dialogNoConnection";
    private static final String DIALOG_MIGRATION_FINISHED    = "dialogMigrationFinished";
    private static final String DIALOG_DOWNLOAD_MOBILE       = "dialogDownloadMobile";
    private static final String DIALOG_ACCOUNT_ERROR         = "dialogAccountError";
    private static final String DIALOG_DOWNLOADMANAGER_ERROR = "dialogDownloadManagerError";
    private static final String DIALOG_DOWNLOAD_ERROR        = "dialogDownloadError";
    private static final String DIALOG_ARCHIVE_YEAR          = "dialogArchiveYear";
    private static final String DIALOG_ARCHIVE_MONTH         = "dialogArchiveMonth";
    private static final String DIALOG_WAIT                  = "dialogWait";
    private static final String DIALOG_LICENCES              = "dialogLicences";
    private static final String DIALOG_ERROR_OPEN_PAPER      = "dialogErrorOpenPaper";

    private static final String ARGUMENT_RESOURCE_KEY           = "resourceKey";
    private static final String ARGUMENT_RESOURCE_URL           = "resourceUrl";
    private static final String ARGUMENT_MIGRATED_IDS_JSONARRAY = "migratedIds";
    private static final String ARGUMENT_ARCHIVE_YEAR           = "archiveYear";

    private CustomToolbar            toolbar;
    private NavigationDrawerFragment mDrawerFragment;

    RetainDataFragment retainDataFragment;

    NavigationDrawerFragment.NavigationItem userItem;
    NavigationDrawerFragment.NavigationItem libraryItem;
    //    NavigationDrawerFragment.NavigationItem settingsItem;
    NavigationDrawerFragment.NavigationItem preferencesItem;

    NavigationDrawerFragment.ClickItem      helpItem;
    NavigationDrawerFragment.NavigationItem imprintItem;
    NavigationDrawerFragment.ClickItem      privacyTermsItem;
    // NavigationDrawerFragment.NavigationItem importItem;

    StartViewModel activityViewModel;

    TazSettings.OnPreferenceChangeListener demoModeChanged = new TazSettings.OnPreferenceChangeListener<Boolean>() {
        @Override
        public void onPreferenceChanged(Boolean value) {
            onDemoModeChanged(value);
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        TazSettings.getInstance(this)
                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.DEMOMODE, demoModeChanged);
    }

    @Override
    public void onStop() {
        TazSettings.getInstance(this)
                   .removeOnPreferenceChangeListener(demoModeChanged);
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Timber.i("receiviing new intent");
        //TODO handle intent data to open book
        super.onNewIntent(intent);
        openReaderFromDownloadNotificationIntent(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityViewModel = ViewModelProviders.of(this)
                                              .get(StartViewModel.class);
        if (TazSettings.getInstance(this)
                       .getPrefInt(TazSettings.PREFKEY.PAPERMIGRATEFROM, 0) != 0) {
            Intent migrationIntent = new Intent(this, MigrationActivity.class);
            migrationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(migrationIntent);
            return;
        }
        TazSettings.getInstance(this)
                   .removePref(TazSettings.PREFKEY.PAPERMIGRATEFROM);

        //Orientation.setActivityOrientationFromPrefs(this);


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
        toolbar.setTitleTextAppearance(this, R.style.Toolbar_TitleText);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        updateTitle();

        mDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        mDrawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);

        userItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_account),
                                                               R.drawable.ic_account,
                                                               LoginFragment.class);
//        importItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_import), R.drawable.ic_file_folder,
//                                                                 ImportFragment.class);
//        importItem.setAccessibilty(false);
        libraryItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_library),
                                                                  R.drawable.ic_library,
                                                                  LibraryFragment.class);
//        settingsItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_settings), R.drawable.ic_settings,
//                                                                   SettingsFragment.class);
//        settingsItem.setAccessibilty(false);

        preferencesItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_preferences),
                                                                      R.drawable.ic_settings,
                                                                      PreferencesFragment.class);
        preferencesItem.setAccessibilty(false);


        helpItem = new NavigationDrawerFragment.ClickItem(getString(R.string.drawer_help), R.drawable.ic_help);
        helpItem.setAccessibilty(false);
        privacyTermsItem = new NavigationDrawerFragment.ClickItem(getString(R.string.drawer_privacy_terms),
                                                                  R.drawable.ic_security_black_24dp);
        helpItem.setAccessibilty(false);
        imprintItem = new NavigationDrawerFragment.NavigationItem(getString(R.string.drawer_imprint),
                                                                  R.drawable.ic_imprint,
                                                                  ImprintFragment.class);


        mDrawerFragment.addItem(libraryItem);
        mDrawerFragment.addDividerItem();
        mDrawerFragment.addItem(userItem);
//        mDrawerFragment.addItem(importItem);
        mDrawerFragment.addItem(helpItem);
        mDrawerFragment.addDividerItem();
        mDrawerFragment.addItem(imprintItem);
        mDrawerFragment.addItem(privacyTermsItem);
        // mDrawerFragment.addItem(settingsItem);
        mDrawerFragment.addItem(preferencesItem);


        activityViewModel.getActionMode()
                         .observe(this, aBoolean -> {
                             if (aBoolean == null) return;
                             mDrawerFragment.setEnabled(!aBoolean);
                         });
        activityViewModel.getCurrentFragment()
                         .observe(this, aClass -> mDrawerFragment.setActive(aClass));

        //has to be after adding useritem
        //updateUserInDrawer();

        //initial Fragment with colored navdrawer
        if (getSupportFragmentManager().findFragmentById(R.id.content_frame) == null)
            mDrawerFragment.simulateClick(libraryItem, false);


        //        showMigrationDownloadDialog();


        if (TazSettings.getInstance(this)
                       .getPrefBoolean(TazSettings.PREFKEY.FISRTSTART, true)) {
            SyncJob.scheduleJobImmediately(false);
            TazSettings.getInstance(this)
                       .setPref(TazSettings.PREFKEY.FISRTSTART, false);
            TazSettings.getInstance(this)
                       .setPref(TazSettings.PREFKEY.USERMIGRATIONNOTIFICATION, true);
//            firstStartDialog();
        } else if (!TazSettings.getInstance(this)
                               .getPrefBoolean(TazSettings.PREFKEY.USERMIGRATIONNOTIFICATION, false)) {
            new Dialog.Builder().setCancelable(false)
                                .setMessage(R.string.dialog_user_reenter)
                                .setPositiveButton(R.string.dialog_understood)
                                .buildSupport()
                                .show(getSupportFragmentManager(), DIALOG_USER_REENTER);
        }

        //Todo run Sync on first start
//        if (TazSettings.getInstance(this)
//                       .getSyncServiceNextRun() == 0) SyncHelper.requestSync(this);
        //Intent intent = getIntent();
        openReaderFromDownloadNotificationIntent(getIntent());
    }

    private void openReaderFromDownloadNotificationIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(NotificationUtils.NOTIFICATION_EXTRA_TYPE_ID) && intent.hasExtra(NotificationUtils.NOTIFICATION_EXTRA_BOOKID)) {
                String bookId = intent.getStringExtra(NotificationUtils.NOTIFICATION_EXTRA_BOOKID);
                int type = intent.getIntExtra(NotificationUtils.NOTIFICATION_EXTRA_TYPE_ID, -1);
                Paper paper = Paper.getPaperWithBookId(this, bookId);
                if (type == NotificationUtils.DOWNLOAD_NOTIFICTAION_ID && paper != null) {
                    openReader(paper.getId());
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.d("requestCode: %s, resultCode: %s, data: %s", requestCode, resultCode, data);
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
                Timber.e(e);
            } catch (IllegalAccessException e) {
                Timber.e(e);
            }
        }
    }

    @Override
    public void onNavigationClick(NavigationDrawerFragment.ClickItem item) {
        Timber.i("");
        if (helpItem.equals(item)) {
            showHelpDialog(HelpDialog.HELP_LIBRARY);
        } else if (privacyTermsItem.equals(item)) {
            showHelpDialog(HelpDialog.HELP_PRIVACY);
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
    public void onSuccessfulCredentialsCheck() {
        mDrawerFragment.simulateClick(libraryItem, false);
    }

    public void onDemoModeChanged(boolean demoMode) {
        updateTitle();
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
    public void onUpdateDrawer(Fragment fragment) {
        //TODO Remove
        //mDrawerFragment.setActive(fragment.getClass());
    }

    @Override
    public void startDownload(long paperId) throws Paper.PaperNotFoundException {
        Paper downloadPaper = Paper.getPaperWithId(this, paperId);
        if (downloadPaper == null) throw new Paper.PaperNotFoundException();
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
        //            mDownloadHelper.enquePaper(bookId);
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
                Paper paper = Paper.getPaperWithId(this, paperId);
                if (paper == null) throw new Paper.PaperNotFoundException();
                try {
                    DownloadManager.getInstance(this)
                                   .enquePaper(paperId, false);
                } catch (IllegalArgumentException e) {
                    showErrorDialog(getString(R.string.dialog_downloadmanager_error), DIALOG_DOWNLOADMANAGER_ERROR);
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


//    private void firstStartDialog() {
//        new Dialog.Builder().setMessage(R.string.dialog_first)
//                            .setPositiveButton()
//                            .setNeutralButton(R.string.drawer_account)
//                            .buildSupport()
//                            .show(getSupportFragmentManager(), DIALOG_FIRST);
//    }

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

    private void showLicencesDialog() {
        new LicenceDialog.Builder().addEntry(new Apache20Licence(this,
                                                                 "Android Support Library",
                                                                 "The Android Open Source Project",
                                                                 2011))
                                   .addEntry(new Apache20Licence(this, "OkHttp", "Square, Inc.", 2016))
                                   .addEntry(new Apache20Licence(this, "Picasso", "Square, Inc.", 2013))
                                   .addEntry(new Apache20Licence(this, "Picasso 2 OkHttp 3 Downloader", "Jake Wharton", 2016))
                                   .addEntry(new Apache20Licence(this, "AESCrypt-Android", "Scott Alexander-Bown", 2014))
                                   .addEntry(new Apache20Licence(this, "Snacky", "Mate Siede", 2017))
                                   .addEntry(new Apache20Licence(this, "DataFragment", "Mate Siede", 2017))
                                   .addEntry(new MitLicence(this, "dd-plist", "Daniel Dreibrodt", 2016))
                                   .addEntry(new Apache20Licence(this, "cwac-provider", "Mark Murphy", 2016))
                                   .addEntry(new Apache20Licence(this, "Centering Recycler View", "Shigehiro Soejima", 2015))
                                   .addEntry(new Apache20Licence(this,
                                                                 "EventBus 3",
                                                                 "Markus Junginger, greenrobot (http://greenrobot.org)",
                                                                 2016))
                                   .addEntry(new Apache20Licence(this, "Commons IO", "The Apache Software Foundation", 2016))
                                   .addEntry(new Apache20Licence(this, "RecyclerView-FlexibleDivider", "yqritc", 2016))
                                   .addEntry(new Agpl30Licence(this, "mupdf", "Artifex Software, Inc.", 2015))
                                   .addEntry(new BsdLicence(this, "Stetho", "Facebook, Inc.", 2015))
                                   .setPositiveButton()
                                   .buildSupport()
                                   .show(getSupportFragmentManager(), DIALOG_LICENCES);
    }

//    private void showAccountErrorDialog() {
//        new Dialog.Builder().setMessage(R.string.dialog_account_error)
//                            .setPositiveButton()
//                            .setCancelable(false)
//                            .buildSupport()
//                            .show(getSupportFragmentManager(), DIALOG_ACCOUNT_ERROR);
//    }

//    private void showDownloadManagerErrorDialog() {
//        new Dialog.Builder().setMessage(R.string.dialog_downloadmanager_error)
//                            .setPositiveButton()
//                            .buildSupport()
//                            .show(getSupportFragmentManager(), DIALOG_DOWNLOADMANAGER_ERROR);
//    }

    private void showErrorDialog(String message, String tag) {
        new Dialog.Builder().setMessage(message)
                            .setPositiveButton()
                            .setCancelable(false)
                            .buildSupport()
                            .show(getSupportFragmentManager(), tag);
    }


    void showArchiveYearPicker() {
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
        Timber.d("year: %s", year);
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
    public void showWaitDialog(String tag, String message) {
        DialogFragment dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(tag);
        if (dialog == null) new DialogIndeterminateProgress.Builder().setCancelable(false)
                                                                     .setMessage(message)
                                                                     .buildSupport()
                                                                     .show(getSupportFragmentManager(), tag);
    }

    @Override
    public void hideWaitDialog(String tag) {
        DialogFragment dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(tag);
        if (dialog != null) dialog.dismiss();
    }


    @Override
    public void openReader(long id) {

        Paper openPaper;
        try {
            openPaper = Paper.getPaperWithId(this, id);
            if (openPaper == null) throw new Paper.PaperNotFoundException();
            if (!openPaper.isDownloaded()) {
                showErrorDialog(getString(R.string.message_paper_not_downloaded), DIALOG_ERROR_OPEN_PAPER);
            }
            Resource paperResource = openPaper.getResourcePartner(this);
            //TODO Check for null resource and handle it, ask for sync / delete and redownload
            if (paperResource.isDownloaded()) {
                Timber.i("start reader for paper: %s", openPaper);
                Intent intent = new Intent(this, ReaderActivity.class);
                intent.putExtra(ReaderActivity.KEY_EXTRA_PAPER_ID, id);
                intent.putExtra(ReaderActivity.KEY_EXTRA_RESOURCE_KEY, paperResource.getKey());
                startActivity(intent);
            } else {
                switch (Connection.getConnectionType(this)) {
                    case Connection.CONNECTION_NOT_AVAILABLE:
                        showErrorDialog(getString(R.string.message_resource_not_downloaded_no_connection),
                                        DIALOG_ERROR_OPEN_PAPER);
                        break;
                    default:
                        showWaitDialog(DIALOG_WAIT + openPaper.getBookId(),
                                       getString(R.string.dialog_meassage_loading_missing_resource));
                        try {
                            DownloadManager.getInstance(this)
                                           .enqueResource(paperResource, false);
                            retainDataFragment.openPaperWaitingForRessource = id;
                        } catch (DownloadManager.NotEnoughSpaceException e) {
                            showDownloadErrorDialog(getString(R.string.message_resourcedownload_error),
                                                    getString(R.string.message_not_enough_space),
                                                    e);
                        }

                }
            }
        } catch (Paper.PaperNotFoundException e) {
            Timber.e(e);
        }
    }

    public void updateTitle() {
        StringBuilder titleBuilder = new StringBuilder(getString(getApplicationInfo().labelRes));
        if (TazSettings.getInstance(this)
                       .isDemoMode()) {
            titleBuilder.append(" ")
                        .append(getString(R.string.demo_titel_appendix));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleBuilder.toString());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPaperDownloadFinished(SyncErrorEvent event) {
        Snacky.builder()
              .setView(findViewById(R.id.content_frame))
              .setDuration(Snacky.LENGTH_SHORT)
              .setText(event.getMessage())
              .error()
              .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPaperDownloadFinished(PaperDownloadFinishedEvent event) {
        Timber.d("event: %s", event);
        if (retainDataFragment.useOpenPaperafterDownload) {
            if (event.getPaperId() == retainDataFragment.openPaperIdAfterDownload) {
                openReader(retainDataFragment.openPaperIdAfterDownload);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPaperDownloadFailed(PaperDownloadFailedEvent event) {
        try {
            Paper paper = Paper.getPaperWithId(this, event.getPaperId());
            if (paper == null) throw new Paper.PaperNotFoundException();
            showDownloadErrorDialog(paper.getTitelWithDate(this), null, event.getException());

            //NotificationHelper.cancelDownloadErrorNotification(this, event.getBookId());
            new NotificationUtils(this).removeDownloadNotification(event.getPaperId());
        } catch (Paper.PaperNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResourceDownload(ResourceDownloadEvent event) {
        if (retainDataFragment.openPaperWaitingForRessource != -1) {
            try {
                Paper waitingPaper = Paper.getPaperWithId(this, retainDataFragment.openPaperWaitingForRessource);
                if (waitingPaper == null) throw new Paper.PaperNotFoundException();
                if (event.getKey()
                         .equals(waitingPaper.getResourcePartner(this)
                                             .getKey())) {
                    hideWaitDialog(DIALOG_WAIT + waitingPaper.getBookId());
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

    AsyncTask<Void, Void, Boolean> firstStartTask;

    @Override
    protected void onResume() {
        super.onResume();
        if (firstStartTask != null) firstStartTask.cancel(true);
        firstStartTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                boolean showDialog = false;
                List<Paper> allPapers = Paper.getAllPapers(StartActivity.this);
                boolean foundDownloaded = false;
                for (Paper paper : allPapers) {
                    if (foundDownloaded || isCancelled()) break;
                    foundDownloaded = paper.isDownloaded() || paper.isDownloading();
                }
                if (!foundDownloaded) {
                    showDialog = TazSettings.getInstance(StartActivity.this)
                                            .isDemoMode();
                }
                return showDialog;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if (aBoolean) showHelpDialog(HelpDialog.HELP_INTRO);
            }
        };
        firstStartTask.execute();
    }

    @Override
    protected void onPause() {
        if (firstStartTask != null) firstStartTask.cancel(true);
        super.onPause();
    }

    @Override
    public void onDialogClick(String tag, Bundle arguments, int which) {
        super.onDialogClick(tag, arguments, which);
        if (DIALOG_HELP.equals(tag)) {
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
                    Timber.w(e);
                }

            }
        } else if (ImprintFragment.DIALOG_TECHINFO.equals(tag)) {
            switch (which) {
                case Dialog.BUTTON_NEUTRAL:
                    showLicencesDialog();
                    break;
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
        super.onDialogAdapterListClick(tag, entry, arguments);
        if (DIALOG_ARCHIVE_YEAR.equals(tag)) {
            if (getResources().getBoolean(R.bool.archive_monthly)) {
                showArchiveMonthPicker(((ArchiveEntry) entry).getNumber());
            } else {
                int year = ((ArchiveEntry) entry).getNumber();
                Calendar startCal = Calendar.getInstance();
                Calendar endCal = Calendar.getInstance();
                startCal.set(year, Calendar.JANUARY, 1);
                endCal.set(year, Calendar.DECEMBER, 31);
                SyncJob.scheduleJobImmediately(true, startCal, endCal);
                //SyncHelper.requestSync(this, startCal, endCal);
            }
        } else if (DIALOG_ARCHIVE_MONTH.equals(tag)) {
            int year = arguments.getInt(ARGUMENT_ARCHIVE_YEAR);
            int month = ((ArchiveEntry) entry).getNumber();
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.set(year, month, 1);
            int lastDayOfMont = startCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            endCal.set(year, month, lastDayOfMont);
            //SyncHelper.requestSync(this, startCal, endCal);
            SyncJob.scheduleJobImmediately(true, startCal, endCal);
        }
    }


    @Override
    public void onDialogDismiss(String tag, Bundle arguments) {
        super.onDialogDismiss(tag, arguments);
        if (DIALOG_DOWNLOAD_MOBILE.equals(tag)) {
            retainDataFragment.downloadQueue.clear();
        }
    }

    @Override
    public void onDialogCancel(String tag, Bundle arguments) {
        super.onDialogCancel(tag, arguments);
        if (DIALOG_DOWNLOAD_MOBILE.equals(tag)) {
            retainDataFragment.downloadQueue.clear();
        }
    }


    public static class RetainDataFragment extends BaseFragment {

        private static final String TAG = "RetainDataFragment";
        public LruCache<String, Bitmap> cache;
        public List<Long> selectedInLibrary = new ArrayList<>();

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
            if (hasCallback())
                getCallback().showWaitDialog(DIALOG_WAIT + "delete", getString(R.string.dialog_message_delete_wait));
            new DeleteTask(getActivity()) {

                @Override
                protected void onPostError(Exception exception) {
                    Timber.e(exception);

                    if (hasCallback()) getCallback().hideWaitDialog(DIALOG_WAIT + "delete");
                }

                @Override
                protected void onPostSuccess(Void aVoid) {
                    if (hasCallback()) getCallback().hideWaitDialog(DIALOG_WAIT + "delete");
                }
            }.execute(ids);
        }
    }
}
