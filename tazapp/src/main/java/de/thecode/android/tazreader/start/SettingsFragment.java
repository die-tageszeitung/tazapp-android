package de.thecode.android.tazreader.start;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.utils.BaseFragment;

import java.lang.ref.WeakReference;

public class SettingsFragment extends StartBaseFragment {

    public static final int REQUESTCODE_NOTIFICATION_SOUND = 8001;


    private CheckBox autoloadWifiCheckBox;
    private CheckBox fullscreenCheckBox;
    private EditText autoDeleteEditText;
    private TextView notificationSound;
    private TextView autodeleteUnitText;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getStartActivity().onUpdateDrawer(this);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.start_settings, container, false);
//
//        CheckBox autoloadCheckBox = (CheckBox) view.findViewById(R.id.autoLoadCheckBox);
//        autoloadWifiCheckBox = (CheckBox) view.findViewById(R.id.autoLoadWifiCheckBox);
//        CheckBox screenActiveCheckBox = (CheckBox) view.findViewById(R.id.screenActiveCheckBox);
//        //fullscreenCheckBox = (CheckBox) view.findViewById(R.id.fullscreenCheckBox);
//        RadioGroup orientationGroup = (RadioGroup) view.findViewById(R.id.orientationGroup);
//        autoDeleteEditText = (EditText) view.findViewById(R.id.autodeleteEditText);
//        CheckBox autodeleteCheckBox = (CheckBox) view.findViewById(R.id.autoDeleteCheckBox);
//        autodeleteUnitText = (TextView) view.findViewById(R.id.autodeleteUnitText);
//        RelativeLayout notificationSoundLayout = (RelativeLayout) view.findViewById(R.id.notificationSoundLayout);
//        notificationSound = (TextView) view.findViewById(R.id.notificationSound);
//        CheckBox notificationVibrateCheckBox = (CheckBox) view.findViewById(R.id.notificationVibrateCheckBox);
//        CheckBox pageIndexButtonCheckBox = (CheckBox) view.findViewById(R.id.showPageIndexButtonCheckBox);
//        CheckBox indexButtonCheckBox = (CheckBox) view.findViewById(R.id.showIndexButtonCheckBox);
//        CheckBox ttsCheckBox = (CheckBox) view.findViewById(R.id.ttsCheckBox);
//        CheckBox acraAlwaysAcceptCheckBox = (CheckBox) view.findViewById(R.id.acraAlwaysAcceptCheckBox);
//        CheckBox pageTapToArticleCheckBox = (CheckBox) view.findViewById(R.id.tapForArticleCheckBox);
//        CheckBox pageDoubleTapZoomCheckBox = (CheckBox) view.findViewById(R.id.doubleTapForZoomCheckBox);
//        CheckBox pageTapToTurnCheckBox = (CheckBox) view.findViewById(R.id.tapToTurnPageCheckbox);
//
//        autoloadCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false));
//        autoloadCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.AUTOLOAD, isChecked);
//                autoloadWifiCheckBox.setEnabled(isChecked);
//            }
//        });
//        autoloadWifiCheckBox.setEnabled(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false));
//
//        autoloadWifiCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD_WIFI, false));
//        autoloadWifiCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.AUTOLOAD_WIFI, isChecked);
//            }
//        });
//
//        autodeleteCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.AUTODELETE, false));
//        autodeleteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.AUTODELETE, isChecked);
//                autoDeleteEditText.setEnabled(isChecked);
//                autodeleteUnitText.setEnabled(isChecked);
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.FORCESYNC, true);
//            }
//        });
//        autoDeleteEditText.setEnabled(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.AUTODELETE, false));
//        autodeleteUnitText.setEnabled(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.AUTODELETE, false));
//
//        autoDeleteEditText.setText(String.valueOf(TazSettings.getInstance(getActivity()).getPrefInt(TazSettings.PREFKEY.AUTODELETE_VALUE, 0)));
//        autoDeleteEditText.addTextChangedListener(new TextWatcher() {
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//            }
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//                try {
//                    int days = Integer.valueOf(s.toString());
//                    TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.AUTODELETE_VALUE, days);
//                    TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.FORCESYNC, true);
//                } catch (NumberFormatException e) {
//
//                }
//            }
//        });
//
//        screenActiveCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.KEEPSCREEN, false));
//        screenActiveCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.KEEPSCREEN, isChecked);
//            }
//        });
//
//        final String orientationValues[] = getResources().getStringArray(R.array.orientationValue);
//        String currentOrientation = TazSettings.getInstance(getActivity()).getPrefString(TazSettings.PREFKEY.ORIENTATION, orientationValues[0]);
//        if (currentOrientation.equalsIgnoreCase(orientationValues[0])) orientationGroup.check(R.id.orientationButtonAuto);
//        else if (currentOrientation.equalsIgnoreCase(orientationValues[1])) orientationGroup.check(R.id.orientationButtonPort);
//        else if (currentOrientation.equalsIgnoreCase(orientationValues[2])) orientationGroup.check(R.id.orientationButtonLand);
//        orientationGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                String newOrientation;
//                switch (checkedId) {
//                    case R.id.orientationButtonAuto:
//                        newOrientation = orientationValues[0];
//                        break;
//                    case R.id.orientationButtonPort:
//                        newOrientation = orientationValues[1];
//                        break;
//                    case R.id.orientationButtonLand:
//                        newOrientation = orientationValues[2];
//                        break;
//                    default:
//                        newOrientation = orientationValues[0];
//                        break;
//                }
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.ORIENTATION, newOrientation);
//                Orientation.setActivityOrientation(getActivity(), TazPreferences.getInstance(getContext()).getOrientation());
//            }
//        });
//
//
//        notificationSoundLayout.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
//                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
//                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_sound_activity_title));
//                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
//                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
//                Uri ringtoneUri = TazSettings.getInstance(getActivity()).getRingtone();
//                if (ringtoneUri != null) intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);
//                startActivityForResult(intent, REQUESTCODE_NOTIFICATION_SOUND);
//            }
//        });
//        updateNotificationSoundLayout();
//        notificationVibrateCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.VIBRATE, false));
//        notificationVibrateCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.VIBRATE, isChecked);
//            }
//        });
//
//        indexButtonCheckBox.setChecked(TazSettings.getInstance(getActivity()).isIndexButton());
//        indexButtonCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setIndexButton(isChecked);
//            }
//        });
//
//        pageIndexButtonCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.PAGEINDEXBUTTON, false));
//        pageIndexButtonCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.PAGEINDEXBUTTON, isChecked);
//            }
//        });
//
//
//
//        ttsCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.TEXTTOSPEACH, false));
//        ttsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.TEXTTOSPEACH, isChecked);
//            }
//        });
//
//        acraAlwaysAcceptCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(ACRA.PREF_ALWAYS_ACCEPT, false));
//        acraAlwaysAcceptCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(ACRA.PREF_ALWAYS_ACCEPT, isChecked);
//            }
//        });
//
//        pageTapToArticleCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.PAGETAPTOARTICLE, false));
//        pageTapToArticleCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.PAGETAPTOARTICLE, isChecked);
//            }
//        });
//
//        pageDoubleTapZoomCheckBox.setChecked(TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.PAGEDOUBLETAPZOOM, false));
//        pageDoubleTapZoomCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.PAGEDOUBLETAPZOOM, isChecked);
//            }
//        });
//
//        pageTapToTurnCheckBox.setChecked(TazSettings.getInstance(getActivity()).isTapBorderToTurnPage());
//        pageTapToTurnCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TazSettings.getInstance(getActivity()).setTapBorderToTurnPage(isChecked);
//            }
//        });
//

        return view;
    }

//    private void updateNotificationSoundLayout() {
//        Uri ringtonUri = TazSettings.getInstance(getActivity()).getRingtone();
//        String ringtoneCaption = "unknown";
//        if (ringtonUri == null) {
//            ringtoneCaption = getString(R.string.notification_sound_silent);
//        } else {
//            try {
//                ringtoneCaption = RingtoneManager.getRingtone(getActivity(), ringtonUri)
//                                                 .getTitle(getActivity());
//            } catch (Exception e) {
//            }
//        }
//        notificationSound.setText(ringtoneCaption);
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUESTCODE_NOTIFICATION_SOUND) {
//            if (resultCode == Activity.RESULT_OK) {
//                if (data.hasExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)) {
//                    TazSettings.getInstance(getContext()).setRingtone((Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI));
//                    updateNotificationSoundLayout();
//                }
//            }
//        }
//
//    }
}
