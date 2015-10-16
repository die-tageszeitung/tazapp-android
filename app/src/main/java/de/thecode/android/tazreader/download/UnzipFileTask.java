package de.thecode.android.tazreader.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.thecode.android.tazreader.utils.Log;

/**
 * Created by Mate on 27.04.2015.
 */
public abstract class UnzipFileTask extends UnzipStreamTask {

    private File zipFile;
    private boolean deleteSourceOnSuccess;

    public UnzipFileTask(File zipFile, File destinationDir, boolean deleteSourceOnSuccess, boolean deleteDestinationOnFailure) throws FileNotFoundException {
        super(new FileInputStream(zipFile), destinationDir, deleteDestinationOnFailure, null);
        this.zipFile = zipFile;
        this.deleteSourceOnSuccess = deleteSourceOnSuccess;
    }

    @Override
    public File doInBackgroundWithException(Object... params) throws Exception {
        Log.t("... start extracting file", zipFile);
        ZipFile zf = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> e = zf.entries();
        long totalUncompressedSize = 0;
        while (e.hasMoreElements()) {
            ZipEntry ze = e.nextElement();
            totalUncompressedSize += ze.getSize();
        }
        zf.close();
        if (totalUncompressedSize > 0) setTotalUncompressedSize(totalUncompressedSize);
        Log.t("... size uncompressed: " + totalUncompressedSize);
        return super.doInBackgroundWithException(params);
    }

    @Override
    protected final void onPostError(Exception exception) {
        super.onPostError(exception);
        onPostError(exception, getZipFile());
    }

    public abstract void onPostError(Exception exception, File sourceZipFile);

    @Override
    protected void onPostSuccess(File destinationFile) {
        super.onPostSuccess(destinationFile);
        Log.t("... finished unzipping file");
        StringBuilder log = new StringBuilder("... should delete source:" + deleteSourceOnSuccess);
        if (deleteSourceOnSuccess) {
            if (zipFile != null) {
                if (zipFile.exists()) {
                    if (zipFile.delete()) {
                        log.append(" - success");
                    }
                }
                else {
                    log.append(" - ").append(zipFile).append(" does not exist");
                }

            } else {
                log.append(" - zipfile null");
            }
        }
        Log.t(log);
    }

    public File getZipFile() {
        return zipFile;
    }
}
