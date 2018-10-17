package de.thecode.android.tazreader.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Toast;

import com.takisoft.preferencex.EditTextPreference;

import androidx.core.content.res.TypedArrayUtils;
import timber.log.Timber;

/**
 * Created by mate on 08.08.2017.
 */

public class IntegerEditTextPreference extends EditTextPreference {

    String mSummary;

    public IntegerEditTextPreference(Context context) {
        this(context, null);
    }

    public IntegerEditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.takisoft.preferencex.R.attr.editTextPreferenceStyle);
    }

    public IntegerEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IntegerEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        /* Retrieve the Preference summary attribute since it's private
         * in the Preference class.
         */
        TypedArray a = context.obtainStyledAttributes(attrs,
                                                      androidx.preference.R.styleable.Preference, defStyleAttr, defStyleRes);

        mSummary = TypedArrayUtils.getString(a, androidx.preference.R.styleable.Preference_summary,
                                             androidx.preference.R.styleable.Preference_android_summary);

        a.recycle();
    }


    @Override
    protected String getPersistedString(String defaultReturnValue) {
        Timber.d("");
        return String.valueOf(getPersistedInt(defaultReturnValue == null ? 0 : Integer.parseInt(defaultReturnValue)));
    }

    @Override
    protected boolean persistString(String value) {
        try {
            return persistInt(Integer.valueOf(value));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setText(String text) {
        try {
            int value = Integer.valueOf(text);
            if (value < 1) value = 1;
            super.setText(String.valueOf(value));
        } catch (Exception e) {
            Toast.makeText(getContext(),e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            Timber.e(e);
        }
    }

    @Override
    public CharSequence getSummary() {


        final CharSequence entry = getText();
        if (mSummary == null) {
            return super.getSummary();
        } else {
            return String.format(mSummary, entry == null ? "" : entry);
        }
    }
}
