package de.thecode.android.tazreader.utils;


import android.support.v4.app.Fragment;

/**
 * Created by mate on 12.05.2015.
 */
public abstract class BaseFragment<T extends BaseActivity> extends Fragment {

    @SuppressWarnings("unchecked")
    public T getMyActivity() {
        return (T) getActivity();
    }

    public boolean checkActivity(){
        return getActivity() != null;
    }

}
