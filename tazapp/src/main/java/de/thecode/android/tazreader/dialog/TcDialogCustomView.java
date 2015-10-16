package de.thecode.android.tazreader.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;

public abstract class TcDialogCustomView extends TcDialog {

    @Override
    void setDialogContent() {
        //Override to do nothing
    }
    
    @Override
    Dialog createDialogToReturn() {
        AlertDialog result = builder.create();
        result.setView(getView(getActivity().getLayoutInflater()),0,0,0,0);
        return result;
    }

    public abstract View getView(LayoutInflater inflater);
}
