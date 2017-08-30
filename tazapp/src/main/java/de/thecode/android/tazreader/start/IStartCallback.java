package de.thecode.android.tazreader.start;


import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import de.thecode.android.tazreader.data.Paper;

/**
 * Created by mate on 05.02.2015.
 */
public interface IStartCallback {
    public StartActivity.RetainDataFragment getRetainData();

    public void loadFragment(NavigationDrawerFragment.NavigationItem item);

    public void onNavigationClick(NavigationDrawerFragment.ClickItem item);

    public void onUpdateDrawer(Fragment fragement);

    public Toolbar getToolbar();

    void onSuccessfulCredentialsCheck();

    public void enableDrawer(boolean bool);

    public void openReader(long paperId);

    public void startDownload(long paperId) throws Paper.PaperNotFoundException;

    public void callArchive();

    public void toggleWaitDialog(String tag);

}
