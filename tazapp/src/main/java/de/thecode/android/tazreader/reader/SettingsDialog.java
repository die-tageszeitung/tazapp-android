package de.thecode.android.tazreader.reader;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.ReaderActivity.THEMES;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsDialog extends DialogCustomView {

    private static final Logger log = LoggerFactory.getLogger(SettingsDialog.class);

    private IReaderCallback mCallback;
    private SeekBar seekBarColumns;
    private Button btnNormal;
    private Button btnSepia;
    private Button btnNight;

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {

        boolean isScroll = TazSettings.getInstance(getContext()).getPrefBoolean(TazSettings.PREFKEY.ISSCROLL, false);
        //boolean isFullscreen = TazSettings.getPrefBoolean(getContext(), TazSettings.PREFKEY.FULLSCREEN, false);
        mCallback = (IReaderCallback) getContext();

        View view = inflater.inflate(R.layout.reader_settings, parent);




        btnNormal = (Button) view.findViewById(R.id.btnNormal);
        btnNormal.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                log.debug("v: {}", v);
                if (mCallback != null) mCallback.onConfigurationChange(TazSettings.PREFKEY.THEME, THEMES.normal.name());
                colorThemeButtonText(THEMES.normal);
            }
        });

        btnSepia = (Button) view.findViewById(R.id.btnSepia);
        btnSepia.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                log.debug("v: {}",v);
                mCallback.onConfigurationChange(TazSettings.PREFKEY.THEME, THEMES.sepia.name());
                colorThemeButtonText(THEMES.sepia);
            }
        });

        btnNight = (Button) view.findViewById(R.id.btnNight);
        btnNight.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                log.debug("v: {}",v);
                mCallback.onConfigurationChange(TazSettings.PREFKEY.THEME, THEMES.night.name());
                colorThemeButtonText(THEMES.night);
            }
        });
        colorThemeButtonText(THEMES.valueOf(TazSettings.getInstance(getContext()).getPrefString(TazSettings.PREFKEY.THEME, null)));

        SwitchCompat switchIsScroll = (SwitchCompat) view.findViewById(R.id.switchIsScroll);
        switchIsScroll.setChecked(!isScroll);
        switchIsScroll.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                log.debug("buttonView: {}, isChecked: {}",buttonView, isChecked);
                if (mCallback != null) mCallback.onConfigurationChange(TazSettings.PREFKEY.ISSCROLL, !isChecked);
                seekBarColumns.setEnabled(isChecked);
            }
        });

        SwitchCompat switchIsPaging = (SwitchCompat) view.findViewById(R.id.switchIsPaging);
        switchIsPaging.setChecked(TazSettings.getInstance(getContext()).getPrefBoolean(TazSettings.PREFKEY.ISPAGING,false));
        switchIsPaging.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCallback != null) mCallback.onConfigurationChange(TazSettings.PREFKEY.ISPAGING, isChecked);
            }
        });

        SwitchCompat switchIsScrollToNext = (SwitchCompat) view.findViewById(R.id.switchIsScrollToNext);
        switchIsScrollToNext.setChecked(TazSettings.getInstance(getContext()).getPrefBoolean(TazSettings.PREFKEY.ISSCROLLTONEXT,false));
        switchIsScrollToNext.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mCallback != null) mCallback.onConfigurationChange(TazSettings.PREFKEY.ISSCROLLTONEXT, isChecked);
            }
        });

        SwitchCompat switchFullscreen = (SwitchCompat) view.findViewById(R.id.switchFullscreen);
        switchFullscreen.setVisibility(View.GONE);
//        switchFullscreen.setChecked(isFullscreen);
//        switchFullscreen.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                Log.v();
//                TazSettings.setPref(getActivity(), TazSettings.PREFKEY.FULLSCREEN, isChecked);
//                mCallback.setImmersiveMode();
//            }
//        });


        seekBarColumns = (SeekBar) view.findViewById(R.id.seekBarColumn);

        seekBarColumns.setEnabled(!isScroll);
        seekBarColumns.setProgress((int) (Float.valueOf(TazSettings.getInstance(getContext()).getPrefString(TazSettings.PREFKEY.COLSIZE, "0")) * 10));
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
                log.debug("color: {}",colValueString);
                if (fromUser) {
                    mCallback.onConfigurationChange(TazSettings.PREFKEY.COLSIZE, colValueString);
                }
            }
        });

        SeekBar seekBarFontSize = (SeekBar) view.findViewById(R.id.seekBarFontSize);
        seekBarFontSize.setProgress(Integer.valueOf(TazSettings.getInstance(getContext()).getPrefString(TazSettings.PREFKEY.FONTSIZE, "0")));
        seekBarFontSize.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                log.debug("seekBar: {}, progress: {}, fromUser: {}",seekBar, progress, fromUser);
                if (fromUser) {
                    mCallback.onConfigurationChange(TazSettings.PREFKEY.FONTSIZE, String.valueOf(progress));
                }
            }
        });

        return view;
    }
    
    private void colorThemeButtonText(THEMES theme) {
        if (theme == null) return;
        btnNight.setTextColor(ContextCompat.getColor(getContext(),R.color.reader_settings_button_text));
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

    public static class Builder extends AbstractBuilder<Builder,SettingsDialog> {
        public Builder() {
            super(SettingsDialog.class);
        }
    }


}
