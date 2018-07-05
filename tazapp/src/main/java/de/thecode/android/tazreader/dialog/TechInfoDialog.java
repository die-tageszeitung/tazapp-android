package de.thecode.android.tazreader.dialog;

import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.utils.UserDeviceInfo;

/**
 * Created by mate on 15.03.2017.
 */

public class TechInfoDialog extends DialogCustomView {



    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        UserDeviceInfo userDeviceInfo = UserDeviceInfo.getInstance(getContext());
        TazSettings settings = TazSettings.getInstance(getContext());
        View view = inflater.inflate(R.layout.dialog_techinfo, parent, false);
        ((TextView) view.findViewById(R.id.version)).setText(userDeviceInfo.getVersionName());
        ((TextView) view.findViewById(R.id.abis)).setText(TextUtils.join(", ", userDeviceInfo.getSupportedArchList()));
        ((TextView) view.findViewById(R.id.installationid)).setText(userDeviceInfo.getInstallationId());
        ((TextView) view.findViewById(R.id.androidVersion)).setText(Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")");
        return view;
    }

    public static class Builder extends DialogCustomView.AbstractBuilder<Builder,TechInfoDialog> {

        public Builder() {
            super(TechInfoDialog.class);
        }
    }
}
