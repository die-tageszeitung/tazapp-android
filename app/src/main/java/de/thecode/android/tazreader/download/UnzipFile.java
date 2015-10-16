package de.thecode.android.tazreader.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.thecode.android.tazreader.utils.Log;

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
        return super.start();
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        Log.t("... finished unzipping file");
        StringBuilder log = new StringBuilder("... should delete source:" + deleteSourceOnSuccess);
        if (deleteSourceOnSuccess) {
            if (zipFile != null) {
                if (zipFile.exists()) {
                    if (zipFile.delete()) {
                        log.append(" - success");
                    }
                } else {
                    log.append(" - ")
                       .append(zipFile)
                       .append(" does not exist");
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
