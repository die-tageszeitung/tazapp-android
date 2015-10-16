package de.thecode.android.tazreader.start;

/**
 * Created by mate on 02.04.2015.
 */
public class DrawerStateChangedEvent {
    int newState;
    public DrawerStateChangedEvent(int newState) {
        this.newState = newState;
    }

    public int getNewState() {
        return newState;
    }
}
