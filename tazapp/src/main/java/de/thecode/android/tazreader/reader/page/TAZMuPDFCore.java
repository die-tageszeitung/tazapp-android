package de.thecode.android.tazreader.reader.page;

import android.content.Context;
import android.graphics.PointF;

import com.artifex.mupdfdemo.MuPDFCore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TAZMuPDFCore extends MuPDFCore {

    private static final Logger log = LoggerFactory.getLogger(TAZMuPDFCore.class);
    
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
        log.debug("{}",tag);
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
        log.debug(tag);
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
