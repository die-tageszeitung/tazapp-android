package de.thecode.android.tazreader.utils;

import android.app.Fragment;

import com.squareup.leakcanary.RefWatcher;

import de.thecode.android.tazreader.TazReaderApplication;

/**
 * Created by mate on 12.05.2015.
 */
public abstract class LeakCanaryFragment extends Fragment {

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = TazReaderApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
