package de.thecode.android.tazreader.start;

import androidx.fragment.app.Fragment;

public abstract class StartBaseFragment extends Fragment {
    public StartActivity getStartActivity(){
        return (StartActivity) getActivity();
    }
}
