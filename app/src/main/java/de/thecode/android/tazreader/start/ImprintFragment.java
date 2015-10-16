package de.thecode.android.tazreader.start;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.utils.LeakCanaryFragment;
import de.thecode.android.tazreader.utils.Log;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImprintFragment extends LeakCanaryFragment {

    private static final String DIALOG_LICENCES = "dialogLicences";

    private IStartCallback callback;

    public ImprintFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        callback = (IStartCallback) getActivity();
        callback.onUpdateDrawer(this);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.start_imprint, container, false);
        ((TextView) view.findViewById(R.id.version)).setText(Log.getVersionName());
        Button licencesButton = (Button) view.findViewById(R.id.buttonLicences);
        licencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLicences();
            }
        });

        //TextView text = (TextView) view.findViewById(R.id.text);

        WebView webView = (WebView) view.findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/imprint.html");


        return view;
    }

    private void showLicences() {
        new LicencesDialog().withPositiveButton()
                            .show(getFragmentManager(), DIALOG_LICENCES);
    }


}
