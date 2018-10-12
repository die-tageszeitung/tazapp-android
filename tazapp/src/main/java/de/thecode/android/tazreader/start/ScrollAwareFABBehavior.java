package de.thecode.android.tazreader.start;

import android.content.Context;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import de.thecode.android.tazreader.data.TazSettings;

/**
 * Created by mate on 03.03.2016.
 */
public class ScrollAwareFABBehavior extends FloatingActionButton.Behavior {

    private TazSettings tazSettings;

    public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
        super();
        tazSettings = TazSettings.getInstance(context);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View directTargetChild,
                                       View target, int nestedScrollAxes) {
        boolean isAuthenticated = false;
        if (tazSettings != null) {
            isAuthenticated = !tazSettings.isDemoMode();
        }
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL && isAuthenticated;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View target, int dxConsumed,
                               int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (dyConsumed > 0) child.hide();
        else child.show();
    }

}
