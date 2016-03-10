package de.thecode.android.tazreader.download;

import java.io.File;
import java.io.FileNotFoundException;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;

/**
 * Created by mate on 10.08.2015.
 */
public abstract class UnzipPaperTask extends AsyncTaskWithExecption<Object, UnzipStream.Progress, File> implements UnzipStream.UnzipStreamProgressListener {

    private UnzipPaper unzipPaper;

    public UnzipPaperTask(Paper paper, File zipFile, File destinationDir, boolean deleteSourceOnSuccess) throws FileNotFoundException {
        unzipPaper = new UnzipPaper(paper, zipFile,destinationDir, deleteSourceOnSuccess);
        unzipPaper.getUnzipFile().addProgressListener(this);
    }


    @Override
    public File doInBackgroundWithException(Object... params) throws Exception {
        return unzipPaper.start();
    }

    @Override
    protected void onPostError(Exception exception) {
        onPostError(exception, unzipPaper.getUnzipFile().getZipFile());
    }

    protected abstract void onPostError(Exception exception, File sourceZipFile);

    @Override
    protected void onPostSuccess(File file) {

    }

    @Override
    public void onProgress(UnzipStream.Progress progress) {
        publishProgress(progress);
    }

    public UnzipPaper getUnzipPaper() {
        return unzipPaper;
    }
}
