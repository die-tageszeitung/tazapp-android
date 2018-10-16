package de.thecode.android.tazreader.start;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialognew.DataFolderDialog;
import de.thecode.android.tazreader.preferences.PreferenceFragmentCompat;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.ExtensionsKt;

import java.io.File;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

/**
 * Created by mate on 07.08.2017.
 */

public class PreferencesFragment extends PreferenceFragmentCompat {

    private static final String DIALOG_DATA_FOLDER = "DIALOG_DATA_FOLDER";

    private PreferenceCategory     pushPreferenceCat;

    private Preference             dataFolderPreference;

    private TazSettings.OnPreferenceChangeListener<String>  firebaseTokenPrefrenceListener = changedValue -> setPushPrefState(!TextUtils.isEmpty(
            changedValue));
//    private TazSettings.OnPreferenceChangeListener<Boolean> logWritingPrefrenceListener    = this::setSendLogPrefenceState;
    private TazSettings.OnPreferenceChangeListener<String>  dataFolderPrefrenceListener    = this::setDataFolderPrefenceState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((StartActivity) getActivity()).onUpdateDrawer(this);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null)
            view.setBackgroundColor(ContextCompat.getColor(inflater.getContext(), R.color.start_fragment_background));
        //setDividerHeight(0);
        return view;
    }

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_preferences);
        PreferenceScreen mainPreferenceScreen = (PreferenceScreen) findPreference("mainPreferenceScreen");
        pushPreferenceCat = (PreferenceCategory) findPreference(getString(R.string.category_notification_push_key));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Preference downloadNotificationCat = findPreference(getString(R.string.category_notification_download_key));
            mainPreferenceScreen.removePreference(downloadNotificationCat);
            if (pushPreferenceCat != null) {
                pushPreferenceCat.removePreference(pushPreferenceCat.findPreference(getString(R.string.pref_key_notification_push_ringtone)));
            }
        }
        PreferenceCategory notificationsCat = (PreferenceCategory) findPreference(getString(R.string.category_notifications_key));
        Preference notificationSettingsPreference = notificationsCat.findPreference(getString(R.string.pref_key_notification_settings));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationSettingsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent();
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                    if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        intent.putExtra("app_package",
                                        preference.getContext()
                                                  .getPackageName());
                        intent.putExtra("app_uid",
                                        preference.getContext()
                                                  .getApplicationInfo().uid);
                    } else {
                        intent.putExtra("android.provider.extra.APP_PACKAGE",
                                        preference.getContext()
                                                  .getPackageName());
                    }
                    startActivity(intent);
                    return true;
                }
            });
        } else {
            notificationsCat.removePreference(notificationSettingsPreference);
        }
        setPushPrefState(!TextUtils.isEmpty(TazSettings.getInstance(getContext())
                                                       .getFirebaseToken()));
        dataFolderPreference = findPreference(getString(R.string.pref_key_storage_folder));
        setDataFolderPrefenceState(TazSettings.getInstance(getContext()).getDataFolderPath());
        dataFolderPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DataFolderDialog.Companion.newInstance()
                                          .show(getFragmentManager(), DIALOG_DATA_FOLDER);
                return true;
            }
        });

    }


    public void setPushPrefState(boolean enabled) {
        pushPreferenceCat.setEnabled(enabled);
    }


    public void setDataFolderPrefenceState(String path) {
        new AsyncTaskListener<String,String>(new AsyncTaskListener.OnExecute<String, String>() {
            @Override
            public String execute(String... strings) throws Exception {
                File path = new File(strings[1]);
                return String.format(strings[0], path, ExtensionsKt.folderSizeReadable(path));
            }
        }, new AsyncTaskListener.OnSuccess<String>() {
            @Override
            public void onSuccess(String s) {
                dataFolderPreference.setSummary(s);
            }
        }).execute(getString(R.string.pref_summary_storage_folder), path);

    }


    @Override
    public void onStart() {
        super.onStart();
        TazSettings.getInstance(getContext())
                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.FIREBASETOKEN, firebaseTokenPrefrenceListener);
//        TazSettings.getInstance(getContext())
//                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.LOGFILE, logWritingPrefrenceListener);
        TazSettings.getInstance(getContext())
                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.DATA_FOLDER, dataFolderPrefrenceListener);

    }

    @Override
    public void onStop() {
        TazSettings.getInstance(getContext())
                   .removeOnPreferenceChangeListener(firebaseTokenPrefrenceListener);
//        TazSettings.getInstance(getContext())
//                   .removeOnPreferenceChangeListener(logWritingPrefrenceListener);
        TazSettings.getInstance(getContext())
                   .removeOnPreferenceChangeListener(dataFolderPrefrenceListener);


        super.onStop();
    }
}
