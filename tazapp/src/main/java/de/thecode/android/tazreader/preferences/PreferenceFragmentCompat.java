package de.thecode.android.tazreader.preferences;


import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;

import androidx.preference.Preference;
import timber.log.Timber;

/**
 * Created by mate on 08.08.2017.
 */

public abstract class PreferenceFragmentCompat extends com.takisoft.preferencex.PreferenceFragmentCompat {

    private static final int REQUESTCODE_RINGTONE = 7491;
    private static String lastRingtoneRequestForKey;

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof NotificationSoundPreference) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_sound_activity_title));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            Uri ringtoneUri = TazSettings.getInstance(getContext()).getNotificationSoundUri(preference.getKey());
            if (ringtoneUri != null) intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);
            lastRingtoneRequestForKey = preference.getKey();
            startActivityForResult(intent, REQUESTCODE_RINGTONE);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUESTCODE_RINGTONE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data.hasExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)) {
                    Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (!TextUtils.isEmpty(lastRingtoneRequestForKey)) {
                        TazSettings.getInstance(getContext()).setNotificationSoundUri(lastRingtoneRequestForKey,ringtoneUri);
                        Preference preference = findPreference(lastRingtoneRequestForKey);
                        if (preference instanceof NotificationSoundPreference) {
                            ((NotificationSoundPreference) preference).notifyChanged();
                        }
                    }
                    Timber.i("");
                }
            }
        }

    }
}
