package de.thecode.android.tazreader.download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;
import de.thecode.android.tazreader.utils.Log;
import de.thecode.android.tazreader.utils.Utils;

/**
 * Created by Mate on 27.04.2015.
 */
public class UnzipStreamTask extends AsyncTaskWithExecption<Object, UnzipStreamTask.Progress, File> {

    private InputStream inputStream;
    private File destinationDir;
    private boolean deleteDestinationOnFailure;
    private Progress progress;


    public UnzipStreamTask(InputStream inputStream, File destinationDir, boolean deleteDestinationOnFailure, Long totalUncompressedSize) {
        //Log.d(destinationDir);
        this.inputStream = inputStream;
        this.destinationDir = destinationDir;
        this.deleteDestinationOnFailure = deleteDestinationOnFailure;
        progress = new Progress();
        if (totalUncompressedSize == null) totalUncompressedSize = 0L;
        setTotalUncompressedSize(totalUncompressedSize);

    }

    public Progress getProgress() {
        return progress;
    }

    public void setTotalUncompressedSize(long totalUncompressedSize) {
        progress.setTotalUncompressedSize(totalUncompressedSize);
    }

    @Override
    public File doInBackgroundWithException(Object... params) throws Exception {
        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream));
            Log.t("... start working with inputstream");
            try {
                ZipEntry ze;

                while ((ze = zis.getNextEntry()) != null) {
                    Log.t("... zipentry:",ze,ze.getSize());
                    if (ze.isDirectory()) {
                        File zipDir = new File(destinationDir, ze.getName());
                        if (dirHelper(zipDir)) {
                            if (progress != null) {
                                //                        progress.setCurrentFile(zipDir);
                                //                        publishProgress(progress);
                                publishProgress(zipDir, 0);
                            }
                        }
                    } else {
                        File destinationFile = new File(destinationDir, ze.getName());
                        if (dirHelper(destinationFile.getParentFile())) {
                            progress.setCurrentFile(destinationFile);
                            publishProgress(progress);

                            FileOutputStream fout = new FileOutputStream(destinationFile);
                            try {
                                byte[] buffer = new byte[32 * 1024]; // play with sizes..
                                int readCount;
                                while ((readCount = zis.read(buffer)) != -1) {
                                    fout.write(buffer, 0, readCount);
                                    //progress.setBytesSoFar(progress.getBytesSoFar() + readCount);
                                    publishProgress(destinationFile, readCount);
                                }
                            } finally {
                                fout.close();
                            }
                        }
                    }
                    zis.closeEntry();
                }
                publishProgress(progress);
            } finally {
                Log.t("... all uncompressed bytes:",progress.getBytesSoFar());
                try {
                    zis.close();
                } catch (IOException ignored) {

                }
            }
        } catch (Exception e) {
            Log.sendExceptionWithCrashlytics(e);
            throw e;
        }
        return destinationDir;
    }

    File lastFilePublished;
    int lastPercentagePublished = -1;

    private void publishProgress(File currentFile, int readCount) {
        progress.setBytesSoFar(progress.getBytesSoFar() + readCount);
        progress.setCurrentFile(currentFile);
        if (progress.totalUncompressedSize != 0) progress.percentage = (int) ((progress.bytesSoFar * 100) / progress.totalUncompressedSize);
        if (progress.getPercentage() != lastPercentagePublished || progress.getCurrentFile() != lastFilePublished) {
            lastPercentagePublished = progress.getPercentage();
            lastFilePublished = currentFile;
            publishProgress(progress);
        }
    }

    @Override
    protected void onPostError(Exception exception) {
        if (deleteDestinationOnFailure) {
            if (destinationDir.exists()) Utils.deleteDir(destinationDir);
        }
    }

    @Override
    protected void onPostSuccess(File file) {
        Log.t("... finished unzipping stream");
    }

    private boolean dirHelper(File directory) throws IOException {

        if (directory.exists()) {
            if (directory.isDirectory()) return true;
            else {
                boolean resultDelete = directory.delete();
                if (!resultDelete) throw new IOException("Cannot delete file " + directory);
                else return dirHelper(directory);
            }
        } else {
            return directory.mkdirs();
        }
    }

    public static class Progress {
        private long totalUncompressedSize = 0;
        private long bytesSoFar = 0;
        private File currentFile;
        private int progressPercentageMax = 100;
        private int percentage = 0;
        private int offset = 0;

        public Progress() {

        }

        public int getPercentage() {
            return offset + ((percentage * progressPercentageMax) / 100);

            //            if (totalUncompressedSize == 0)
            //                return 0;
            //            return (int) ((bytesSoFar * 100) / totalUncompressedSize);
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public int getOffset() {
            return offset;
        }

        public void setProgressPercentageMax(int progressPercentageMax) {
            this.progressPercentageMax = progressPercentageMax;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }

        public int getProgressPercentageMax() {
            return progressPercentageMax;
        }

        public long getTotalUncompressedSize() {
            return totalUncompressedSize;
        }

        public void setTotalUncompressedSize(long totalUncompressedSize) {
            this.totalUncompressedSize = totalUncompressedSize;
        }

        public long getBytesSoFar() {
            return bytesSoFar;
        }

        public File getCurrentFile() {
            return currentFile;
        }

        public void setBytesSoFar(long bytesSoFar) {
            this.bytesSoFar = bytesSoFar;
        }

        public void setCurrentFile(File currentFile) {
            this.currentFile = currentFile;
        }
    }


}
