package de.thecode.android.tazreader.download;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.thecode.android.tazreader.utils.Log;

/**
 * Created by mate on 07.08.2015.
 */
public class UnzipStream {

    private InputStream inputStream;
    private File destinationDir;
    private boolean deleteDestinationOnFailure;
    private Progress progress;
    List<WeakReference<UnzipStreamProgressListener>> listeners = new ArrayList<>();
    private boolean canceled;

    public UnzipStream(InputStream inputStream, File destinationDir, boolean deleteDestinationOnFailure, Long totalUncompressedSize) {
        //Log.d(destinationDir);
        this.inputStream = inputStream;
        this.destinationDir = destinationDir;
        this.deleteDestinationOnFailure = deleteDestinationOnFailure;
        progress = new Progress();
        if (totalUncompressedSize == null) totalUncompressedSize = 0L;
        setTotalUncompressedSize(totalUncompressedSize);
    }

    public File start() throws IOException, UnzipCanceledException {
        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream));
            Log.t("... start working with inputstream");
            try {
                ZipEntry ze;

                while ((ze = zis.getNextEntry()) != null) {
                    if (canceled)
                        throw new UnzipCanceledException();
                    Log.d("... zipentry:", ze, ze.getSize());
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
                            notifyListeners(progress);

                            FileOutputStream fout = new FileOutputStream(destinationFile);
                            Log.d("open fileoutputstream for",destinationFile);
                            try {
                                byte[] buffer = new byte[32 * 1024]; // play with sizes..
                                int readCount;
                                while ((readCount = zis.read(buffer)) != -1) {
                                    fout.write(buffer, 0, readCount);
                                    //progress.setBytesSoFar(progress.getBytesSoFar() + readCount);
                                    publishProgress(destinationFile, readCount);
                                }
                            } finally {
                                Log.d("closing fileoutputstream");
                                fout.close();
                            }
                        }
                    }
                    zis.closeEntry();
                }
                notifyListeners(progress);

            } finally {
                Log.t("... all uncompressed bytes:", progress.getBytesSoFar());
                try {
                    Log.d("closing inputstream");
                    zis.close();
                } catch (IOException ignored) {

                }
            }
        } catch (IOException | UnzipCanceledException e) {
            onError(e);
            throw e;
        }
        onSuccess();
        return destinationDir;
    }

    public void cancel(){
        canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void onError(Exception e) throws IOException {
        Log.e(e);
        if (deleteDestinationOnFailure) {
            if (destinationDir.exists()) FileUtils.deleteDirectory(destinationDir);
                //Utils.deleteDir(destinationDir);
        }
        //Log.sendExceptionWithCrashlytics(e);
    }

    public void onSuccess() {
        Log.t("... finished unzipping stream");
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
            notifyListeners(progress);
        }
    }

    public Progress getProgress() {
        return progress;
    }


    public void setTotalUncompressedSize(long totalUncompressedSize) {
        progress.setTotalUncompressedSize(totalUncompressedSize);
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

    public void addProgressListener(UnzipStreamProgressListener listener) {
        tidyListerners();
        boolean alreadyRegistered = false;
        for (WeakReference<UnzipStreamProgressListener> weakRefListner : listeners) {
            if (weakRefListner.get() == listener) alreadyRegistered = true;
        }
        if (!alreadyRegistered) {
            listeners.add(new WeakReference<>(listener));
        }
    }

    private void tidyListerners() {
        for (int i = 0; i < listeners.size(); i++) {
            WeakReference<UnzipStreamProgressListener> weakListener = listeners.get(i);
            if (weakListener.get() == null) {
                listeners.remove(i);
                i--;
            }
        }
    }

    protected void notifyListeners(Progress progress) {
        tidyListerners();
        for (WeakReference<UnzipStreamProgressListener> weakRefListner : listeners) {
            weakRefListner.get()
                          .onProgress(progress);
        }
    }

    public File getDestinationDir() {
        return destinationDir;
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

    public interface UnzipStreamProgressListener {
        public void onProgress(Progress progress);
    }

}
