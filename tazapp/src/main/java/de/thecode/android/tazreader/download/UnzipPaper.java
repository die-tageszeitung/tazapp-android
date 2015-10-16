package de.thecode.android.tazreader.download;

import com.dd.plist.PropertyListFormatException;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by mate on 07.08.2015.
 */
public class UnzipPaper {

    private Paper paper;
    private File destinationDir;
    private UnzipFile unzipFile;

    public UnzipPaper(Paper paper, File zipFile, File destinationDir, boolean deleteSourceOnSuccess) throws FileNotFoundException {
        this.paper = paper;
        this.destinationDir = destinationDir;
        this.unzipFile = new UnzipFile(zipFile, destinationDir, deleteSourceOnSuccess, true);
    }

    public File start() throws PropertyListFormatException, ParserConfigurationException, SAXException, ParseException, IOException, UnzipCanceledException {
        unzipFile.getProgress()
                 .setProgressPercentageMax(50);
        File result = unzipFile.start();
        unzipFile.getProgress()
                 .setOffset(50);
        checkCanceled();
        Log.t("... start parsing plist to check hashvals.");
        if (!destinationDir.exists() || !destinationDir.isDirectory()) throw new FileNotFoundException("Directory not found");
        File plistFile = new File(destinationDir, Paper.CONTENT_PLIST_FILENAME);
        if (!plistFile.exists()) throw new FileNotFoundException("Plist not found");
        paper.parsePlist(plistFile, false);
        Map<String, String> hashVals = paper.getPlist()
                                            .getHashVals();
        if (hashVals != null) {
            int count = 0;
            for (Map.Entry<String, String> entry : hashVals.entrySet()) {
                checkCanceled();
                count++;
                unzipFile.getProgress()
                         .setPercentage((count * 100) / hashVals.size());
                unzipFile.notifyListeners(unzipFile.getProgress());
                File checkFile = new File(destinationDir, entry.getKey());
                if (!checkFile.exists()) throw new FileNotFoundException(checkFile.getName() + " not found");
                else {
                    try {
                        if (!HashHelper.verifyHash(checkFile, entry.getValue(), HashHelper.SHA_1))
                            throw new FileNotFoundException("Wrong hash for file " + checkFile.getName());
                    } catch (NoSuchAlgorithmException e) {
                        Log.w(e);
                    }
                }
            }
        } else Log.w("No hash values found in Plist");
        checkCanceled();
        Log.t("... finished");
        return result;
    }

    public UnzipFile getUnzipFile() {
        return unzipFile;
    }

    public Paper getPaper() {
        return paper;
    }

    public void cancel() {
        unzipFile.cancel();
    }

    private void checkCanceled() throws UnzipCanceledException, IOException {
        if (unzipFile.isCanceled()) {
            UnzipCanceledException e = new UnzipCanceledException();
            unzipFile.onError(e);
            throw e;
        }
    }



}
