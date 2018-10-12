package de.thecode.android.tazreader.utils;

import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Created by mate on 03.03.2016.
 */
public class TintHelper {

    public static void tintDrawable(@NonNull Drawable drawable, @ColorInt int color) {
        drawable = DrawableCompat.wrap(drawable);
        if (drawable != null) {
            drawable = drawable.mutate();
            DrawableCompat.setTint(drawable, color);
        }
    }

    public static Drawable tintAndReturnDrawable(@NonNull Drawable drawable, @ColorInt int color) {
        tintDrawable(drawable,color);
        return drawable;
    }
}
