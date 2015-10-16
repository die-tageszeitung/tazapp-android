package de.thecode.android.tazreader.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import java.util.Map;

import de.thecode.android.tazreader.secure.SimpleCrypto;
import de.thecode.android.tazreader.utils.Log;

public final class TazSettings {

    public static final class PREFKEY {

        public static final String FONTSIZE = "fontsize";
        public static final String COLSIZE = "colsize";
        public static final String THEME = "theme";
        public static final String ISFOOT = "isFoot";
        public static final String FULLSCREEN = "FullScreen";
        public static final String AUTOLOAD = "autoload";
        public static final String AUTOLOAD_WIFI = "autoload_wifi";
        public static final String AUTODELETE = "autodelete";
        public static final String AUTODELETE_DAYS = "autodeleteDays";
        public static final String LASTAUTOLOAD = "lastautoload";
        public static final String CONTENTVERBOSE = "ContentVerbose";
        public static final String KEEPSCREEN = "KeepScreen";
        public static final String ORIENTATION = "Orientation";
        public static final String LASTACTIVITY = "lastActivity";
        public static final String LASTOPENPAPER = "lastOpenPaper";
        public static final String LASTVERSION = "lastVersion";
        public static final String FISRTSTART = "firstStart";
//        public static final String PAGING = "paging";
        public static final String ISSCROLL = "isScroll";
        public static final String RINGTONE = "ringtone";
        public static final String VIBRATE = "vibrate";
        public static final String NAVDRAWERLEARNED = "navdrawerlearned";
        public static final String FORCESYNC = "forcesync";
        public static final String PAPERMIGRATEFROM = "paperMigrateFrom";
//        public static final String PAPERMIGRATERUNNING = "paperMigrateRunning";
        public static final String PAPERMIGRATEDIDS = "paperMigratedIds";
        public static final String PAPERNOTIFICATIONIDS = "paperNotificationIds";
        public static final String ISSOCIAL = "isSocial";
        public static final String PAGEINDEXBUTTON = "pageIndexButton";

    }

    public static boolean setPref(Context context, String key, Object v) {
        
        SharedPreferences prefs = getSharedPreferences(context);
        Editor prefEdit = prefs.edit();
        

        
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
        Log.d(key,v,result);
        return result;
    }

    public static String getPrefString(Context context, String key, String defValue) throws ClassCastException {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getString(key, defValue);
    }

    public static String getDecryptedPrefString(Context context, String key, String defValue) throws ClassCastException {
        String result = getPrefString(context, key, null);
        if (result != null)
            return SimpleCrypto.decrypt(result);
        return defValue;
    }

    public static long getPrefLong(Context context, String key, long defValue) throws ClassCastException {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getLong(key, defValue);
    }

    public static int getPrefInt(Context context, String key, int defValue) throws ClassCastException {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getInt(key, defValue);
    }

    public static boolean getPrefBoolean(Context context, String key, boolean defValue) throws ClassCastException {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getBoolean(key, defValue);
    }

    public static float getPrefFloat(Context context, String key, float defValue) throws ClassCastException {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getFloat(key, defValue);
    }

    public static Map<String, ?> getPrefAll(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getAll();
    }

    public static void setDefaultPref(Context context, String key, Object v) {
        SharedPreferences prefs = getSharedPreferences(context);
        if (!prefs.contains(key)) {
            setPref(context, key, v);
        }
    }
    
    public static boolean removePref(Context context, String key) {
        SharedPreferences prefs = getSharedPreferences(context);
        Editor prefEdit = prefs.edit();
        prefEdit.remove(key);
        boolean result = prefEdit.commit();
        Log.d(key,result);
        return result;
    }

    public static SharedPreferences getSharedPreferences(Context context)
    {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context), getDefaultSharedPreferencesMode());
    }
    
    private static String getDefaultSharedPreferencesName(Context context) {
        return context.getApplicationContext().getPackageName() + "_preferences";
    }
    
    @SuppressLint("InlinedApi")
    private static int getDefaultSharedPreferencesMode() {
        if (Build.VERSION.SDK_INT >= 11)
            return Context.MODE_MULTI_PROCESS;
        else
            return Context.MODE_PRIVATE;
    }
    
    public static Uri getRingtone(Context context) {
        Uri result = null;
        String ringtoneUri = getPrefString(context, PREFKEY.RINGTONE, null);
        if (ringtoneUri == null) {
            result = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            if (ringtoneUri.equals(""))
                return null;
            result = Uri.parse(ringtoneUri);
        }
        return result;
    }
    

}
