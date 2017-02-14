package de.thecode.android.tazreader.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.scottyab.aescrypt.AESCrypt;

import de.thecode.android.tazreader.secure.SimpleCrypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.Map;

public final class TazSettings {

    private static final Logger log = LoggerFactory.getLogger(TazSettings.class);

    public static final class PREFKEY {

        public static final String FONTSIZE                  = "fontsize";
        public static final String COLSIZE                   = "colsize";
        public static final String THEME                     = "theme";
        public static final String ISFOOT                    = "isFoot";
        public static final String FULLSCREEN                = "FullScreen";
        public static final String AUTOLOAD                  = "autoload";
        public static final String AUTOLOAD_WIFI             = "autoload_wifi";
        public static final String AUTODELETE                = "autodelete";
        public static final String AUTODELETE_VALUE          = "autodeleteDays";
        public static final String LASTAUTOLOAD              = "lastautoload";
        public static final String CONTENTVERBOSE            = "ContentVerbose";
        public static final String KEEPSCREEN                = "KeepScreen";
        public static final String ORIENTATION               = "Orientation";
        public static final String LASTACTIVITY              = "lastActivity";
        public static final String LASTOPENPAPER             = "lastOpenPaper";
        public static final String LASTVERSION               = "lastVersion";
        public static final String FISRTSTART                = "firstStart";
        //        public static final String PAGING = "paging";
        public static final String ISSCROLL                  = "isScroll";
        public static final String RINGTONE                  = "ringtone";
        public static final String VIBRATE                   = "vibrate";
        public static final String NAVDRAWERLEARNED          = "navdrawerlearned";
        public static final String FORCESYNC                 = "forcesync";
        public static final String PAPERMIGRATEFROM          = "paperMigrateFrom";
        //        public static final String PAPERMIGRATERUNNING = "paperMigrateRunning";
        public static final String PAPERMIGRATEDIDS          = "paperMigratedIds";
        public static final String PAPERNOTIFICATIONIDS      = "paperNotificationIds";
        public static final String ISSOCIAL                  = "isSocial";
        public static final String PAGEINDEXBUTTON           = "pageIndexButton";
        public static final String TEXTTOSPEACH              = "textToSpeech";
        public static final String USER                      = "user";
        public static final String PASS                      = "pass";
        public static final String USERMIGRATIONNOTIFICATION = "usermigrationnotification";
    }


    private static TazSettings instance;
    private SharedPreferences sharedPreferences;

    public static synchronized TazSettings getInstance(Context context) {
        if (instance == null) instance = new TazSettings(context.getApplicationContext());
        return instance;
    }

    private TazSettings(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean setPref(String key, Object v) {


        Editor prefEdit = sharedPreferences.edit();


        if (v instanceof Boolean) {
            prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
        } else if (v instanceof Float) {
            prefEdit.putFloat(key, ((Float) v).floatValue());
        } else if (v instanceof Integer) {
            prefEdit.putInt(key, ((Integer) v).intValue());
        } else if (v instanceof Long) {
            prefEdit.putLong(key, ((Long) v).longValue());
        } else if (v instanceof String) {
            prefEdit.putString(key, ((String) v));
        } else if (v instanceof Uri) {
            prefEdit.putString(key, ((Uri) v).toString());
        }

        boolean result = prefEdit.commit();
        log.debug("{} {} {}", key, v, result);
        return result;
    }

    public String getPrefString(String key, String defValue) throws ClassCastException {
        return sharedPreferences.getString(key, defValue);
    }

    public String getOldDecryptedPrefString(String key, String defValue) throws ClassCastException {
        String result = getPrefString(key, null);
        if (result != null) return SimpleCrypto.decrypt(result);
        return defValue;
    }

    public String getDecrytedPrefString(String password, String key, String defValue) {
        String result = getPrefString(key, null);
        if (result != null) {
            try {
                return AESCrypt.decrypt(password, result);
            } catch (GeneralSecurityException e) {
                return result;
            }
        }
        return defValue;
    }

    public void setEncrytedPrefString(String password, String key, String value) {
        try {
            value = AESCrypt.encrypt(password, value);
        } catch (GeneralSecurityException ignored) {
        }
        setPref(key, value);
    }

    public long getPrefLong(String key, long defValue) {
        return sharedPreferences.getLong(key, defValue);
    }

    public int getPrefInt(String key, int defValue) {
        return sharedPreferences.getInt(key, defValue);
    }

    public boolean getPrefBoolean(String key, boolean defValue) throws ClassCastException {
        return sharedPreferences.getBoolean(key, defValue);
    }

    public float getPrefFloat(String key, float defValue) throws ClassCastException {
        return sharedPreferences.getFloat(key, defValue);
    }

    public Map<String, ?> getPrefAll() {
        return sharedPreferences.getAll();
    }

    public void setDefaultPref(String key, Object v) {
        if (!sharedPreferences.contains(key)) {
            setPref(key, v);
        }
    }

    public void removePref(String key) {
        Editor prefEdit = sharedPreferences.edit();
        prefEdit.remove(key);
        prefEdit.apply();
        log.debug("{}", key);
    }

    public Uri getRingtone() {
        Uri result;
        String ringtoneUri = getPrefString(PREFKEY.RINGTONE, null);
        if (ringtoneUri == null) {
            result = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            if (ringtoneUri.equals("")) return null;
            result = Uri.parse(ringtoneUri);
        }
        return result;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public boolean hasPreference(String key) {
        return sharedPreferences.contains(key);
    }
}
