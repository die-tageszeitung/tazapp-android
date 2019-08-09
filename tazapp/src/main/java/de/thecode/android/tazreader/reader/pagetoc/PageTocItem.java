package de.thecode.android.tazreader.reader.pagetoc;

import android.graphics.Bitmap;

import de.thecode.android.tazreader.data.ITocItem;

/**
 * Created by mate on 22.03.18.
 */

public class PageTocItem implements Cloneable {

    private final ITocItem indexItem;
    private       Bitmap   overlayBitmap;


    PageTocItem(ITocItem indexItem) {
        this.indexItem = indexItem;
    }

    void setOverlayBitmap(Bitmap overlayBitmap) {
        this.overlayBitmap = overlayBitmap;
    }

    Bitmap getOverlayBitmap() {
        return overlayBitmap;
    }

    boolean hasOverlayBitmap() {
        return overlayBitmap != null;
    }

    void removeOverlayBitmap() {
        overlayBitmap = null;
    }

    public ITocItem getIndexItem() {
        return indexItem;
    }

    public String getKey(){
        return indexItem.getKey();
    }

    @Override
    public PageTocItem clone() throws CloneNotSupportedException {
        return (PageTocItem) super.clone();
    }
}
