package de.thecode.android.tazreader.notifications;

/**
 * Created by mate on 12.10.2017.
 */

public class NewNotificationHelper {

    private static volatile NewNotificationHelper mInstance;

    public static NewNotificationHelper getInstance() {
        if (mInstance == null) {
            synchronized (NewNotificationHelper.class) {
                if (mInstance == null) {
                    mInstance = new NewNotificationHelper();
                }
            }
        }
        return mInstance;
    }


    private NewNotificationHelper() {
    }

}
