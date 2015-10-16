package de.thecode.android.tazreader.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;

import de.thecode.android.tazreader.utils.Log;


public class TcDialog extends DialogFragment {

    static final String ARG_INT_TITLE = "title_resid";
    static final String ARG_STRING_TITLE = "title_text";

    static final String ARG_INT_MESSAGE = "message_resid";
    static final String ARG_STRING_MESSAGE = "message";

    static final String ARG_INT_ICONID = "icon_id";
    static final String ARG_INT_STYLEID = "style";

    static final String ARG_INT_BUTTONTEXTPOSITIVE = "positive_button_resid";
    static final String ARG_INT_BUTTONTEXTNEGATIVE = "negative_button_resid";
    static final String ARG_INT_BUTTONTEXTNEUTRAL = "neutral_button_resid";
    static final String ARG_STRING_BUTTONTEXTPOSITIVE = "positive_button_text";
    static final String ARG_STRING_BUTTONTEXTNEGATIVE = "negative_button_text";
    static final String ARG_STRING_BUTTONTEXTNEUTRAL = "neutral_button_text";

    public final static int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
    public final static int BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL;
    public final static int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;

    public Bundle args = new Bundle();
    TcDialogButtonListener buttonListener;
    TcDialogDismissListener dismissListener;
    TcDialogCancelListener cancelListener;


    @Override
    public void show(@NonNull FragmentManager manager, String tag) {
        this.setArguments(args);
        super.show(manager, tag);
    }

    @Override
    public int show(@NonNull FragmentTransaction transaction, String tag) {
        this.setArguments(args);
        return super.show(transaction, tag);
    }

    public TcDialog withTitle(String title) {
        args.putString(ARG_STRING_TITLE, title);
        return this;
    }

    public TcDialog withTitle(int resId) {
        args.putInt(ARG_INT_TITLE, resId);
        return this;
    }

    public TcDialog withMessage(String message) {
        args.putString(ARG_STRING_MESSAGE, message);
        return this;
    }

    public TcDialog withMessage(int resId) {
        args.putInt(ARG_INT_MESSAGE, resId);
        return this;
    }

    public TcDialog withPositiveButton(String text) {
        args.putString(ARG_STRING_BUTTONTEXTPOSITIVE, text);
        return this;
    }

    public TcDialog withPositiveButton(int resId) {
        args.putInt(ARG_INT_BUTTONTEXTPOSITIVE, resId);
        return this;
    }

    public TcDialog withPositiveButton() {
        return withPositiveButton(android.R.string.ok);
    }

    public TcDialog withNeutralButton(String text) {
        args.putString(ARG_STRING_BUTTONTEXTNEUTRAL, text);
        return this;
    }

    public TcDialog withNeutralButton(int resId) {
        args.putInt(ARG_INT_BUTTONTEXTNEUTRAL, resId);
        return this;
    }

    public TcDialog withNeutralButton() {
        return withNeutralButton(android.R.string.untitled);
    }

    public TcDialog withNegativeButton(String text) {
        args.putString(ARG_STRING_BUTTONTEXTNEGATIVE, text);
        return this;
    }

    public TcDialog withNegativeButton(int resId) {
        args.putInt(ARG_INT_BUTTONTEXTNEGATIVE, resId);
        return this;
    }

    public TcDialog withNegativeButton() {
        return withNegativeButton(android.R.string.cancel);
    }

    public TcDialog withIcon(int resId) {
        args.putInt(ARG_INT_ICONID, resId);
        return this;
    }

    public TcDialog withStyle(int resId) {
        args.putInt(ARG_INT_STYLEID, resId);
        return this;
    }

    public TcDialog withCancelable(boolean cancelable) {
        this.setCancelable(cancelable);
        return this;
    }

    public TcDialog withBundle(Bundle bundle) {
        args.putAll(bundle);
        return this;
    }

