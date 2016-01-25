package de.thecode.android.tazreader.start;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.Log;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImprintFragment extends BaseFragment {

    private static final String DIALOG_LICENCES = "dialogLicences";

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

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private IStartCallback getCallback() {
        return callback.get();
    }


    private void showLicences() {
        new LicencesDialog().withPositiveButton()
                            .show(getFragmentManager(), DIALOG_LICENCES);
    }


}
