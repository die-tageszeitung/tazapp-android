package de.thecode.android.tazreader.start;

import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.download.PaperDeletedEvent;
import de.thecode.android.tazreader.download.PaperDownloadFinishedEvent;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by mate on 13.04.2015.
 */
public class ImportDataRetainFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<List<ImportDirectoryLoader.ImportFileWrapper>> {


    private static final String TAG = "RetainFragmentImport";
    private static final int DIRECTORY_LOADER = 3;
    List<ImportDirectoryLoader.ImportFileWrapper> data;

    private File rootDir;
    private File currentDir;

    private WeakReference<ImportDataCallback> callback;

    public ImportDataRetainFragment() {
    }

    public static ImportDataRetainFragment findOrCreateRetainFragment(FragmentManager fm, ImportDataCallback callback) {
        Log.d();
        ImportDataRetainFragment fragment = (ImportDataRetainFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            Log.d("new");
            fragment = new ImportDataRetainFragment();
            fragment.setCurrentDir(Environment.getExternalStorageDirectory());
            fragment.setCallback(callback);
            fm.beginTransaction()
              .add(fragment, TAG)
              .commit();
        } else {
            fragment.setCallback(callback);
            Log.d("retained");
        }
        return fragment;
    }

    public void setCallback(ImportDataCallback callback) {
        this.callback = new WeakReference<>(callback);
    }

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private ImportDataCallback getCallback() {
        return callback.get();
    }

    public void setCurrentDir(File currentDir) {
        Log.d();
        if (rootDir == null) this.rootDir = currentDir;

        this.currentDir = currentDir;
    }

    public File getCurrentDir() {
        Log.d();
        return currentDir;
    }

    public File getRootDir() {
        Log.d();
        return rootDir;
    }

    public List<ImportDirectoryLoader.ImportFileWrapper> getData() {
        return data;
    }

    public void setData(List<ImportDirectoryLoader.ImportFileWrapper> data) {
        Log.d();
        this.data = data;

        if (hasCallback()) getCallback().dataChanged();
    }

    public void restart() {
        getLoaderManager().restartLoader(DIRECTORY_LOADER, null, this);
    }

    public void destroyLoader() {getLoaderManager().destroyLoader(DIRECTORY_LOADER);}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d();
        setRetainInstance(true);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d();
        getLoaderManager().initLoader(DIRECTORY_LOADER,null, this);
    }

    @Override
    public void onStop() {
        Log.d();
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onDetach() {
        Log.d();
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onEventMainThread(PaperDownloadFinishedEvent event) {
        restart();
    }

    public void onEventMainThread(PaperDeletedEvent event) {
        restart();
    }

    @Override
    public Loader<List<ImportDirectoryLoader.ImportFileWrapper>> onCreateLoader(int id, Bundle args) {
        Log.d();


        if (data==null)
            data = new ArrayList<>();
        data.clear();

        ImportDirectoryLoader.ImportFileWrapper waitIpf = new ImportDirectoryLoader.ImportFileWrapper();
        waitIpf.type = ImportDirectoryLoader.ImportFileWrapper.TYPE.WAIT;
        data.add(waitIpf);
        if (hasCallback()) getCallback().dataChanged();

        return new ImportDirectoryLoader(getActivity(), getRootDir(), getCurrentDir());
    }

    @Override
    public void onLoadFinished(Loader<List<ImportDirectoryLoader.ImportFileWrapper>> loader, List<ImportDirectoryLoader.ImportFileWrapper> data) {
        Log.d();
        setData(data);

    }

    @Override
    public void onLoaderReset(Loader<List<ImportDirectoryLoader.ImportFileWrapper>> loader) {
        Log.d();
    }

    public interface ImportDataCallback {
        public void dataChanged();
//        public void onStartLoading();
    }



}
