package de.thecode.android.tazreader.start;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.preferences.PreferenceFragmentCompat;

import java.lang.ref.WeakReference;

/**
 * Created by mate on 07.08.2017.
 */

public class PreferencesFragment extends PreferenceFragmentCompat {

    Preference                    pushPreferenceCat;
    SwitchPreferenceCompat        crashlyticsAlwaysSendPreference;

    TazSettings.OnPreferenceChangeListener<String> firebaseTokenPrefrenceListener = new TazSettings.OnPreferenceChangeListener<String>() {
        @Override
        public void onPreferenceChanged(String changedValue) {
            setPushPrefState(!TextUtils.isEmpty(changedValue));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
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
        pushPreferenceCat = findPreference("pushCat");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Preference downloadNotificationCat = findPreference("downloadNotificationCat");
            mainPreferenceScreen.removePreference(downloadNotificationCat);
            PreferenceCategory pushNotificationCat = (PreferenceCategory) findPreference("pushCat");
            if (pushNotificationCat != null) {
                pushNotificationCat.removePreference(pushNotificationCat.findPreference(getString(R.string.pref_key_notification_push_ringtone)));
            }

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
    }


    public void setPushPrefState(boolean enabled) {
        pushPreferenceCat.setEnabled(enabled);
    }


    @Override
    public void onStart() {
        super.onStart();
        TazSettings.getInstance(getContext())
                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.FIREBASETOKEN, firebaseTokenPrefrenceListener);
    }

    @Override
    public void onStop() {
        TazSettings.getInstance(getContext())
                   .removeOnPreferenceChangeListener(firebaseTokenPrefrenceListener);
        super.onStop();
    }
}
