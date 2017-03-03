package de.thecode.android.tazreader.acra;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import de.thecode.android.tazreader.R;

import org.acra.ACRAConstants;
import org.acra.config.ACRAConfiguration;
import org.acra.dialog.BaseCrashReportDialog;

/**
 * Created by mate on 03.03.2017.
 */

public class NewTazCrashDialog extends BaseCrashReportDialog {

    private static final String DIALOG_TAG = "CRASHDIALOG";

    @Override
    protected void init(@Nullable Bundle savedInstanceState) {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_TAG) == null) {
            CrashDialogFragment.newInstance(getConfig())
                               .show(getSupportFragmentManager(), DIALOG_TAG);
        }
    }

    public static class CrashDialogFragment extends DialogFragment {

        private static final int PADDING = 10;

        private static final String STATE_EMAIL   = "email";
        private static final String STATE_COMMENT = "comment";

        private static final String ARG_CONFIG = "config";

        public static CrashDialogFragment newInstance(ACRAConfiguration config) {
            Bundle args = new Bundle();
            args.putSerializable(ARG_CONFIG, config);
            CrashDialogFragment fragment = new CrashDialogFragment();
            fragment.setArguments(args);
            fragment.setCancelable(false);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ACRAConfiguration config = (ACRAConfiguration) getArguments().getSerializable(ARG_CONFIG);
            assert config != null;

            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());

            final int titleResourceId = config.resDialogTitle();
            if (titleResourceId != ACRAConstants.DEFAULT_RES_VALUE) {
                alertDialogBuilder.setTitle(titleResourceId);
            }
            final int iconResourceId = config.resDialogIcon();
            if (iconResourceId != ACRAConstants.DEFAULT_RES_VALUE) {
                alertDialogBuilder.setIcon(iconResourceId);
            }
            alertDialogBuilder.setView(buildCustomView(savedInstanceState,config));

            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            alertDialogBuilder.setNegativeButton("ABBRECHEN", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            alertDialogBuilder.setNeutralButton("IMMER SENDEN", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            return alertDialogBuilder.create();
        }

        private View buildCustomView(Bundle savedInstanceState, ACRAConfiguration config) {

            View root = LayoutInflater.from(getContext()).inflate(R.layout.crash_dialog, null);

            return root;
        }

        private void addView(LinearLayout layout, View viewToAdd, boolean topPadding) {
            viewToAdd.setPadding(viewToAdd.getPaddingLeft(), topPadding ? dpToPx(PADDING) : viewToAdd.getPaddingTop(),
                                 viewToAdd.getPaddingRight(), viewToAdd.getPaddingBottom());
            layout.addView(viewToAdd);
        }

        private int dpToPx(int dp) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources()
                                                                                                .getDisplayMetrics());
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }
    }

}
