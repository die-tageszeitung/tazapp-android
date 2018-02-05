package de.thecode.android.tazreader.reader.index;

import de.thecode.android.tazreader.widget.ShareButton;

import java.util.List;


public interface IIndexItem extends ShareButton.ShareButtonCallback{
    
    enum Type {
        SOURCE, CATEGORY, PAGE, ARTICLE, TOPLINK, UNKNOWN
    }
    
    String getTitle();
    void setIndexChildsVisible(boolean childsVisible);
    boolean areIndexChildsVisible() ;
    IIndexItem getIndexParent();
    boolean hasIndexParent();
    boolean hasIndexChilds();
    List<IIndexItem> getIndexChilds();
    int getIndexChildCount();
    String getKey();
    boolean hasBookmarkedChilds();
    Type getType();
    IIndexItem getIndexAncestorWithKey(String key);
    boolean isBookmarkable();
    void setBookmark(boolean bool);
    boolean isBookmarked();
    boolean isLink();
    void setLink(IIndexItem item);
    IIndexItem getLink();
}
