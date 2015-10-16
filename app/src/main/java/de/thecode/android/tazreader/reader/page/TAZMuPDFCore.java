package de.thecode.android.tazreader.reader.page;

import android.content.Context;
import android.graphics.PointF;

import com.artifex.mupdfdemo.MuPDFCore;

import de.thecode.android.tazreader.utils.Log;


public class TAZMuPDFCore extends MuPDFCore {
    
    PointF pageSize;
    String tag;
    boolean isDestroyed = false;


    public TAZMuPDFCore(Context context, String filename) throws Exception {
        super(context, filename);
        int lastIndex = filename.lastIndexOf("/");
        if (lastIndex != 0) lastIndex+=1;
        tag = filename.substring(lastIndex);
        init();
    }


    public TAZMuPDFCore(Context context, byte[] buffer, String magic) throws Exception {
        super(context, buffer, magic);
        tag = String.valueOf(System.currentTimeMillis());
        init();
    }

    private void init() {
        isDestroyed = false;
        Log.v(tag);
        this.countPages();
        pageSize = this.getPageSize(0);
    }
    
    
    public PointF getPageSize() {
        return pageSize;
    }

    public void setPageSize(PointF pageSize) {
        this.pageSize = pageSize;
    }


    @Override
    public synchronized void onDestroy() {
        Log.v(tag);
        super.onDestroy();
        isDestroyed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        if (!isDestroyed)
            onDestroy();
        super.finalize();
    }
}
