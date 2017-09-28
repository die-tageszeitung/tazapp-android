package de.thecode.android.tazreader.reader;


import android.content.Context;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.utils.BaseFragment;

/**
 * Created by mate on 18.12.2014.
 */
public abstract class AbstractContentFragment extends BaseFragment implements ReaderActivity.ConfigurationChangeListener {

    public IReaderCallback callback;


    public AbstractContentFragment() {

    }

    //public abstract void init(Paper paper, String key, String position);

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IReaderCallback) callback = (IReaderCallback)context;
        else throw new RuntimeException(context.toString() + " must implement " + IReaderCallback.class.getSimpleName());
        callback.addConfigChangeListener(this);

//        mCallback = new WeakReference<>((IReaderCallback) context);
//        if (hasCallback()) getCallback().addConfigChangeListener(this);

    }

//    public boolean hasCallback() {
//        return mCallback != null && mCallback.get() != null;
//    }

//    public IReaderCallback getCallback() {
//        return mCallback.get();
//    }

    // Configuration Handling///////
    public boolean setConfig(String name, String value) {
        boolean isBoolean = false;
        try {
            TazSettings.getInstance(getContext()).getPrefString(name, null);
        } catch (ClassCastException e) {
            isBoolean = true;
        }

        if (isBoolean) {
            return TazSettings.getInstance(getContext()).setPref(name, value.equals("on"));
        } else {
            return TazSettings.getInstance(getContext()).setPref(name, value);
        }
    }

    public String getConfig(String name) {
        StringBuilder result = new StringBuilder();
        try {
            result.append(TazSettings.getInstance(getContext()).getPrefString(name, null));
        } catch (ClassCastException e) {
            result.append(TazSettings.getInstance(getContext()).getPrefBoolean(name, false) ? "on" : "off");
        }
        return result.toString();
    }

    public abstract void onTtsStateChanged(ReaderTtsFragment.TTS state);
}
