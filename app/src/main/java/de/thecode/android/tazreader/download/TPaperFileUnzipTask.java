package de.thecode.android.tazreader.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by Mate on 27.04.2015.
 */
public class TPaperFileUnzipTask extends UnzipFileTask {

    private Paper paper;


    public TPaperFileUnzipTask(Paper paper, File zipFile, File destinationDir, boolean deleteSourceOnSuccess, boolean deleteDestinationOnFailure) throws FileNotFoundException {
        super(zipFile, destinationDir, deleteSourceOnSuccess, deleteDestinationOnFailure);
        this.paper = paper;
    }

    public Paper getPaper() {
        return paper;
    }

    @Override
    public File doInBackgroundWithException(Object... params) throws Exception {
        getProgress().setProgressPercentageMax(50);
        File destinationDir = super.doInBackgroundWithException(params);
        getProgress().setPercentage(0);
        getProgress().setOffset(50);

        Log.t("... start parsing plist to check hashvals.");
        if (!destinationDir.exists() || !destinationDir.isDirectory())
            throw new FileNotFoundException("Directory not found");
        File plistFile = new File(destinationDir, Paper.CONTENT_PLIST_FILENAME);
        if (!plistFile.exists())
            throw new FileNotFoundException("Plist not found");
        paper.parsePlist(plistFile, false);
        Map<String, String> hashVals = paper.getPlist()
                                            .getHashVals();
        if (hashVals == null) {
            Log.w("No hash values found in Plist");
            return destinationDir;
        }

        int count = 0;
        for (Map.Entry<String, String> entry : hashVals.entrySet()) {
            count++;
            getProgress().setPercentage((count*100)/hashVals.size());
            publishProgress(getProgress());
            File checkFile = new File(destinationDir, entry.getKey());
            if (!checkFile.exists())
                throw new FileNotFoundException(checkFile.getName() + " not found");
            else {
                try {
                    if (!HashHelper.verifyHash(checkFile, entry.getValue(), HashHelper.SHA_1))
                        throw new FileNotFoundException("Wrong hash for file " + checkFile.getName());
                } catch (NoSuchAlgorithmException e) {
                    Log.w(e);
                }
            }
        }
        Log.t("... finished");
        return destinationDir;
    }

    @Override
    public void onPostError(Exception exception, File sourceZipFile) {

    }

}
