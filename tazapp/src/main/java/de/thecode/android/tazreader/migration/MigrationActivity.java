package de.thecode.android.tazreader.migration;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.importer.ImportActivity;
import de.thecode.android.tazreader.start.StartActivity;
import de.thecode.android.tazreader.utils.BaseActivity;

import java.io.File;
import java.util.List;

import timber.log.Timber;

/**
 * Created by mate on 21.04.2015.
 */
public class MigrationActivity extends BaseActivity implements MigrationWorkerFragment.MigrationWorkerCallback {

    private static final String WORKER_FRAGMENT_TAG = "MigrationWorkerFragment";
    MigrationWorkerFragment workerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_migrate);
        workerFragment = MigrationWorkerFragment.findOrCreateWorkerFragment(getSupportFragmentManager(), WORKER_FRAGMENT_TAG,
                                                                            this);
    }


    @Override
    public void onMigrationStart(int toVersionNumber) {
        Timber.d("%d", toVersionNumber);
    }

    @Override
    public void onMigrationFinished(int toVersionNumber, List<File> importFiles) {
        Timber.d("%d", toVersionNumber);
//        if (toVersionNumber < ??)                                                     For futher Migrations activate this
//            TazSettings.setPref(this, TazSettings.PREFKEY.PAPERMIGRATEFROM,toVersionNumber);
//        else
        TazSettings.getInstance(this)
                   .removePref(TazSettings.PREFKEY.PAPERMIGRATEFROM);

        if (importFiles != null && importFiles.size() > 0) {
            Uri[] dataUris = new Uri[importFiles.size()];
            for (int i = 0; i < dataUris.length; i++) {
                dataUris[i] = Uri.fromFile(importFiles.get(i));
            }
            Intent importIntent = new Intent(this, ImportActivity.class);
            //importIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            importIntent.putExtra(ImportActivity.EXTRA_DATA_URIS, dataUris);
            importIntent.putExtra(ImportActivity.EXTRA_PREVENT_IMPORT_FLAG, true);
            importIntent.putExtra(ImportActivity.EXTRA_DELETE_SOURCE_FILE, true);
//            importIntent.putExtra(ImportActivity.EXTRA_START_LIBRARY,true);
            importIntent.putExtra(ImportActivity.EXTRA_NO_USER_CALLBACK, true);
            startActivityForResult(importIntent, ImportActivity.REQUEST_CODE_IMPORT_ACTIVITY);
        } else {
            finishActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ImportActivity.REQUEST_CODE_IMPORT_ACTIVITY) {
//            TazSettings.getInstance(this)
//                       .setPref(TazSettings.PREFKEY.FORCESYNC, true);
            finishActivity();
        }
    }

    private void finishActivity() {

        Intent startIntent = new Intent(this, StartActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startIntent);
    }

    @Override
    public void onMigrationError(Exception e) {
        Timber.e(e);
    }


    @Override
    public void onBackPressed() {
        //Block back button
    }
}
