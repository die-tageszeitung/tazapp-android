package de.thecode.android.tazreader.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.ListAdapter;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by Mate on 18.03.2015.
 */
public abstract class TcDialogAdapterList extends TcDialog {

    private static final Logger log = LoggerFactory.getLogger(TcDialogAdapterList.class);

    public static final String ARG_ARRAY_PARCABLE_ARCHIVEENTRIES = "archiveEntries";

    TcDialogAdapterListListener listListener;

    DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            TcDialogAdapterListEntry entry = getEntries().get(which);

            log.debug("Button {} {}", which,entry);
            if (listListener != null) listListener.onDialogAdapterListClick(getTag(), entry, TcDialogAdapterList.this.getArguments());
            else log.info(TcDialogAdapterListListener.class.getSimpleName() + " not set in Activity " + getActivity().getClass()
                                                                                                           .getSimpleName());
        }
    };

    @Override
    void setDialogContent() {
        builder.setAdapter(getAdapter(getActivity()), onClickListener);
    }

    public TcDialogAdapterList withEntries(ArrayList<? extends TcDialogAdapterListEntry> entries) {
        args.putParcelableArrayList(ARG_ARRAY_PARCABLE_ARCHIVEENTRIES, entries);
        return this;
    }

    public abstract ListAdapter getAdapter(Activity activity);

    public abstract ArrayList<? extends TcDialogAdapterListEntry> getEntries();

    public abstract static class TcDialogAdapterListEntry implements Parcelable{
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        log.debug(this.getTag());
        super.onAttach(activity);
        try {
            listListener = (TcDialogAdapterListListener) activity;
        } catch (ClassCastException e) {
            log.warn(e.getMessage());
        }
    }

    public interface TcDialogAdapterListListener {
        public void onDialogAdapterListClick(String tag,TcDialogAdapterListEntry entry, Bundle arguments);
    }

}
