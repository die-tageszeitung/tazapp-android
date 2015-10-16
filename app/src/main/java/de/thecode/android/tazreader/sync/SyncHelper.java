package de.thecode.android.tazreader.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.thecode.android.tazreader.provider.TazProvider;
import de.thecode.android.tazreader.sync.AccountHelper.CreateAccountException;
import de.thecode.android.tazreader.utils.Log;

/**
 * The type Sync helper.
 */
public class SyncHelper {

    /**
     * Request manual sync.
     *
     * @param context the context
     */
    public static void requestManualSync(Context context)
    {
        Log.v();
        Log.t("Requested manual sync");
        requestManualSync(context, new Bundle(),null,null);
    }

    /**
     * Request manual sync.
     *
     * @param context the context
     * @param start the start
     * @param end the end
     */
    public static void requestManualSync(Context context, Calendar start, Calendar end)
    {
        Log.v();
        requestManualSync(context, new Bundle(),start,end);
    }
    
    private static void requestManualSync(Context context, Bundle bundle, Calendar start, Calendar end)
    {
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
            Log.e(e);
        }
    }
    
    
    

}
