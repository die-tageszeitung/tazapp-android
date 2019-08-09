package de.thecode.android.tazreader.sync;

import com.squareup.picasso.Callback;

import de.thecode.android.tazreader.data.Paper;

/**
 * Created by mate on 01.03.18.
 */

public abstract class PreloadImageCallback implements Callback {

    private final Paper paper;


    protected PreloadImageCallback(Paper paper) {
        this.paper = paper;

    }

    @Override
    public void onSuccess() {
        onSuccess(paper);
    }

    @Override
    public void onError() {
        onError(paper);
    }

    public abstract void onSuccess(Paper paper);

    public abstract void onError(Paper paper);
}
