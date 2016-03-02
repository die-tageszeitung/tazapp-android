package de.thecode.android.tazreader.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomToolbar extends Toolbar {

    private static final Logger log = LoggerFactory.getLogger(CustomToolbar.class);

    public CustomToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    public CustomToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public CustomToolbar(Context context) {
        super(context);

    }


    int itemColor;


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        colorizeToolbar(this, itemColor);
        super.onLayout(changed, l, t, r, b);
    }

    public void setItemColor(final int color) {
        itemColor = color;
        colorizeToolbar(CustomToolbar.this, color);
    }



    /**
     * Use this method to colorize toolbar icons to the desired target color
     *
     * @param toolbarView       toolbar view being colored
     * @param toolbarIconsColor the target color of toolbar icons

     */
    public static void colorizeToolbar(Toolbar toolbarView, int toolbarIconsColor) {


        for (int i = 0; i < toolbarView.getChildCount(); i++) {
            final View v = toolbarView.getChildAt(i);

            doColorizing(v, toolbarIconsColor);
        }

        //Step 3: Changing the color of title and subtitle.
        toolbarView.setTitleTextColor(toolbarIconsColor);
        toolbarView.setSubtitleTextColor(toolbarIconsColor);
    }

    public static void doColorizing(View v, int toolbarIconsColor) {
        if (v instanceof ImageButton) {
            tintDrawable(((ImageButton)v).getDrawable(),toolbarIconsColor);
        }

        if (v instanceof ImageView) {
            tintDrawable(((ImageView) v).getDrawable(), toolbarIconsColor);
        }

        if (v instanceof AutoCompleteTextView) {
            ((AutoCompleteTextView) v).setTextColor(toolbarIconsColor);
        }

        if (v instanceof TextView) {
            ((TextView) v).setTextColor(toolbarIconsColor);
        }

        if (v instanceof EditText) {
            ((EditText) v).setTextColor(toolbarIconsColor);
        }

        if (v instanceof ViewGroup) {
            for (int lli = 0; lli < ((ViewGroup) v).getChildCount(); lli++) {
                doColorizing(((ViewGroup) v).getChildAt(lli), toolbarIconsColor);
            }
        }

        if (v instanceof ActionMenuView) {
            for (int j = 0; j < ((ActionMenuView) v).getChildCount(); j++) {

                //Step 2: Changing the color of any ActionMenuViews - icons that
                //are not back button, nor text, nor overflow menu icon.
                final View innerView = ((ActionMenuView) v).getChildAt(j);

                if (innerView instanceof ActionMenuItemView) {
                    int drawablesCount = ((ActionMenuItemView) innerView).getCompoundDrawables().length;
                    for (int k = 0; k < drawablesCount; k++) {
                        if (((ActionMenuItemView) innerView).getCompoundDrawables()[k] != null) {
                            tintDrawable(((ActionMenuItemView) innerView).getCompoundDrawables()[k],toolbarIconsColor);
                        }
                    }
                }
            }
        }
    }

    private static void tintDrawable(Drawable drawable, int color) {
        Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
        wrappedDrawable = wrappedDrawable.mutate();
        DrawableCompat.setTint(wrappedDrawable, color);
    }


}
