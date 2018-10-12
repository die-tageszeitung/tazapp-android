package de.thecode.android.tazreader.start;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.commonsware.cwac.provider.StreamProvider;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialognew.DataFolderDialog;
import de.thecode.android.tazreader.preferences.PreferenceFragmentCompat;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.Charsets;
import de.thecode.android.tazreader.utils.ExtensionsKt;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.StreamUtils;
import de.thecode.android.tazreader.utils.UserDeviceInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import timber.log.Timber;

/**
 * Created by mate on 07.08.2017.
 */

public class PreferencesFragment extends PreferenceFragmentCompat {

    private static final String DIALOG_DATA_FOLDER = "DIALOG_DATA_FOLDER";

    private PreferenceCategory     pushPreferenceCat;
    private SwitchPreferenceCompat crashlyticsAlwaysSendPreference;
    private Preference             sendLogPreference;
    private Preference             dataFolderPreference;

    private TazSettings.OnPreferenceChangeListener<String>  firebaseTokenPrefrenceListener = changedValue -> setPushPrefState(!TextUtils.isEmpty(
            changedValue));
    private TazSettings.OnPreferenceChangeListener<Boolean> logWritingPrefrenceListener    = this::setSendLogPrefenceState;
    private TazSettings.OnPreferenceChangeListener<String>  dataFolderPrefrenceListener    = this::setDataFolderPrefenceState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
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
        crashlyticsAlwaysSendPreference = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_key_crashlytics_always_send));
        crashlyticsAlwaysSendPreference.setChecked(TazSettings.getInstance(getContext())
                                                              .getCrashlyticsAlwaysSend());
        crashlyticsAlwaysSendPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                TazSettings.getInstance(preference.getContext())
                           .setCrashlyticsAlwaysSend((Boolean) newValue);
                return true;
            }
        });
        setPushPrefState(!TextUtils.isEmpty(TazSettings.getInstance(getContext())
                                                       .getFirebaseToken()));

        sendLogPreference = findPreference(getString(R.string.pref_key_send_log_file));
        setSendLogPrefenceState(TazSettings.getInstance(getContext())
                                           .isWriteLogfile());
        sendLogPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                try {
                    try (InputStream bodyInputStream = getContext().getAssets()
                                                                   .open("errorReportMail/logfiles.txt")) {

                        String aboId = AccountHelper.getInstance(getContext())
                                                    .getUser("");
                        UserDeviceInfo userDeviceInfo = UserDeviceInfo.getInstance(getContext());

                        String body = StreamUtils.toString(bodyInputStream, Charsets.UTF_8);
                        body = body.replaceFirst("\\{appversion\\}", userDeviceInfo.getVersionName());
                        body = body.replaceFirst("\\{installid\\}", userDeviceInfo.getInstallationId());
                        body = body.replaceFirst("\\{aboid\\}", aboId);
                        body = body.replaceFirst("\\{androidVersion\\}",
                                                 Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")");
                        body = body.replaceFirst("\\{pushToken\\}",
                                                 TazSettings.getInstance(getContext())
                                                            .getFirebaseToken());

                        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        emailIntent.setType("message/rfc822");
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{BuildConfig.ERRORMAIL});
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                                             getString(R.string.logsmail_subject, getString(R.string.app_name), aboId));
                        emailIntent.putExtra(Intent.EXTRA_TEXT, body);


                        if (TazSettings.getInstance(getContext())
                                       .isWriteLogfile()) {
                            File logDir = StorageManager.getInstance(getContext())
                                                        .getLogCache();
                            if (logDir != null) {
                                File[] logfiles = logDir.listFiles();
                                ArrayList<Uri> uris = new ArrayList<>();
                                for (File logfile : logfiles) {
                                    if (!logfile.isDirectory()) {
                                        Uri contentUri = StreamProvider.getUriForFile(BuildConfig.APPLICATION_ID + ".streamprovider",
                                                                                      logfile);
                                        uris.add(contentUri);
                                    }
                                }
                                if (!uris.isEmpty()) {
                                    emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                                }
                            }
                        }

                        startActivity(emailIntent);
                    }
                } catch (NullPointerException | IOException | ActivityNotFoundException e) {
                    Timber.e(e);
                }


                return true;
            }
        });
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

    public void setSendLogPrefenceState(boolean enabled) {
        sendLogPreference.setEnabled(enabled);
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
        TazSettings.getInstance(getContext())
                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.LOGFILE, logWritingPrefrenceListener);
        TazSettings.getInstance(getContext())
                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.DATA_FOLDER, dataFolderPrefrenceListener);

    }

    @Override
    public void onStop() {
        TazSettings.getInstance(getContext())
                   .removeOnPreferenceChangeListener(firebaseTokenPrefrenceListener);
        TazSettings.getInstance(getContext())
                   .removeOnPreferenceChangeListener(logWritingPrefrenceListener);
        TazSettings.getInstance(getContext())
                   .removeOnPreferenceChangeListener(dataFolderPrefrenceListener);


        super.onStop();
    }
}
