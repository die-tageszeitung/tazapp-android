
package de.thecode.android.tazreader.secure;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SimpleCrypto {

    private static final String PREFIX = "TC#";
    private static String CHARSETNAME = "UTF-8";

    public static String encrypt(String value) {
        if (value == null)
            return null;
        if (value.length() == 0)
            return value;
        value = PREFIX + xor_encrypt(value, getS());
        return value;
    }

    public static String decrypt(String value) {
        if (value == null)
            return null;
        if (value.length() == 0)
            return value;
        if (value.startsWith(PREFIX))
        {
            value = value.substring(PREFIX.length(), value.length());
            value =xor_decrypt(value, getS()); 
            return value;
        }
        else
        {
            try {
                value = innerdecrypt(value);
            } catch (Exception e) {
                return null;
            }
            return value;
        }
    }

    private static String innerdecrypt(String encrypted) throws Exception {
        byte[] rawKey = getRawKey(getS().getBytes());
        byte[] enc = toByte(encrypted);
        byte[] result = decrypt(rawKey, enc);
        return new String(result);
    }

    private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
        sr.setSeed(seed);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }

    private static Cipher getCipher() throws Exception
    {
        return Cipher.getInstance("AES");
    }


    private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = getCipher();
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        return result;
    }

    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    private final static String HEX = "0123456789ABCDEF";

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }

    private static String getS()
    {
        return "FD9221334DFE22222FDECAAAB123EE231";
    }

    public static String xor_encrypt(String message, String key) {
        try {
            if (message == null || key == null)
                return null;

            byte[] keys = key.getBytes(CHARSETNAME);
            byte[] mesg = message.getBytes(CHARSETNAME);

            int ml = mesg.length;
            int kl = keys.length;
            byte[] newmsg = new byte[ml];

            for (int i = 0; i < ml; i++) {
                newmsg[i] = (byte) (mesg[i] ^ keys[i % kl]);
            }
            mesg = null;
            keys = null;
            return toHex(newmsg);
        } catch (Exception e) {
            return null;
        }
    }

    public static String xor_decrypt(String message, String key) {
        try {
            if (message == null || key == null)
                return null;
            byte[] keys = key.getBytes(CHARSETNAME);
            byte[] mesg = toByte(message);

            int ml = mesg.length;
            int kl = keys.length;
            byte[] newmsg = new byte[ml];

            for (int i = 0; i < ml; i++) {
                newmsg[i] = (byte) (mesg[i] ^ keys[i % kl]);
            }
            mesg = null;
            keys = null;
            return new String(newmsg, CHARSETNAME);
        } catch (Exception e) {
            return null;
        }
    }

}
