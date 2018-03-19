package de.thecode.android.tazreader.reader.usertoc;

import android.support.annotation.NonNull;

import de.thecode.android.tazreader.data.ITocItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mate on 08.03.18.
 */

public class UserTocItem {

    private boolean childsVisible;

    private UserTocItem parent;
    private List<UserTocItem> childs = new ArrayList<>();

    private ITocItem indexItem;

    public UserTocItem(UserTocItem parent, @NonNull ITocItem indexItem) {
        this.parent = parent;
        this.indexItem = indexItem;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public void setChildsVisible(boolean childsVisible) {
        this.childsVisible = childsVisible;
        if (!childsVisible && hasChilds()) {
            for (UserTocItem child : childs) {
                child.setChildsVisible(false);
            }
        }
    }

    public boolean areChildsVisible() {
        return childsVisible;
    }

    public boolean isVisible(){
        return parent == null || parent.areChildsVisible();
    }

    public boolean hasChilds() {
        return childs.size() > 0;
    }

    public boolean hasParent() {
        return parent!=null;
    }

    public UserTocItem getParent() {
        return parent;
    }

    private void addChild(UserTocItem child){
        childs.add(child);
    }

    public List<UserTocItem> addChilds(List<ITocItem> indexItems) {
        List<UserTocItem> newChilds = new ArrayList<>();
        if (indexItems != null) {
            for (ITocItem indexItem : indexItems) {
                newChilds.add(new UserTocItem(this, indexItem));
            }
        }
        return newChilds;
    }


    public ITocItem getIndexItem() {
        return indexItem;
    }

    public String getKey() {
        return indexItem.getKey();
    }
}
