package de.thecode.android.tazreader.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.reader.ReaderActivity;

/**
 * Created by mate on 27.07.2015.
 */
public class PageIndexButton extends ImageView {

    ColorFilter cfPressed;
    ColorFilter cfNormal;



    public PageIndexButton(Context context) {
        super(context);
        init(context);
    }

    public PageIndexButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PageIndexButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PageIndexButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        setImageDrawable(getResources().getDrawable(R.drawable.ic_apps_white_24dp));
        setScaleType(ScaleType.CENTER);
        cfPressed = new LightingColorFilter(context.getResources().getColor(R.color.index_bookmark_on), 1);
        cfNormal = new LightingColorFilter(context.getResources().getColor(R.color.index_bookmark_off), 1);
        setColorFilter(cfNormal);
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getContext() instanceof ReaderActivity){
                    ((ReaderActivity) v.getContext()).openPageIndexDrawer();
                }
            }
        });
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (pressed) {
            setColorFilter(cfPressed);
        } else {
            setColorFilter(cfNormal);
        }
    }
}
