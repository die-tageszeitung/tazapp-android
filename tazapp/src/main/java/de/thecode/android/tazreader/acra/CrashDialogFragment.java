package de.thecode.android.tazreader.acra;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import de.thecode.android.tazreader.R;

import org.acra.ACRAConstants;
import org.acra.config.ACRAConfiguration;

/**
 * Created by Mate on 03.03.2017.
 */
public class CrashDialogFragment extends DialogFragment {

    private static final String ARG_CONFIG = "config";
    private EditText editCommentText;
    private EditText editEmailText;

    public static CrashDialogFragment newInstance(ACRAConfiguration config) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONFIG, config);
        CrashDialogFragment fragment = new CrashDialogFragment();
        fragment.setArguments(args);
        fragment.setCancelable(false);
        return fragment;
    }

    private CrashActivtyCallback callback;

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
        alertDialogBuilder.setView(buildCustomView(config));

        alertDialogBuilder.setPositiveButton(config.resDialogPositiveButtonText(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onSend(editCommentText.getText()
                                               .toString(), editEmailText.getText()
                                                                         .toString(), false);
            }
        });
        alertDialogBuilder.setNegativeButton(config.resDialogNegativeButtonText(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onCancel();
            }
        });
        alertDialogBuilder.setNeutralButton(R.string.crash_dialog_always_accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onSend(editCommentText.getText()
                                               .toString(), editEmailText.getText()
                                                                         .toString(), true);
            }
        });
        return alertDialogBuilder.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        callback = (CrashActivtyCallback) getActivity();
    }

    private View buildCustomView(ACRAConfiguration config) {
        View root = LayoutInflater.from(getContext())
                                  .inflate(R.layout.dialog_crash, null);

        TextView dialogText = (TextView) root.findViewById(R.id.dialogText);
        if (config.resDialogText() != ACRAConstants.DEFAULT_RES_VALUE) {
            dialogText.setText(config.resDialogText());
        } else {
            dialogText.setVisibility(View.GONE);
        }

        TextView commentText = (TextView) root.findViewById(R.id.commentText);
        editCommentText = (EditText) root.findViewById(R.id.commentEditText);
        if (config.resDialogCommentPrompt() != ACRAConstants.DEFAULT_RES_VALUE) {
            commentText.setText(config.resDialogCommentPrompt());
        } else {
            commentText.setVisibility(View.GONE);
            editCommentText.setVisibility(View.GONE);
        }

        TextView emailText = (TextView) root.findViewById(R.id.emailText);
        editEmailText = (EditText) root.findViewById(R.id.emailEditText);
        if (config.resDialogEmailPrompt() != ACRAConstants.DEFAULT_RES_VALUE) {
            commentText.setText(config.resDialogEmailPrompt());
        } else {
            emailText.setVisibility(View.GONE);
            editEmailText.setVisibility(View.GONE);
        }

        return root;
    }

    protected interface CrashActivtyCallback {
        void onSend(String comment, String userEmail, boolean always);

        void onCancel();
    }

}
