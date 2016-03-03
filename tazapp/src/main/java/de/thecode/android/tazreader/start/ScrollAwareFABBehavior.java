package de.thecode.android.tazreader.start;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thecode.android.tazreader.sync.AccountHelper;

/**
 * Created by mate on 03.03.2016.
 */
public class ScrollAwareFABBehavior extends FloatingActionButton.Behavior {

    private static final Logger log = LoggerFactory.getLogger(ScrollAwareFABBehavior.class);

    AccountHelper mAccountHelper;
    boolean isAuthenticated;

    public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
        super();
        try {
            mAccountHelper = new AccountHelper(context);
        } catch (AccountHelper.CreateAccountException e) {

        }
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {
        if (mAccountHelper != null) {
            isAuthenticated = mAccountHelper.isAuthenticated();
        }
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL && isAuthenticated;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (dyConsumed > 0) child.hide();
        else child.show();
    }

}
