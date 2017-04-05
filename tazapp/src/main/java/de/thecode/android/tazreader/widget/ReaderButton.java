package de.thecode.android.tazreader.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import de.thecode.android.tazreader.R;

/**
 * Created by mate on 27.07.2015.
 */
public class ReaderButton extends AppCompatImageView {

    int colorNormal;
    int colorPressed;


    public ReaderButton(Context context) {
        super(context);
        init(context);
    }

    public ReaderButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReaderButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.CENTER);
        colorPressed = ContextCompat.getColor(context, R.color.index_bookmark_on);
        colorNormal = ContextCompat.getColor(context,R.color.index_bookmark_off);
        tintDrawable(colorNormal);
        setClickable(true);
//        setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (v.getContext() instanceof ReaderActivity) {
//                    ((ReaderActivity) v.getContext()).openPageIndexDrawer();
//                }
//            }
//        });
//        setOnLongClickListener(new OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                Toast.makeText(v.getContext(), R.string.reader_action_pageindex, Toast.LENGTH_LONG)
//                     .show();
//                return true;
//            }
//        });
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (pressed) {
            tintDrawable(colorPressed);
        } else {
            tintDrawable(colorNormal);
        }
    }

    private void tintDrawable(int color) {
        Drawable wrappedDrawable = DrawableCompat.wrap(getDrawable());
        wrappedDrawable = wrappedDrawable.mutate();
        DrawableCompat.setTint(wrappedDrawable, color);
    }
}
