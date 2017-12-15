package de.thecode.android.tazreader.start;


import de.thecode.android.tazreader.data.Paper;

/**
 * Created by mate on 05.02.2015.
 */
public interface IStartCallback {
    StartActivity.RetainDataFragment getRetainData();

    void loadFragment(NavigationDrawerFragment.NavigationItem item);

    void onNavigationClick(NavigationDrawerFragment.ClickItem item);

    //void openReader(long paperId);

    void startDownload(long paperId) throws Paper.PaperNotFoundException;


    void showWaitDialog(String tag, String message);

    void hideWaitDialog(String tag);

}
