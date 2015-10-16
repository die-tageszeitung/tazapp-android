package de.thecode.android.tazreader.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Created by mate on 17.04.2015.
 */
public class SlidingRelativeLayout extends RelativeLayout {
    public SlidingRelativeLayout(Context context) {
        super(context);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SlidingRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public float getXFraction() {
        return (getWidth() > 0) ? getX() / getWidth() : 0F;
    }

    public float getYFraction() {
        return (getHeight() > 0) ? getY() / getHeight() : 0F;
    }


    int width = 0;
    int height = 0;

    public void setXFraction(float xFraction) {
        if (width == 0) width = getWidth();
        setTranslationX((width > 0) ? (xFraction * width) : -9999);
    }

    public void setYFraction(float yFraction) {
        if (height == 0) height = getHeight();
        setTranslationY((height > 0) ? (yFraction * height) : -9999);
    }
}
