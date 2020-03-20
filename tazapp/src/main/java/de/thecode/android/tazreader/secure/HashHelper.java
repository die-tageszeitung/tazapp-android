package de.thecode.android.tazreader.secure;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Mate on 24.03.2015.
 */
public class HashHelper {

    public static final String SHA_1 = "SHA-1";

    public static final String UTF_8 = "UTF-8";

    private static String getHash(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.reset();
        return convertByteArrayToHexString(digest.digest(data));
    }

    public static String getHash(String data, String encoding, String algorithm) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return getHash(data.getBytes(encoding), algorithm);
    }

    public static String getHash(File file, String algorithm) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.reset();

        BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
        DigestInputStream digestInputStream = new DigestInputStream(is,digest);

        byte[] bytes = new byte[1024];
        //noinspection StatementWithEmptyBody
        while (digestInputStream.read(bytes) != -1) {

        }

        digestInputStream.close();

        return convertByteArrayToHexString(digest.digest());
    }

    public static boolean verifyHash(File file, String hash, String algorithm) throws IOException, NoSuchAlgorithmException {
        return hash != null && hash.equals(getHash(file, algorithm));
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16)
                                       .substring(1));
        }
        return stringBuffer.toString();
    }

}
