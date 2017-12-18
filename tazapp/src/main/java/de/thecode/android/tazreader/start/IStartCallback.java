package de.thecode.android.tazreader.start;


/**
 * Created by mate on 05.02.2015.
 */
public interface IStartCallback {
    StartActivity.RetainDataFragment getRetainData();

    void loadFragment(NavigationDrawerFragment.NavigationItem item);

    void onNavigationClick(NavigationDrawerFragment.ClickItem item);

    void showWaitDialog(String tag, String message);

    void hideWaitDialog(String tag);

}
