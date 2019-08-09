package de.thecode.android.tazreader.reader;


import android.os.Bundle;
import androidx.annotation.Nullable;

import de.thecode.android.tazreader.data.TazSettings;

/**
 * Created by mate on 18.12.2014.
 */
public abstract class AbstractContentFragment extends ReaderBaseFragment implements ReaderActivity.ConfigurationChangeListener {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getReaderActivity().addConfigChangeListener(this);
    }

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
}
