package de.thecode.android.tazreader.importer;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mateware.dialog.Dialog;
import de.mateware.dialog.listener.DialogButtonListener;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.utils.BaseActivity;

/**
 * Created by mate on 16.04.2015.
 */
public class ImportActivity extends BaseActivity implements DialogButtonListener, ImportWorkerFragment.ImportRetainFragmentCallback {

    private static final Logger log = LoggerFactory.getLogger(ImportActivity.class);

    public static final int REQUEST_CODE_IMPORT_ACTIVITY = 29452;

    public static final String EXTRA_RESULT_URIS = "resultUris";
    public static final String EXTRA_PREVENT_IMPORT_FLAG = "preventImportFlag";
    public static final String EXTRA_DATA_URIS = "resultUris";

    public static final String EXTRA_DELETE_SOURCE_FILE = "deleteSourceFile";
    public static final String EXTRA_NO_USER_CALLBACK = "deleteSourceFile";

    private static final String WORKER_FRAGMENT_TAG = "ImportWorkerFragment";

    public static final String DIALOG_ERROR_IMPORT = "dialogError";
    public static final String DIALOG_EXISTS_IMPORT = "dialogExists";
    public static final String DIALOG_DOWNLOAD = "dialogDownload";

    private static final String BUNDLE_KEY_METADATA = "metadata";
    private static final String BUNDLE_KEY_FILE = "file";
    private static final String BUNDLE_KEY_DATAURI = "datauri";
    private static final String BUNDLE_KEY_DELETEFILE = "deleteFile";
    private static final String BUNDLE_KEY_DOWNLOAD_URIS = "resultUris";
    private static final String BUNDLE_KEY_RESULT_URIS = EXTRA_RESULT_URIS;


    ImportWorkerFragment workerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        ArrayList<Uri> data = new ArrayList<>();
        if (getIntent().getExtras() != null) {
            Parcelable[] dataUris = getIntent().getExtras()
                                               .getParcelableArray(EXTRA_DATA_URIS);
            if (dataUris != null) {
                data.addAll(Arrays.asList(Arrays.copyOf(dataUris, dataUris.length, Uri[].class)));
            }
        }
        if (getIntent().getData() != null) data.add(getIntent().getData());

