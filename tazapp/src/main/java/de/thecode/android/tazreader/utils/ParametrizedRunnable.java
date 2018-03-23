package de.thecode.android.tazreader.utils;

/**
 * Created by mate on 21.03.18.
 */

public abstract class ParametrizedRunnable<P> implements Runnable {

    private P parameter;

    @Override
    public void run() {
        run(parameter);
    }

    public abstract void run(P parameter);

    public Runnable set(P parameter) {
        this.parameter = parameter;
        return this;
    }
}
