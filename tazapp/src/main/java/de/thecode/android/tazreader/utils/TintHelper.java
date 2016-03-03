package de.thecode.android.tazreader.utils;

import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;

/**
 * Created by mate on 03.03.2016.
 */
public class TintHelper {

    public static void tintDrawable(Drawable drawable, int color) {
        Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
        wrappedDrawable = wrappedDrawable.mutate();
        DrawableCompat.setTint(wrappedDrawable, color);
    }

}