        workerFragment = ImportWorkerFragment.findOrCreateRetainFragment(getSupportFragmentManager(), WORKER_FRAGMENT_TAG, this, data);
    }

    public void showErrorDialog(String message) {
        new Dialog.Builder().setMessage(message)
                            .setPositiveButton()
                            .setCancelable(false)
                            .buildSupport()
                            .show(getSupportFragmentManager(), DIALOG_ERROR_IMPORT);
    }


    @Override
    public void onDialogClick(String tag, Bundle arguments, int which) {
        if (DIALOG_ERROR_IMPORT.equals(tag) && which == Dialog.BUTTON_POSITIVE) {
            workerFragment.handleNextDataUri();
        } else if (DIALOG_EXISTS_IMPORT.equals(tag)) {
            File cacheFile = (File) arguments.getSerializable(BUNDLE_KEY_FILE);
            ImportMetadata metadata = arguments.getParcelable(BUNDLE_KEY_METADATA);
            Uri dataUri = arguments.getParcelable(BUNDLE_KEY_DATAURI);
            boolean deleteFile = arguments.getBoolean(BUNDLE_KEY_DELETEFILE);
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    workerFragment.stepFourStartImport(dataUri, metadata, cacheFile, deleteFile);
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    workerFragment.onFinished(dataUri, cacheFile, deleteFile);
                    break;
            }
        } else if (DIALOG_DOWNLOAD.equals(tag)) {
            Parcelable[] pDownloadUris = arguments.getParcelableArray(BUNDLE_KEY_DOWNLOAD_URIS);
            Uri[] downloadUris = null;
            if (pDownloadUris != null) {
                downloadUris = Arrays.copyOf(pDownloadUris, pDownloadUris.length, Uri[].class);
            }
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    if (downloadUris != null) {
                        for (Uri downloadUri : downloadUris) {
                            try {
                                DownloadManager.getInstance(this)
                                               .enquePaper(ContentUris.parseId(downloadUri));
                            } catch (Paper.PaperNotFoundException | AccountHelper.CreateAccountException | DownloadManager.DownloadNotAllowedException | DownloadManager.NotEnoughSpaceException e) {
                                log.error("", e);
                                Crashlytics.logException(e);
                            }
                        }
                    }
                case Dialog.BUTTON_NEGATIVE:
                    finishActivity(downloadUris);
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        //Block BackPress
    }

    @Override
    public void onImportRetainFragmentCreate(ImportWorkerFragment importRetainWorkerFragment) {
        log.debug("");
        //        new DialogIndeterminateProgress().withCancelable(false)
        //                                           .withMessage("Import")
        //                                           .show(getFragmentManager(), DIALOG_WAIT_IMPORT);
        if (getIntent().getExtras() != null) {
            workerFragment.preventImportFlag = getIntent().getExtras()
                                                          .getBoolean(EXTRA_PREVENT_IMPORT_FLAG, false);
            workerFragment.deleteSourceFile = getIntent().getExtras()
                                                         .getBoolean(EXTRA_DELETE_SOURCE_FILE, false);
            workerFragment.noUserCallback = getIntent().getExtras()
                                                       .getBoolean(EXTRA_NO_USER_CALLBACK, false);
        }

        workerFragment.handleNextDataUri();
    }

    @Override
    public void onFinishedImporting(List<Paper> importedPapers) {
        log.debug("");
        Uri[] importedPaperUris = new Uri[importedPapers.size()];
        Uri[] downloadPaperUris = new Uri[0];
        int i = 0;
        for (Paper importedPaper : importedPapers) {
            importedPaperUris[i] = importedPaper.getContentUri();

            if (importedPaper.hasUpdate()) {
                int j = downloadPaperUris.length;
                downloadPaperUris = Arrays.copyOf(downloadPaperUris, j + 1);
                downloadPaperUris[j] = importedPaper.getContentUri();
            }

            i++;
        }

        if (downloadPaperUris.length > 0) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArray(BUNDLE_KEY_DOWNLOAD_URIS, downloadPaperUris);
            bundle.putParcelableArray(BUNDLE_KEY_RESULT_URIS, importedPaperUris);
            new Dialog.Builder().addBundle(bundle)
                                .setMessage(getResources().getQuantityString(R.plurals.import_download, downloadPaperUris.length, downloadPaperUris.length))
                                .setCancelable(false)
                                .setPositiveButton(R.string.import_download_positive)
                                .setNegativeButton(R.string.import_download_negative)
                                .buildSupport()
                                .show(getSupportFragmentManager(), DIALOG_DOWNLOAD);
        } else {
            finishActivity(importedPaperUris);
        }
    }

    private void finishActivity(Uri[] paperUris) {
        Intent resultIntent = new Intent();
        if (paperUris == null || paperUris.length == 0) {
            setResult(RESULT_CANCELED, resultIntent);
        } else {
            resultIntent.putExtra(EXTRA_RESULT_URIS, paperUris);
            setResult(RESULT_OK, resultIntent);
            TazSettings.setPref(this, TazSettings.PREFKEY.FORCESYNC, true);
        }
        finish();
    }

    @Override
    public void onErrorWhileImport(Uri dataUri, Exception e) {
        log.debug("{} {}", dataUri, e);
        showErrorDialog(String.format(getString(R.string.import_error), dataUri, e.getLocalizedMessage()));
    }

    @Override
    public void onImportAlreadyExists(Uri dataUri, ImportMetadata metadata, File cacheFile, boolean deleteFile) {
        log.debug("");
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_KEY_METADATA, metadata);
        bundle.putSerializable(BUNDLE_KEY_FILE, cacheFile);
        bundle.putParcelable(BUNDLE_KEY_DATAURI, dataUri);
        bundle.putBoolean(BUNDLE_KEY_DELETEFILE, deleteFile);
        new Dialog.Builder().setCancelable(false)
                            .setPositiveButton()
                            .addBundle(bundle)
                            .setNegativeButton()
                            .setMessage(String.format(getString(R.string.import_already_exists), metadata.getBookId()))
                            .buildSupport()
                            .show(getSupportFragmentManager(), DIALOG_EXISTS_IMPORT);
    }
}
