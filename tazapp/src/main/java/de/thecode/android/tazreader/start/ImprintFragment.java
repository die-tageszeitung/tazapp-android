package de.thecode.android.tazreader.start;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import de.mateware.dialog.Dialog;
import de.mateware.dialog.LicenceDialog;
import de.mateware.dialog.licences.Agpl30Licence;
import de.mateware.dialog.licences.Apache20Licence;
import de.mateware.dialog.licences.BsdLicence;
import de.mateware.dialog.licences.MitLicence;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.dialog.LogbackLicence;
import de.thecode.android.tazreader.utils.BaseFragment;

import org.acra.util.Installation;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImprintFragment extends BaseFragment {

    private static final String DIALOG_LICENCES        = "dialogLicences";
    private static final String DIALOG_INSTALLATION_ID = "dialogInstallationId";

    private WeakReference<IStartCallback> callback;

    public ImprintFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        callback = new WeakReference<>((IStartCallback) getActivity());
        if (hasCallback()) getCallback().onUpdateDrawer(this);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.start_imprint, container, false);
        ((TextView) view.findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME);
        Button licencesButton = (Button) view.findViewById(R.id.buttonLicences);
        licencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLicences();
            }
        });

        Button installationIdButton = (Button) view.findViewById(R.id.buttonInstallationId);
        installationIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Dialog.Builder().setMessage(Installation.id(getContext()))
                                    .setPositiveButton()
                                    .buildSupport()
                                    .show(getFragmentManager(), DIALOG_INSTALLATION_ID);
            }
        });


        //TextView text = (TextView) view.findViewById(R.id.text);


        WebView webView = (WebView) view.findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/imprint.html");


        return view;
    }

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private IStartCallback getCallback() {
        return callback.get();
    }


    private void showLicences() {
        new LicenceDialog.Builder().addEntry(
                new Apache20Licence(getContext(), "Android Support Library", "The Android Open Source Project", 2011))
                                   .addEntry(new Apache20Licence(getContext(), "OkHttp", "Square, Inc.", 2016))
                                   .addEntry(new Apache20Licence(getContext(), "Picasso", "Square, Inc.", 2013))
                                   .addEntry(new Apache20Licence(getContext(), "Picasso 2 OkHttp 3 Downloader", "Jake Wharton",
                                                                 2016))
                                   .addEntry(new Apache20Licence(getContext(), "AESCrypt-Android", "Scott Alexander-Bown", 2014))
                                   .addEntry(new Apache20Licence(getContext(), "Snacky", "Mate Siede", 2017))
                                   .addEntry(new MitLicence(getContext(), "dd-plist", "Daniel Dreibrodt", 2016))
                                   .addEntry(new Apache20Licence(getContext(), "Guava", "Google", 2016))
                                   .addEntry(new Apache20Licence(getContext(), "cwac-provider", "Mark Murphy", 2016))
                                   .addEntry(new Apache20Licence(getContext(), "EventBus", "Markus Junginger, greenrobot", 2014))
                                   .addEntry(new MitLicence(getContext(), "SLF4J API", "QOS.ch", 2013))
                                   .addEntry(new LogbackLicence())
                                   .addEntry(new Apache20Licence(getContext(), "Calligraphy", "Christopher Jenkins", 2013))
                                   .addEntry(new Apache20Licence(getContext(), "Commons Lang, Commons IO",
                                                                 "The Apache Software Foundation", 2016))
                                   .addEntry(new Apache20Licence(getContext(), "ViewpagerIndicator", "Jordan Réjaud", 2016))
                                   .addEntry(new Apache20Licence(getContext(), "RecyclerView-FlexibleDivider", "yqritc", 2016))
                                   .addEntry(new Agpl30Licence(getContext(), "mupdf", "Artifex Software, Inc.", 2015))
                                   .addEntry(new BsdLicence(getContext(), "Stetho", "Facebook, Inc.", 2015))
                                   .setPositiveButton()
                                   .buildSupport()
                                   .show(getFragmentManager(), DIALOG_LICENCES);
    }


}
