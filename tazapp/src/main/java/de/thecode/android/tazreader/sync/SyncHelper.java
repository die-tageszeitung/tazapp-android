package de.thecode.android.tazreader.sync;

import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * The type Sync helper.
 */
public class SyncHelper {

    private static final Logger log = LoggerFactory.getLogger(SyncHelper.class);

    /**
     * Request manual sync.
     *
     * @param context the context
     */
    public static void requestManualSync(Context context) {
        log.trace("Requested manual sync");
        requestManualSync(context, null, null);
    }


    public static void requestManualSync(Context context, Calendar start, Calendar end) {
        Intent serviceIntent = new Intent(context,SyncService.class);
        if (start != null && end != null) {
            serviceIntent.putExtra(SyncService.ARG_START_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(start.getTime()));
            serviceIntent.putExtra(SyncService.ARG_END_DATE, new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(end.getTime()));
        }
        context.startService(serviceIntent);
    }


}
