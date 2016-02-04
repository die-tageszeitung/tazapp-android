package de.thecode.android.tazreader.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.thecode.android.tazreader.provider.TazProvider;
import de.thecode.android.tazreader.sync.AccountHelper.CreateAccountException;

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
        requestManualSync(context, new Bundle(), null, null);
    }

    /**
     * Request manual sync.
     *
     * @param context the context
     * @param start   the start
     * @param end     the end
     */
    public static void requestManualSync(Context context, Calendar start, Calendar end) {
        log.trace("");
        requestManualSync(context, new Bundle(), start, end);
    }

    private static void requestManualSync(Context context, Bundle bundle, Calendar start, Calendar end) {
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        if (start != null && end != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            bundle.putString(SyncAdapter.ARG_START_DATE, sdf.format(start.getTime()));
            bundle.putString(SyncAdapter.ARG_END_DATE, sdf.format(end.getTime()));
        }

        try {
            ContentResolver.requestSync(new AccountHelper(context).getAccount(), TazProvider.AUTHORITY, bundle);
        } catch (CreateAccountException e) {
            log.error("",e);
        }
    }


}
