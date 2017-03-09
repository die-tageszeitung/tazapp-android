package de.thecode.android.tazreader.utils;

/**
 * Created by Mate on 17.02.2017.
 */

public abstract class RunnableExtended implements Runnable{

    private final Object[] objects;

    protected RunnableExtended(Object... objects) {
        this.objects = objects;
    }

    public Object getObject(int index){
        return objects[index];
    }
}
