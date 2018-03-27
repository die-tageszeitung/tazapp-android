package de.thecode.android.tazreader.start;


import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import de.thecode.android.tazreader.data.Paper;

/**
 * Created by mate on 05.02.2015.
 */
public interface IStartCallback {
    StartActivity.RetainDataFragment getRetainData();

    void loadFragment(NavigationDrawerFragment.NavigationItem item);

    void onNavigationClick(NavigationDrawerFragment.ClickItem item);

    void onUpdateDrawer(Fragment fragement);

    Toolbar getToolbar();

    void onSuccessfulCredentialsCheck();

    void enableDrawer(boolean bool);

    void openReader(String bookId);

    void startDownload(String bookId) throws Paper.PaperNotFoundException;

    void callArchive();

    void showWaitDialog(String tag, String message);

    void hideWaitDialog(String tag);

}
