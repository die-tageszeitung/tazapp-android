package de.thecode.android.tazreader.download;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by Mate on 27.04.2015.
 */
public class ResourceFileUnzipTask extends UnzipFileTask {

    private static final String RESOURCE_SHA1_PLIST_FILENAME = "sha1.plist";
    private static final String KEY_HASHVALS = "HashVals";


    private Resource resource;

    public ResourceFileUnzipTask(Resource resource, File zipFile, File destinationDir, boolean deleteSourceOnSuccess, boolean deleteDestinationOnFailure) throws FileNotFoundException {
        super(zipFile, destinationDir, deleteSourceOnSuccess, deleteDestinationOnFailure);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    @Override
    public File doInBackgroundWithException(Object... params) throws Exception {
        File destinationDir = super.doInBackgroundWithException(params);
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
        Log.t("... finished");
        return destinationDir;
    }

    @Override
    public void onPostError(Exception exception, File sourceZipFile) {

    }
}
