package de.thecode.android.tazreader.dialog;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.thecode.android.tazreader.R;


/**
 * Created by mate on 21.10.2015.
 */
public class DialogIndeterminateProgress extends DialogCustomView {

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);
        int customDialogMargin = getResources().getDimensionPixelSize(R.dimen.custom_dialog_padding);
        layout.setPadding(customDialogMargin, customDialogMargin, customDialogMargin, customDialogMargin);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        ProgressBar progressCircle = new ProgressBar(getContext());
        progressCircle.setIndeterminate(true);
        LinearLayout.LayoutParams progressCircleLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(progressCircle, progressCircleLayoutParams);

        if (hasMessage()) {
            TextView messageView = new TextView(getContext());
            messageView.setText(getMessage());
            LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textLayoutParams.setMargins(customDialogMargin, 0, 0, 0);
            layout.addView(messageView, textLayoutParams);
        }
        return layout;
    }
}
