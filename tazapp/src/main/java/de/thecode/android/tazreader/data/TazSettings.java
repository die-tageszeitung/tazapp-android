package de.thecode.android.tazreader.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.crashlytics.android.core.CrashlyticsCore;
import com.scottyab.aescrypt.AESCrypt;

import de.thecode.android.tazreader.secure.SimpleCrypto;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public final class TazSettings implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final class PREFKEY {

        public static final  String FONTSIZE                    = "fontsize";
        public static final  String COLSIZE                     = "colsize";
        public static final  String THEME                       = "theme";
        public static final  String ISFOOT                      = "isFoot";
        public static final  String FULLSCREEN                  = "FullScreen";
        public static final  String AUTOLOAD                    = "autoload";
        public static final  String AUTOLOAD_WIFI               = "autoload_wifi";
        public static final  String AUTODELETE                  = "autodelete";
        public static final  String AUTODELETE_VALUE            = "autodeleteDays";
        public static final  String LASTAUTOLOAD                = "lastautoload";
        public static final  String CONTENTVERBOSE              = "ContentVerbose";
        public static final  String KEEPSCREEN                  = "KeepScreen";
        public static final  String ORIENTATION                 = "Orientation";
        public static final  String LASTACTIVITY                = "lastActivity";
        public static final  String LASTOPENPAPER               = "lastOpenPaper";
        public static final  String LASTVERSION                 = "lastVersion";
        public static final  String FISRTSTART                  = "firstStart";
        //        public static final String PAGING = "paging";
        public static final  String ISSCROLL                    = "isScroll";
        public static final  String NOTIFICATION_SOUND_DOWNLOAD = "ringtone";
        public static final  String NOTIFICATION_SOUND_PUSH     = "ringtone_push";
        public static final  String VIBRATE                     = "vibrate";
        public static final  String NAVDRAWERLEARNED            = "navdrawerlearned";
        // public static final  String FORCESYNC                   = "forcesync";
        public static final  String PAPERMIGRATEFROM            = "paperMigrateFrom";
        //        public static final String PAPERMIGRATERUNNING = "paperMigrateRunning";
        public static final  String PAPERMIGRATEDIDS            = "paperMigratedIds";
        public static final  String PAPERNOTIFICATIONIDS        = "paperNotificationIds";
        public static final  String ISSOCIAL                    = "isSocial";
        public static final  String PAGEINDEXBUTTON             = "pageIndexButton";
        private static final String INDEXBUTTON                 = "indexButton";
        public static final  String TEXTTOSPEACH                = "textToSpeech";
        public static final  String USER                        = "user";
        public static final  String PASS                        = "pass";
        public static final  String USERMIGRATIONNOTIFICATION   = "usermigrationnotification";
        // private static final String SYNCSERVICENEXTRUN          = "syncServiceNextRun";
        public static final  String DEMOMODE                    = "demoMode";
        public static final  String ISCHANGEARTICLE             = "isChangeArtikel";
        public static final  String ISPAGING                    = "isPaging";
        public static final  String ISJUSTIFY                   = "isJustify";
        public static final  String ISSCROLLTONEXT              = "isScrollToNext";
        public static final  String PAGETAPTOARTICLE            = "pageTapToArticle";
        public static final  String PAGEDOUBLETAPZOOM           = "pageDoubleTapZoom";
        public static final String PAGETAPBORDERTOTURN         = "pageTapBorderToTurn";
        private static final String INDEXALWAYSEXPANDED         = "indexAlwaysExpanded";
        public static final  String FIREBASETOKEN               = "firebaseToken";
        private static final String FIREBASETOKENOLD            = "firebaseTokenOld";
        public static final  String NOTIFICATION_PUSH           = "notification_push";
        public static final  String CRASHLYTICS_ALWAYS_SEND     = "always_send_reports_opt_in";
    }


    private static TazSettings       instance;
    private        SharedPreferences sharedPreferences;
    private        SharedPreferences crashlyticsSharedPreferences;

    public static synchronized TazSettings getInstance(Context context) {
        if (instance == null) instance = new TazSettings(context.getApplicationContext());
        return instance;
    }

    private TazSettings(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        crashlyticsSharedPreferences = context.getSharedPreferences("com.crashlytics.sdk.android.crashlytics-core:"+ CrashlyticsCore.class.getName(), Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (changeListeners.containsKey(key) && changeListeners.get(key) != null) {
            Object value = sharedPreferences.getAll()
                                            .get(key);
            for (OnPreferenceChangeListener listener : changeListeners.get(key)) {
                if (listener != null) {
                    listener.onPreferenceChanged(value);
                }
            }
        }
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
        Timber.d("key %s %s %s", key, v, result);
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

    public void setDefaultPref(String key, Object v) {
        if (!sharedPreferences.contains(key)) {
            setPref(key, v);
        }
    }

    public void removePref(String key) {
        Editor prefEdit = sharedPreferences.edit();
        prefEdit.remove(key);
        prefEdit.apply();
        Timber.d("key %s", key);
    }

    public void setNotificationSoundUri(String key, Uri uri) {
        if (TextUtils.isEmpty(key)) return;
        String uriString = uri == null ? "" : uri.toString();
        sharedPreferences.edit()
                         .putString(key, uriString)
                         .apply();
//        if (key.equals(PREFKEY.NOTIFICATION_SOUND_PUSH)) {
//            PushRestApiJob.scheduleJob();
//        }

    }


    public Uri getNotificationSoundUri(String key) {
        String soundUri = sharedPreferences.getString(key, null);
        if ("".equals(soundUri)) return null;
        try {
            return Uri.parse(soundUri);
        } catch (Exception e) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
    }

    public String getFirebaseToken() {
        return sharedPreferences.getString(PREFKEY.FIREBASETOKEN, "");
    }

    public String getOldFirebaseToken() {
        return sharedPreferences.getString(PREFKEY.FIREBASETOKENOLD, null);
    }

    public void removeOldToken() {
        sharedPreferences.edit()
                         .remove(PREFKEY.FIREBASETOKENOLD)
                         .apply();
    }

    public void setFirebaseToken(String token) {
        String oldToken = getFirebaseToken();
        Editor editor = sharedPreferences.edit();
        if (oldToken != null) {
            editor.putString(PREFKEY.FIREBASETOKENOLD, oldToken);
        }
        editor.putString(PREFKEY.FIREBASETOKEN, token)
              .apply();
        //PushRestApiJob.scheduleJob();
    }

    public void setCrashlyticsAlwaysSend(boolean alwaysSend) {
        crashlyticsSharedPreferences.edit()
                                    .putBoolean(PREFKEY.CRASHLYTICS_ALWAYS_SEND, alwaysSend)
                                    .apply();
    }

    public boolean getCrashlyticsAlwaysSend() {
        return crashlyticsSharedPreferences.getBoolean(PREFKEY.CRASHLYTICS_ALWAYS_SEND, false);
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public boolean hasPreference(String key) {
        return sharedPreferences.contains(key);
    }


//    public long getSyncServiceNextRun() {
//        return sharedPreferences.getLong(PREFKEY.SYNCSERVICENEXTRUN, 0);
//    }
//
//    public void setSyncServiceNextRun(long timeInMillis) {
//        sharedPreferences.edit()
//                         .putLong(PREFKEY.SYNCSERVICENEXTRUN, timeInMillis)
//                         .apply();
//    }

    public boolean isDemoMode() {
        return sharedPreferences.getBoolean(PREFKEY.DEMOMODE, true);
    }

    public void setDemoMode(boolean demoMode) {
        sharedPreferences.edit()
                         .putBoolean(PREFKEY.DEMOMODE, demoMode)
                         .apply();
    }

    public boolean isIndexAlwaysExpanded() {
        return sharedPreferences.getBoolean(PREFKEY.INDEXALWAYSEXPANDED, false);
    }

    public void setIndexAlwaysExpanded(boolean isAlwaysExpanded) {
        sharedPreferences.edit()
                         .putBoolean(PREFKEY.INDEXALWAYSEXPANDED, isAlwaysExpanded)
                         .apply();
    }

    public boolean isTapBorderToTurnPage() {
        return sharedPreferences.getBoolean(PREFKEY.PAGETAPBORDERTOTURN, true);
    }

    public void setTapBorderToTurnPage(boolean tapToTurn) {
        sharedPreferences.edit()
                         .putBoolean(PREFKEY.PAGETAPBORDERTOTURN, tapToTurn)
                         .apply();
    }

    public boolean isIndexButton() {
        return sharedPreferences.getBoolean(PREFKEY.INDEXBUTTON, false);
    }

    public void setIndexButton(boolean show) {
        sharedPreferences.edit()
                         .putBoolean(PREFKEY.INDEXBUTTON, show)
                         .apply();
    }

    private Map<String, List<OnPreferenceChangeListener>> changeListeners = new HashMap<>();

    public void addOnPreferenceChangeListener(String key, OnPreferenceChangeListener listener) {
        if (changeListeners.containsKey(key)) {
            List<OnPreferenceChangeListener> listenerList = changeListeners.get(key);
            int listSize = listenerList.size();
            for (int i = 0; i < listSize; i++) {
                if ((listenerList.get(i) != null && listenerList.get(i) == listener)) {
                    listenerList.remove(i);
                    listSize--;
                    i--;
                }
            }
            listenerList.add(listener);
            changeListeners.put(key, listenerList);
        } else {
            List<OnPreferenceChangeListener> newListenerList = new ArrayList<>();
            newListenerList.add(listener);
            changeListeners.put(key, newListenerList);
        }
    }

    public void removeOnPreferenceChangeListener(OnPreferenceChangeListener listener) {
        Set<String> keys = changeListeners.keySet();
        for (String key : keys) {
            List<OnPreferenceChangeListener> list = changeListeners.get(key);
            if (list != null) {
                if (list.remove(listener)) {
                    changeListeners.put(key, list);
                }
            }
        }
    }

    public interface OnPreferenceChangeListener<T> {
        void onPreferenceChanged(T changedValue);
    }

}
