package de.thecode.android.tazreader.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import timber.log.Timber;

public class FileUtils {

    public static boolean deleteContents(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    success &= deleteContents(file);
                }
                if (!file.delete()) {
                    Timber.e("Failed to delete " + file);
                    success = false;
                }
            }
        }
        return success;
    }

    public static boolean deleteContentsAndDir(File dir) {
        return deleteContents(dir) && dir.delete();
    }

    public static String readFile(File file, Charset encoding) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return StreamUtils.toString(in, encoding);
        }
    }

}
