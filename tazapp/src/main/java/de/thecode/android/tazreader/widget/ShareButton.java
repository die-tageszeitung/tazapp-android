package de.thecode.android.tazreader.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import de.thecode.android.tazreader.R;

/**
 * Created by mate on 22.01.2015.
 */
public class ShareButton extends ImageView implements View.OnClickListener, View.OnLongClickListener {

    ColorFilter cfPressed;
    ColorFilter cfNormal;

    Context mContext;
    ShareButtonCallback mCallback;


    public ShareButton(Context context) {
        super(context);
        init(context);
    }

    public ShareButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ShareButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ShareButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setImageDrawable(getResources().getDrawable(R.drawable.ic_share_24dp));
        setScaleType(ScaleType.CENTER);
        cfPressed = new LightingColorFilter(context.getResources()
                                                   .getColor(R.color.index_bookmark_on), 1);
        cfNormal = new LightingColorFilter(context.getResources()
                                                  .getColor(R.color.index_bookmark_off), 1);
        setColorFilter(cfNormal);
        setClickable(true);
        setOnClickListener(this);
        setOnLongClickListener(this);
        if (!isInEditMode()) setVisibility(View.INVISIBLE);
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

    public void setCallback(ShareButtonCallback callback) {
        this.mCallback = callback;
        if (!mCallback.isShareable()) {
            setVisibility(View.INVISIBLE);
        } else setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (mCallback != null) {
            Intent intent = mCallback.getShareIntent(mContext);
            if (intent != null) {
                mContext.startActivity(intent);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mContext != null) {
            Toast.makeText(mContext, R.string.reader_action_share, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    public interface ShareButtonCallback {
        public Intent getShareIntent(Context context);

        public boolean isShareable();
    }
}
