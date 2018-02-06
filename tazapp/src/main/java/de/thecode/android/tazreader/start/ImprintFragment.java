package de.thecode.android.tazreader.start;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

import de.mateware.dialog.Dialog;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.dialog.TechInfoDialog;
import de.thecode.android.tazreader.utils.BaseFragment;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImprintFragment extends BaseFragment {


    public static final String DIALOG_TECHINFO  = "dialogTechInfo";
    public static final String DIALOG_ERRORMAIL = "dialogErrorMail";

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
        Button techInfoButton = (Button) view.findViewById(R.id.buttonTechInfo);
        techInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TechInfoDialog.Builder().setPositiveButton()
                                            .setNeutralButton(R.string.imprint_licenses)
                                            .buildSupport()
                                            .show(getFragmentManager(), DIALOG_TECHINFO);
            }
        });

        Button errorReportButton = view.findViewById(R.id.buttonErrorReport);
        errorReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Dialog.Builder().setMessage(R.string.imprint_error_report_dialog_message)
                                    .setNegativeButton()
                                    .setPositiveButton()
                                    .setNeutralButton(R.string.drawer_settings)
                                    .buildSupport()
                                    .show(getFragmentManager(),DIALOG_ERRORMAIL);
            }
        });

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
}
