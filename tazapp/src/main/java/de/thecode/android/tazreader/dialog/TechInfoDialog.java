package de.thecode.android.tazreader.dialog;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;

import org.acra.util.Installation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by mate on 15.03.2017.
 */

public class TechInfoDialog extends DialogCustomView {

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {

        String versionName = BuildConfig.VERSION_NAME;
        try {
            PackageInfo packageInfo = getContext().getPackageManager()
                                                  .getPackageInfo(getContext().getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (Exception ignored) {
        }
        versionName += " ("+BuildConfig.VERSION_CODE+")";

        String[] supportedArch;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            supportedArch = Build.SUPPORTED_ABIS;
        } else {
            supportedArch = new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
        List<String> supportedArchList = new ArrayList<>(Arrays.asList(supportedArch));
        supportedArchList.removeAll(Arrays.asList("", null)); //remove empty
        supportedArchList = new ArrayList<>(new LinkedHashSet<>(supportedArchList)); //remove duplicate


        View view = inflater.inflate(R.layout.dialog_techinfo, parent, false);
        ((TextView) view.findViewById(R.id.version)).setText(versionName);
        ((TextView) view.findViewById(R.id.abis)).setText(TextUtils.join(", ", supportedArchList));
        ((TextView) view.findViewById(R.id.installationid)).setText(Installation.id(getContext()));

        return view;
    }

    public static class Builder extends DialogCustomView.AbstractBuilder<Builder,TechInfoDialog> {

        public Builder() {
            super(TechInfoDialog.class);
        }
    }
}
