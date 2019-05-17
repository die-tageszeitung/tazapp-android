package de.thecode.android.tazreader.widget;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import de.thecode.android.tazreader.R;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by mate on 22.01.2015.
 */
public class ShareButton extends AppCompatImageView implements View.OnClickListener, View.OnLongClickListener {

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

    private void init(Context context) {
        mContext = context;
        setImageResource(R.drawable.ic_share_24dp);
        setClickable(true);
        setOnClickListener(this);
        setOnLongClickListener(this);
        if (!isInEditMode()) setVisibility(View.INVISIBLE);
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
        Intent getShareIntent(Context context);
        boolean isShareable();
    }
}
