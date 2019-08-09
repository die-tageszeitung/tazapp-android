package de.thecode.android.tazreader.reader;

import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.ReaderActivity.THEMES;

import timber.log.Timber;

public class SettingsDialog extends DialogCustomView {

    private SettingsDialogCallback mCallback;
    private SeekBar         seekBarColumns;
    private Button          btnNormal;
    private Button          btnSepia;
    private Button          btnNight;

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {

        boolean isScroll = TazSettings.getInstance(getContext())
                                      .getPrefBoolean(TazSettings.PREFKEY.ISSCROLL, false);
        mCallback = (SettingsDialogCallback) getContext();

        View view = inflater.inflate(R.layout.reader_settings, parent);


        btnNormal = view.findViewById(R.id.btnNormal);
        btnNormal.setOnClickListener(v -> {
                Timber.d("v: %s", v);
                if (mCallback != null) mCallback.onConfigurationChange(TazSettings.PREFKEY.THEME, THEMES.normal.name());
                colorThemeButtonText(THEMES.normal);
        });

        btnSepia = view.findViewById(R.id.btnSepia);
        btnSepia.setOnClickListener(v -> {
                Timber.d("v: %s", v);
                mCallback.onConfigurationChange(TazSettings.PREFKEY.THEME, THEMES.sepia.name());
                colorThemeButtonText(THEMES.sepia);
        });

        btnNight = view.findViewById(R.id.btnNight);
        btnNight.setOnClickListener(v -> {
                Timber.d("v: %s", v);
                mCallback.onConfigurationChange(TazSettings.PREFKEY.THEME, THEMES.night.name());
                colorThemeButtonText(THEMES.night);
        });
        colorThemeButtonText(THEMES.valueOf(TazSettings.getInstance(getContext())
                                                       .getPrefString(TazSettings.PREFKEY.THEME, null)));

        SwitchCompat switchIsScroll = view.findViewById(R.id.switchIsScroll);
        switchIsScroll.setChecked(!isScroll);
        switchIsScroll.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked)-> {
                Timber.d("buttonView: %s, isChecked: %s", buttonView, isChecked);
                if (mCallback != null)
                    mCallback.onConfigurationChange(TazSettings.PREFKEY.ISSCROLL, !isChecked);
                seekBarColumns.setEnabled(isChecked);
        });

        SwitchCompat switchIsPaging = view.findViewById(R.id.switchIsPaging);
        switchIsPaging.setChecked(TazSettings.getInstance(getContext())
                                             .getPrefBoolean(TazSettings.PREFKEY.ISPAGING, false));
        switchIsPaging.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                if (mCallback != null)
                    mCallback.onConfigurationChange(TazSettings.PREFKEY.ISPAGING, isChecked);
        });

        SwitchCompat switchIsScrollToNext = view.findViewById(R.id.switchIsScrollToNext);
        switchIsScrollToNext.setChecked(TazSettings.getInstance(getContext())
                                                   .getPrefBoolean(TazSettings.PREFKEY.ISSCROLLTONEXT, false));
        switchIsScrollToNext.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                    if (mCallback != null)
                        mCallback.onConfigurationChange(TazSettings.PREFKEY.ISSCROLLTONEXT, isChecked);
        });

        SwitchCompat switchIsJustify = view.findViewById(R.id.switchIsJustify);
        switchIsJustify.setChecked(TazSettings.getInstance(getContext())
                                              .getPrefBoolean(TazSettings.PREFKEY.ISJUSTIFY, false));
        switchIsJustify.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                if (mCallback != null)
                    mCallback.onConfigurationChange(TazSettings.PREFKEY.ISJUSTIFY, isChecked);
        });


        SwitchCompat switchFullscreen = view.findViewById(R.id.switchFullscreen);
        switchFullscreen.setVisibility(View.GONE);

        seekBarColumns = view.findViewById(R.id.seekBarColumn);

        seekBarColumns.setEnabled(!isScroll);
        seekBarColumns.setProgress((int) (Float.valueOf(TazSettings.getInstance(getContext())
                                                                   .getPrefString(TazSettings.PREFKEY.COLSIZE, "0")) * 10));
        seekBarColumns.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float colValue = ((float) progress) / 10;
                String colValueString = String.valueOf(colValue);
                Timber.d("color: %s", colValueString);
                if (fromUser) {
                    mCallback.onConfigurationChange(TazSettings.PREFKEY.COLSIZE, colValueString);
                }
            }
        });

        SeekBar seekBarFontSize = view.findViewById(R.id.seekBarFontSize);
        seekBarFontSize.setProgress(Integer.valueOf(TazSettings.getInstance(getContext())
                                                               .getPrefString(TazSettings.PREFKEY.FONTSIZE, "0")));
        seekBarFontSize.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Timber.d("seekBar: %s, progress: %s, fromUser: %s", seekBar, progress, fromUser);
                if (fromUser) {

                    mCallback.onConfigurationChange(TazSettings.PREFKEY.FONTSIZE, String.valueOf(progress));
                }
            }
        });

        return view;
    }

    private void colorThemeButtonText(THEMES theme) {
        if (theme == null) return;
        btnNight.setTextColor(ContextCompat.getColor(getContext(), R.color.reader_settings_button_text));
        btnNormal.setTextColor(ContextCompat.getColor(getContext(), R.color.reader_settings_button_text));
        btnSepia.setTextColor(ContextCompat.getColor(getContext(), R.color.reader_settings_button_text));
        switch (theme) {
            case night:
                btnNight.setTextColor(ContextCompat.getColor(getContext(), R.color.reader_settings_button_text_activated));
                break;
            case sepia:
                btnSepia.setTextColor(ContextCompat.getColor(getContext(), R.color.reader_settings_button_text_activated));
                break;
            case normal:
                btnNormal.setTextColor(ContextCompat.getColor(getContext(), R.color.reader_settings_button_text_activated));
                break;
        }
    }

    public static class Builder extends AbstractBuilder<Builder, SettingsDialog> {
        public Builder() {
            super(SettingsDialog.class);
        }
    }

    public interface SettingsDialogCallback {
        void onConfigurationChange(String name, String value);
        void onConfigurationChange(String name, boolean value);
    }
}