    OnClickListener onClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Log.d("Button", which);
            if (buttonListener != null) buttonListener.onDialogClick(getTag(), TcDialog.this.getArguments(), which);
            else Log.i(TcDialogButtonListener.class.getSimpleName() + " not set in Activity " + getActivity().getClass()
                                                                                                             .getSimpleName());
        }
    };

    AlertDialog.Builder builder;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(this.getTag());
        builder = new AlertDialog.Builder(hasStyle() ? new ContextThemeWrapper(getActivity(), getStyle()) : getActivity());

        if (hasIcon()) builder.setIcon(getIcon());

        if (hasTitle()) builder.setTitle(getTitle());

        setDialogContent();

        if (hasPositiveButton()) builder.setPositiveButton(getPositiveButton(), onClickListener);

        if (hasNeutralButton()) builder.setNeutralButton(getNeutralButton(), onClickListener);

        if (hasNegativeButton()) builder.setNegativeButton(getNegativeButton(), onClickListener);

        return createDialogToReturn();
    }

    void setDialogContent() {
        if (hasMessage()) builder.setMessage(getMessage());
    }

    Dialog createDialogToReturn() {
        return builder.create();
    }


    protected boolean hasTitle() {
        return (getArguments().containsKey(ARG_STRING_TITLE) || getArguments().containsKey(ARG_INT_TITLE));
    }

    protected String getTitle() {
        return getText(ARG_STRING_TITLE, ARG_INT_TITLE);
    }

    protected boolean hasMessage() {
        return (getArguments().containsKey(ARG_STRING_MESSAGE) || getArguments().containsKey(ARG_INT_MESSAGE));
    }

    protected String getMessage() {
        return getText(ARG_STRING_MESSAGE, ARG_INT_MESSAGE);
    }

    protected boolean hasIcon() {
        return (getArguments().containsKey(ARG_INT_ICONID));
    }

    protected int getIcon() {
        return getArguments().getInt(ARG_INT_ICONID);
    }

    protected boolean hasStyle() {
        return (getArguments().containsKey(ARG_INT_STYLEID));
    }

    protected int getStyle() {
        return getArguments().getInt(ARG_INT_STYLEID);
    }

    protected boolean hasPositiveButton() {
        return (getArguments().containsKey(ARG_STRING_BUTTONTEXTPOSITIVE) || getArguments().containsKey(ARG_INT_BUTTONTEXTPOSITIVE));
    }

    protected String getPositiveButton() {
        return getText(ARG_STRING_BUTTONTEXTPOSITIVE, ARG_INT_BUTTONTEXTPOSITIVE);
    }

    protected boolean hasNegativeButton() {
        return (getArguments().containsKey(ARG_STRING_BUTTONTEXTNEGATIVE) || getArguments().containsKey(ARG_INT_BUTTONTEXTNEGATIVE));
    }

    protected String getNegativeButton() {
        return getText(ARG_STRING_BUTTONTEXTNEGATIVE, ARG_INT_BUTTONTEXTNEGATIVE);
    }

    protected boolean hasNeutralButton() {
        return (getArguments().containsKey(ARG_STRING_BUTTONTEXTNEUTRAL) || getArguments().containsKey(ARG_INT_BUTTONTEXTNEUTRAL));
    }

    protected String getNeutralButton() {
        return getText(ARG_STRING_BUTTONTEXTNEUTRAL, ARG_INT_BUTTONTEXTNEUTRAL);
    }

    protected String getText(String arg_string, String arg_int) {
        String result = null;
        if (getArguments().containsKey(arg_string)) result = getArguments().getString(arg_string);
        else if (getArguments().containsKey(arg_int)) result = getString(getArguments().getInt(arg_int));
        return result;
    }


    @Override
    public void onAttach(Activity activity) {
        Log.d(this.getTag());
        super.onAttach(activity);
        try {
            buttonListener = (TcDialogButtonListener) activity;
        } catch (ClassCastException e) {
            Log.w(e.getMessage());
        }
        try {
            dismissListener = (TcDialogDismissListener) activity;
        } catch (ClassCastException e) {
            Log.w(e.getMessage());
        }

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (getTag() != null) {
            Log.v(getTag());
            if (dismissListener != null) dismissListener.onDialogDismiss(getTag(), TcDialog.this.getArguments());
            else Log.i(TcDialogDismissListener.class.getSimpleName() + " not set in Activity " + getActivity().getClass()
                                                                                                              .getSimpleName());
        }
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (getTag() != null) {
            Log.v(getTag());
            if (cancelListener != null) cancelListener.onDialogCancel(getTag(), TcDialog.this.getArguments());
            else Log.i(TcDialogCancelListener.class.getSimpleName() + " not set in Activity " + getActivity().getClass()
                                                                                                             .getSimpleName());
        }
        super.onCancel(dialog);
    }

    public interface TcDialogButtonListener {

        public void onDialogClick(String tag, Bundle arguments, int which);
    }

    public interface TcDialogDismissListener {
        public void onDialogDismiss(String tag, Bundle arguments);
    }

    public interface TcDialogCancelListener {
        public void onDialogCancel(String tag, Bundle arguments);
    }

    public static void dismissDialog(FragmentManager fm, String dialogTag) {
        Log.d(dialogTag);
        DialogFragment dialog = (DialogFragment) fm.findFragmentByTag(dialogTag);
        if (dialog != null) dialog.dismiss();
    }
}
