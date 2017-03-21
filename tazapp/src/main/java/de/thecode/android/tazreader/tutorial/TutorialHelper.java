package de.thecode.android.tazreader.tutorial;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import de.thecode.android.tazreader.R;

/**
 * Created by mate on 21.03.2017.
 */

public class TutorialHelper {

    public static TapTargetView show(Activity activity, TapTarget tapTarget, TapTargetView.Listener listener) {
        return TapTargetView.showFor(activity,makeDefault(activity,tapTarget),listener);
    }


    public static TapTarget makeDefault(Context context, TapTarget tapTarget) {
        tapTarget
                 .textTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/TazWt05-Regular.otf"))
                 .titleTextColor(R.color.mtt_primaryTextColour)
                 .descriptionTextColor(R.color.mtt_secondaryTextColour)
                 .transparentTarget(true)
                 .outerCircleColor(R.color.mtt_backgroundColour);
        return tapTarget;
    }

    public static abstract class TapTargetListener extends TapTargetView.Listener {
        @Override
        public void onOuterCircleClick(TapTargetView view) {
            onTargetCancel(view);
        }
    }

}
