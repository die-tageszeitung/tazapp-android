package de.thecode.android.tazreader.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;

/**
 * Created by mate on 09.08.2017.
 */

public class NotificationSoundPreference extends Preference {

    private final String mSummary;

    public NotificationSoundPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs,
                                                      android.support.v7.preference.R.styleable.Preference, defStyleAttr, defStyleRes);

        mSummary = TypedArrayUtils.getString(a, android.support.v7.preference.R.styleable.Preference_summary,
                                             android.support.v7.preference.R.styleable.Preference_android_summary);

        a.recycle();
    }

    public NotificationSoundPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationSoundPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, android.support.v7.preference.R.attr.preferenceStyle,
                                                     android.R.attr.preferenceStyle));
    }

    public NotificationSoundPreference(Context context) {
        this(context, null);
    }

    @Override
    public CharSequence getSummary() {
        Uri soundUri = TazSettings.getInstance(getContext()).getNotificationSoundUri(getKey());
        if (soundUri == null) return getContext().getString(R.string.notification_sound_silent);
        else {
            try {
                Ringtone ringtone = RingtoneManager.getRingtone(getContext(), soundUri);
                String title = ringtone.getTitle(getContext());
                ringtone.stop();
                return title;
            } catch (Exception e) {
                return e.getMessage();
            }
        }
    }

    @Override
    public void notifyChanged() {
        super.notifyChanged();
    }
}
