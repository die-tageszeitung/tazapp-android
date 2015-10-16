package de.thecode.android.tazreader.download;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by mate on 07.08.2015.
 */
public class UnzipResource {

    private static final String RESOURCE_SHA1_PLIST_FILENAME = "sha1.plist";
    private static final String KEY_HASHVALS = "HashVals";


    private File destinationDir;
    private UnzipFile unzipFile;

    public UnzipResource(File zipFile, File destinationDir, boolean deleteSourceOnSuccess) throws FileNotFoundException {
        this.destinationDir = destinationDir;
        this.unzipFile = new UnzipFile(zipFile, destinationDir, deleteSourceOnSuccess, true);
    }

    public File start() throws IOException, ParserConfigurationException, ParseException, SAXException, PropertyListFormatException, UnzipStream.UnzipCanceledException {
        File result = unzipFile.start();
        File sha1File = new File(destinationDir, RESOURCE_SHA1_PLIST_FILENAME);
        if (!sha1File.exists()) throw new FileNotFoundException("Sha1 File not found");
        try {
            Log.t("... start parsing sha1 plist to check hashvals for ressources.");
            NSDictionary root = (NSDictionary) PropertyListParser.parse(sha1File);
            NSDictionary hashValsDict = (NSDictionary) root.objectForKey(KEY_HASHVALS);
            Set<Map.Entry<String, NSObject>> set = hashValsDict.entrySet();
            for (Map.Entry<String, NSObject> hashVal : set) {
                File checkFile = new File(destinationDir, hashVal.getKey());
                if (!checkFile.exists()) throw new FileNotFoundException(checkFile.getName() + " not found");
                else {
                    try {
                        if (!HashHelper.verifyHash(checkFile, ((NSString) hashVal.getValue()).getContent(), HashHelper.SHA_1))
                            throw new FileNotFoundException("Wrong hash for file " + checkFile.getName());
                    } catch (NoSuchAlgorithmException e) {
                        Log.w(e);
                    }
                }
            }
        } catch (UnsupportedOperationException e) {
            Log.w(e);
        }
        return result;
    }


}
