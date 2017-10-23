package de.thecode.android.tazreader.reader;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.reader.ReaderActivity.ConfigurationChangeListener;
import de.thecode.android.tazreader.reader.ReaderActivity.DIRECTIONS;
import de.thecode.android.tazreader.reader.index.IIndexItem;

public interface IReaderCallback {

    public boolean onLoad(String key);

    public boolean onLoadNextArticle(DIRECTIONS fromDirection,String position);

    public boolean onLoadPrevArticle(DIRECTIONS fromDirection,String position);

    void onShowHelp();

    public int onGetBackgroundColor(String themeName);
    
    public void onBookmarkClick(IIndexItem item);

    public void closeDrawers();
    
    public void onConfigurationChange(String name, String value);
    
    public void onConfigurationChange(String name, boolean value);
    
    public void addConfigChangeListener(ConfigurationChangeListener listener);

    public void updateIndexes(String key);

    public Paper getPaper();

    public Resource getResource();

    public String getCurrentKey();

    public String getStoreValue(String path, String value);

    public void setFilterBookmarks(boolean bool);
    public boolean isFilterBookmarks();

    public void setImmersiveMode();

    public ReaderTtsFragment.TTS getTtsState();

    public void speak(String id, CharSequence text);
    
}
