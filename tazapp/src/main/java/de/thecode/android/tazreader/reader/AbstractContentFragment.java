package de.thecode.android.tazreader.reader;


import android.app.Activity;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.utils.BaseFragment;

/**
 * Created by mate on 18.12.2014.
 */
public abstract class AbstractContentFragment extends BaseFragment implements ReaderActivity.ConfigurationChangeListener {
    private static final Logger log = LoggerFactory.getLogger(AbstractContentFragment.class);
    public Context mContext;
    private WeakReference<IReaderCallback> mCallback;

    public AbstractContentFragment() {
        log.trace("");
    }

    public abstract void init(Paper paper, String key, String position);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mCallback = new WeakReference<>((IReaderCallback) activity);
        if (hasCallback()) getCallback().addConfigChangeListener(this);
    }

    public boolean hasCallback() {
        return mCallback != null && mCallback.get() != null;
    }

    public IReaderCallback getCallback() {
        return mCallback.get();
    }

    // Configuration Handling///////
    public boolean setConfig(String name, String value) {
        boolean isBoolean = false;
        try {
            TazSettings.getPrefString(mContext, name, null);
        } catch (ClassCastException e) {
            isBoolean = true;
        }

        if (isBoolean) {
            return TazSettings.setPref(mContext, name, value.equals("on"));
        } else {
            return TazSettings.setPref(mContext, name, value);
        }
    }

    public String getConfig(String name) {
        StringBuilder result = new StringBuilder();
        try {
            result.append(TazSettings.getPrefString(mContext, name, null));
        } catch (ClassCastException e) {
            result.append(TazSettings.getPrefBoolean(mContext, name, false) ? "on" : "off");
        }
        return result.toString();
    }

    public abstract void onTtsStateChanged(ReaderTtsFragment.TTS state);
}
