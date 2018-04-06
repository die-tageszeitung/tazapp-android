package de.thecode.android.tazreader.start;

import de.thecode.android.tazreader.utils.BaseFragment;

public abstract class StartBaseFragment extends BaseFragment {

    public StartActivity getStartActivity(){
        return (StartActivity) getActivity();
    }
}
