package de.thecode.android.tazreader.reader;


import android.app.Activity;
import android.content.Context;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by mate on 18.12.2014.
 */
public abstract class AbstractContentFragment extends BaseFragment implements ReaderActivity.ConfigurationChangeListener {

    public Context mContext;
    public IReaderCallback mCallback;

    public AbstractContentFragment() {
        Log.v();
    }

    public abstract void init(Paper paper, String key, String position);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mCallback = (IReaderCallback) activity;
        mCallback.addConfigChangeListener(this);
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
}
