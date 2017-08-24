package de.thecode.android.tazreader.start;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mateware.dialog.Dialog;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.preferences.PreferenceFragmentCompat;

import java.lang.ref.WeakReference;

/**
 * Created by mate on 07.08.2017.
 */

public class PreferencesFragment extends PreferenceFragmentCompat {

    WeakReference<IStartCallback> callback;
    Preference                    pushPreferenceCat;
    Preference                    dataLocationPreference;

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
        callback = new WeakReference<>((IStartCallback) getActivity());
        if (hasCallback()) getCallback().onUpdateDrawer(this);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(ContextCompat.getColor(inflater.getContext(), R.color.start_fragment_background));
        setDividerHeight(0);
        return view;
    }

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_preferences);
        pushPreferenceCat = findPreference("pushCat");
        setPushPrefState(!TextUtils.isEmpty(TazSettings.getInstance(getContext())
                                                       .getFirebaseToken()));
        dataLocationPreference = findPreference(TazSettings.PREFKEY.DATA_LOCATION);
        setDataLocationSummary();
    }


    public void setPushPrefState(boolean enabled) {
        pushPreferenceCat.setEnabled(enabled);
    }

    public void setDataLocationSummary() {
        dataLocationPreference.setSummary(TazSettings.getInstance(getContext())
                                                     .getPrefString(TazSettings.PREFKEY.DATA_LOCATION, null));
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

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private IStartCallback getCallback() {
        return callback.get();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (TazSettings.PREFKEY.DATA_LOCATION.equals(preference.getKey())) {
            new Dialog.Builder().setTitle("Test")
                                .setMessage("Bla Bla")
                                .setNegativeButton()
                                .setPositiveButton()
                                .buildSupport()
                                .show(getChildFragmentManager(), "Test");
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
