package de.thecode.android.tazreader.data;

import de.thecode.android.tazreader.widget.ShareButton;

import java.util.List;


public interface ITocItem extends ShareButton.ShareButtonCallback, Cloneable{
    
    enum Type {
        SOURCE, CATEGORY, PAGE, ARTICLE, TOPLINK, UNKNOWN
    }
    
    String getTitle();
    ITocItem getIndexParent();
    boolean hasIndexParent();
    boolean hasIndexChilds();
    List<ITocItem> getIndexChilds();
    String getKey();
    boolean hasBookmarkedChilds();
    Type getType();
    Paper getPaper();
    ITocItem getIndexAncestorWithKey(String key);
    boolean isBookmarkable();
    void setBookmark(boolean bool);
    boolean isBookmarked();
    boolean isLink();
    void setLink(ITocItem item);
    ITocItem getLink();
}
