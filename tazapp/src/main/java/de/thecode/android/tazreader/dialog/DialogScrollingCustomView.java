package de.thecode.android.tazreader.dialog;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import de.thecode.android.tazreader.R;


/**
 * Created by Mate on 18.10.2015.
 */
public abstract class DialogScrollingCustomView extends DialogCustomView {

    @Override
    AppCompatDialog createDialogToReturn() {
        AlertDialog result = builder.create();
        ScrollView scroll = new ScrollView(getContext());
        scroll.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        int padding = getContext().getResources().getDimensionPixelSize(R.dimen.custom_dialog_padding);
        int topPadding = getContext().getResources().getDimensionPixelSize(R.dimen.custom_dialog_padding_top);
        scroll.setPadding(padding, topPadding, padding, 0);
        scroll.setClipToPadding(false);
        setCustomView(getView(LayoutInflater.from(getContext()), scroll));
        scroll.addView(getCustomView());
        result.setView(scroll);
        return result;
    }
}
