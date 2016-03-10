package de.thecode.android.tazreader.migration;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.importer.ImportActivity;
import de.thecode.android.tazreader.start.StartActivity;
import de.thecode.android.tazreader.utils.BaseActivity;

/**
 * Created by mate on 21.04.2015.
 */
public class MigrationActivity extends BaseActivity implements MigrationWorkerFragment.MigrationWorkerCallback {

    private static final Logger log = LoggerFactory.getLogger(MigrationActivity.class);

    private static final String WORKER_FRAGMENT_TAG = "MigrationWorkerFragment";
    MigrationWorkerFragment workerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.debug("");
        setContentView(R.layout.activity_migrate);
        workerFragment = MigrationWorkerFragment.findOrCreateWorkerFragment(getFragmentManager(), WORKER_FRAGMENT_TAG, this);
    }




    @Override
    public void onMigrationStart(int toVersionNumber) {
        log.debug("{}",toVersionNumber);
    }

    @Override
    public void onMigrationFinished(int toVersionNumber,List<File> importFiles) {
        log.debug("{}",toVersionNumber);
//        if (toVersionNumber < ??)                                                     For futher Migrations activate this
//            TazSettings.setPref(this, TazSettings.PREFKEY.PAPERMIGRATEFROM,toVersionNumber);
//        else
            TazSettings.removePref(this, TazSettings.PREFKEY.PAPERMIGRATEFROM);

        if (importFiles != null && importFiles.size() > 0) {
            Uri[] dataUris = new Uri[importFiles.size()];
            for (int i = 0; i < dataUris.length; i++) {
                dataUris[i] = Uri.fromFile(importFiles.get(i));
            }
            Intent importIntent = new Intent(this, ImportActivity.class);
            //importIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            importIntent.putExtra(ImportActivity.EXTRA_DATA_URIS, dataUris);
            importIntent.putExtra(ImportActivity.EXTRA_PREVENT_IMPORT_FLAG,true);
            importIntent.putExtra(ImportActivity.EXTRA_DELETE_SOURCE_FILE,true);
//            importIntent.putExtra(ImportActivity.EXTRA_START_LIBRARY,true);
            importIntent.putExtra(ImportActivity.EXTRA_NO_USER_CALLBACK,true);
            startActivityForResult(importIntent, ImportActivity.REQUEST_CODE_IMPORT_ACTIVITY);
        } else
        {
            finishActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        log.debug("");
        if (requestCode == ImportActivity.REQUEST_CODE_IMPORT_ACTIVITY) {
            TazSettings.setPref(this,TazSettings.PREFKEY.FORCESYNC,true);
            finishActivity();
        }
    }

    private void finishActivity() {
        log.debug("");
        Intent startIntent = new Intent(this, StartActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startIntent);
    }

    @Override
    public void onMigrationError(Exception e) {
        log.error("",e);
    }



    @Override
    public void onBackPressed() {
        //Block back button
    }
}
