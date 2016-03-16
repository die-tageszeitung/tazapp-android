package de.thecode.android.tazreader.dialog;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Created by Mate on 18.10.2015.
 */
public abstract class DialogCustomView extends Dialog {
    @Override
    void setDialogContent() {
        //Override to do nothing
    }

    private View customView;

    @Override
    AppCompatDialog createDialogToReturn() {
        AlertDialog result = builder.create();
        setCustomView(getView(LayoutInflater.from(getContext()), null));
        result.setView(getCustomView());
        return result;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void setCustomView(View customView) {
        this.customView = customView;
    }

    public View getCustomView() {
        return customView;
    }

    public abstract View getView(LayoutInflater inflater, ViewGroup parent);

}
