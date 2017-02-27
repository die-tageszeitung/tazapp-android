package de.thecode.android.tazreader.utils;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import timber.log.Timber;

/**
 * Created by mate on 04.02.2016.
 */
public class Display {

    public static double getScreenSizeInch(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getApplicationContext()
                                                             .getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager.getDefaultDisplay()
                         .getRealMetrics(dm);
        } else {
            windowManager.getDefaultDisplay()
                         .getMetrics(dm);
        }
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y);

        Timber.d("ScreenSize: %s", screenInches);
        return screenInches;
    }
}
