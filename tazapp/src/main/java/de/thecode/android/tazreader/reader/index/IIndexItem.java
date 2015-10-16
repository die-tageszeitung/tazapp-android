package de.thecode.android.tazreader.reader.index;

import java.util.List;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.widget.ShareButton;


public interface IIndexItem extends ShareButton.ShareButtonCallback{
    
    public static enum Type {
        SOURCE, CATEGORY, PAGE, ARTICLE, TOPLINK, UNKNOWN
    };
    
    public String getTitle();
    public void setIndexChildsVisible(boolean childsVisible);
    public boolean areIndexChildsVisible() ;
    public IIndexItem getIndexParent();
    public boolean isVisible();
    public boolean hasIndexParent();
    public boolean hasIndexChilds();
    public List<IIndexItem> getIndexChilds();
    public String getKey();
    public boolean hasBookmarkedChilds();
    public Type getType();
    public Paper getPaper();
    public IIndexItem getIndexAncestorWithKey(String key);
    public boolean isBookmarkable();
    public void setBookmark(boolean bool);
    public boolean isBookmarked();
    boolean isLink();
    void setLink(IIndexItem item);
    IIndexItem getLink();
//    public boolean hasIndexChildWithKey(String key);
}
