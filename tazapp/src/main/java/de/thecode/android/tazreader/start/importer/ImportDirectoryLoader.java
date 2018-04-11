package de.thecode.android.tazreader.start.importer;


import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by mate on 07.04.2015.
 */
public class ImportDirectoryLoader extends AsyncTaskLoader<List<ImportDirectoryLoader.ImportFileWrapper>> {

    File root;
    File currentDir;

    List<ImportFileWrapper> mData;

    public ImportDirectoryLoader(Context context, File root, File currentDir) {
        super(context);
        this.root = root;
        this.currentDir = currentDir;
    }

    @Override
    public List<ImportFileWrapper> loadInBackground() {
        //EventBus.getDefault().postSticky(new LoadingEvent(true));



        List<ImportFileWrapper> result = new ArrayList<>();

        List<File> dirs = new ArrayList<>();
        List<File> files = new ArrayList<>();

        File[] list = currentDir.listFiles(filter);
        if (list != null) {
            for (File file : list) {
                if (file.isDirectory())
                    dirs.add(file);
                else
                    files.add(file);
            }
            Collections.sort(dirs, new SortIgnoreCase());
            Collections.sort(files, new SortIgnoreCase());
        }

        try {
            if (!root.getCanonicalPath().equals(currentDir.getCanonicalPath())) {
                boolean rootIsParent = false;
                File parentFile = currentDir.getParentFile();
                do {
                    if (root.getCanonicalPath().equals(parentFile.getCanonicalPath()))
                        rootIsParent = true;
                    else
                        parentFile = parentFile.getParentFile();
                }
                while(parentFile != null && !rootIsParent);
                if (rootIsParent) {
                    ImportFileWrapper ifw = new ImportFileWrapper();
                    ifw.file = currentDir.getParentFile();
                    ifw.type = ImportFileWrapper.TYPE.PARENTDIRECTORY;
                    ifw.selectable = true;
                    result.add(ifw);
                }

            }
        } catch (IOException e) {
           Timber.e(e);
        }

        for (File dir : dirs) {
            ImportFileWrapper ifw = new ImportFileWrapper();
            ifw.file = dir;
            ifw.name = dir.getName();
            ifw.type = ImportFileWrapper.TYPE.DIRECTORY;
            ifw.selectable = true;
            result.add(ifw);
        }

        for (File file : files) {
            ImportFileWrapper ifw = new ImportFileWrapper();
            ifw.file = file;
            ifw.name = file.getName();
            ifw.type = ImportFileWrapper.TYPE.FILE;

            if (isFileendingTazandroid(file.getName()) || isFileendingTpaper(file.getName()))
            {

//                try {
//                    ImportMetadata metadata = ImportMetadata.parse(file);
//
//                    if (metadata == null) throw new NullPointerException();
//                    ifw.selectable = true;
//                    ifw.detail = metadata.getTitelWithDate(", ");
//                    try {
//                        Paper paper = Paper.getPaperWithBookId(getContext(), metadata.getBookId());
//                        if (paper == null) throw new Paper.PaperNotFoundException();
//                        if (paper.isDownloaded() || paper.hasUpdate() || paper.isDownloading())
//                            ifw.override = true;
//                    } catch (Paper.PaperNotFoundException ignored) {
//                    }
//                } catch (IOException | NullPointerException | ImportMetadata.NotReadableException e) {
//                    Timber.w(e,"in file %s",file.getName());
//                    ifw.selectable = false;
//                    ifw.detail = getContext().getString(R.string.import_error_detail);
//                }
//                result.add(ifw);
            }

        }
        mData = result;
        //EventBus.getDefault().postSticky(new LoadingEvent(false));
        return mData;
    }

    @Override
    public void deliverResult(List<ImportFileWrapper> data) {
           Timber.d("data: %s",data);
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<ImportFileWrapper> oldData = mData;
        mData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }
        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            releaseResources(oldData);
        }
    }

    @Override
    protected void onStartLoading() {

        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {

        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();
    }

    @Override
    protected void onReset() {

        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (mData != null) {
            releaseResources(mData);
            mData = null;
        }
    }

    @Override
    public void onCanceled(List<ImportFileWrapper> data) {
        Timber.d("data: %s",data);
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<ImportFileWrapper> data) {
        Timber.d("data: %s",data);
        // All resources associated with the Loader
        // should be released here.
    }

    private class SortIgnoreCase implements Comparator<Object> {

        public int compare(Object o1, Object o2) {
            File s1 = (File) o1;
            File s2 = (File) o2;
            return s1.getName().toLowerCase(Locale.getDefault()).compareTo(s2.getName().toLowerCase(Locale.getDefault()));
        }
    }

    FilenameFilter filter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
            boolean result = false;
            File file = new File(dir, filename);
            if (file.isDirectory())
                result = true;
            if (file.isHidden())
                result = false;
            if (!result && !file.isDirectory()) {
                result = isFileendingTazandroid(filename) || isFileendingTpaper(filename);
            }
            return result;
        }
    };

    private static boolean isFileendingTazandroid(String filename) {
        return isFilending("tazandroid",filename);
    }

    private static boolean isFileendingTpaper(String filename) {
        return isFilending("tpaper",filename);
    }

    private static boolean isFilending(String fileEnding, String filename) {
        int dotPos = filename.lastIndexOf(".");
        if (dotPos != -1) {
            String ending = filename.substring(dotPos);
            return ending.equalsIgnoreCase("." + fileEnding);
        }
        return false;
    }

    public static class ImportFileWrapper {

        public enum TYPE {DIRECTORY, FILE, PARENTDIRECTORY, WAIT}

        TYPE type;
        String name;
        File file;
        String detail;
        boolean selectable;
        boolean override;

        public String getName() {
            return name;
        }

        public String getDetail() {
            return detail;
        }

        public File getFile() {
            return file;
        }

        public TYPE getType() {
            return type;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public boolean isOverride() {
            return override;
        }
    }
}
