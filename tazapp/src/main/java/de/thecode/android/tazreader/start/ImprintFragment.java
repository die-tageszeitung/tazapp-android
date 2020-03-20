package de.thecode.android.tazreader.start;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.dialog.TechInfoDialog;

import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImprintFragment extends StartBaseFragment {


    public static final String DIALOG_TECHINFO  = "dialogTechInfo";
    public static final String DIALOG_ERRORMAIL = "dialogErrorMail";

    public ImprintFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getStartActivity().onUpdateDrawer(this);


        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.start_imprint, container, false);
        Button techInfoButton = view.findViewById(R.id.buttonTechInfo);
        techInfoButton.setOnClickListener(v -> {
                new TechInfoDialog.Builder().setPositiveButton()
                                            .setNeutralButton(R.string.imprint_licenses)
                                            .buildSupport()
                                            .show(getFragmentManager(), DIALOG_TECHINFO);
        });

        WebView webView = view.findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/imprint.html");


        return view;
    }
}
