package de.thecode.android.tazreader.sync;

/**
 * Created by mate on 06.03.2015.
 */
public class SyncStateChangedEvent {
    private boolean running;

    /**
     * Instantiates a new Sync state changed event.
     *
     * @param running the running
     */
    public SyncStateChangedEvent(boolean running) {
        this.running = running;
    }

    /**
     * Is running.
     *
     * @return the boolean
     */
    public boolean isRunning() {
        return running;
    }
}
