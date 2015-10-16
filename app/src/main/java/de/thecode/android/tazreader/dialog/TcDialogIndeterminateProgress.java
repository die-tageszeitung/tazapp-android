package de.thecode.android.tazreader.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;


public class TcDialogIndeterminateProgress extends TcDialog {
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(hasStyle() ? new ContextThemeWrapper(getActivity(), getStyle()) : getActivity());
        dialog.setIndeterminate(true);
        
        if (hasTitle())
            dialog.setTitle(getTitle());
        
        if (hasIcon())
            dialog.setIcon(getIcon());
        
        if (hasMessage())
            dialog.setMessage(getMessage());
        
        
        OnKeyListener keyListener = new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                    KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }

        };
        dialog.setOnKeyListener(keyListener);
        
        if (hasPositiveButton())
            dialog.setButton(BUTTON_POSITIVE, getPositiveButton(), onClickListener);

        if (hasNeutralButton())
            dialog.setButton(BUTTON_NEUTRAL, getNeutralButton(), onClickListener);
        
        if (hasNegativeButton())
            dialog.setButton(BUTTON_NEGATIVE, getNegativeButton(), onClickListener);
        
        return dialog;
    }
}
