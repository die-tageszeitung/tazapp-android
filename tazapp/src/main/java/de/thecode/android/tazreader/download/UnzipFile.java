package de.thecode.android.tazreader.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import timber.log.Timber;

/**
 * Created by mate on 07.08.2015.
 */
public class UnzipFile extends UnzipStream {

    private File zipFile;
    private boolean deleteSourceOnSuccess;

    public UnzipFile(File zipFile, File destinationDir, boolean deleteSourceOnSuccess, boolean deleteDestinationOnFailure) throws FileNotFoundException {
        super(new FileInputStream(zipFile), destinationDir, deleteDestinationOnFailure, null);
        this.zipFile = zipFile;
        this.deleteSourceOnSuccess = deleteSourceOnSuccess;
    }

    @Override
    public File start() throws IOException, UnzipCanceledException {
        Timber.i("... start extracting file %s", zipFile);
        ZipFile zf = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> e = zf.entries();
        long totalUncompressedSize = 0;
        while (e.hasMoreElements()) {
            ZipEntry ze = e.nextElement();
            totalUncompressedSize += ze.getSize();
        }
        zf.close();
        if (totalUncompressedSize > 0) setTotalUncompressedSize(totalUncompressedSize);
        Timber.i("... size uncompressed: %d", totalUncompressedSize);
        return super.start();
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        Timber.i("... finished unzipping file");
        StringBuilder logBuilder = new StringBuilder("... should delete source:" + deleteSourceOnSuccess);
        if (deleteSourceOnSuccess) {
            if (zipFile != null) {
                if (zipFile.exists()) {
                    if (zipFile.delete()) {
                        logBuilder.append(" - success");
                    }
                } else {
                    logBuilder.append(" - ")
                       .append(zipFile)
                       .append(" does not exist");
                }

            } else {
                logBuilder.append(" - zipfile null");
            }
        }
        Timber.i(logBuilder.toString());
    }

    public File getZipFile() {
        return zipFile;
    }
}
