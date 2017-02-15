package de.thecode.android.tazreader.sync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.data.TazSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

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
    public static void requestSync(Context context) {
        log.trace("Requested manual sync");
        requestSync(context, null, null);
    }


    public static void requestSync(Context context, Calendar start, Calendar end) {
        Intent serviceIntent = new Intent(context, SyncService.class);
        if (start != null && end != null) {
            serviceIntent.putExtra(SyncService.ARG_START_DATE,
                                   new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(start.getTime()));
            serviceIntent.putExtra(SyncService.ARG_END_DATE,
                                   new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(end.getTime()));
        }
        context.startService(serviceIntent);
    }

    private static long getNextServiceTime(boolean notToday) {

        int checkFromHour1 = 19;
        int checkEveryMinutes1 = 15;
        int checkFromHour2 = 21;
        int checkEveryMinutes2 = 30;

        ArrayList<Integer> minutes1 = new ArrayList<Integer>();
        ArrayList<Integer> minutes2 = new ArrayList<Integer>();
        for (int i = 0; i <= 59; i = i + checkEveryMinutes1) {
            minutes1.add(i);
        }
        for (int i = 0; i <= 59; i = i + checkEveryMinutes2) {
            minutes2.add(i);
        }

        TimeZone berlinTimeZone = TimeZone.getTimeZone("Europe/Berlin");
        GregorianCalendar cal;
        try {
            cal = new GregorianCalendar(berlinTimeZone);
        } catch (Exception e) {
            cal = new GregorianCalendar();
        }

        cal.setTimeInMillis(System.currentTimeMillis());// set the current time and date for this calendar
        log.debug("aktuelle Zeit: {}", cal.getTime()
                                          .toString());
        log.debug("aktuelle Zeit: {}", cal.getTimeInMillis());
        if (notToday) {
            log.debug("nicht heute!");
            cal.add(DAY_OF_WEEK, 1);
            cal.set(HOUR_OF_DAY, 0);
            cal.set(MINUTE, 0);
        }
        while (cal.get(DAY_OF_WEEK) == GregorianCalendar.SATURDAY) {
            cal.add(MINUTE, 1);
        }
        cal.add(MINUTE, 1);
        if (cal.get(HOUR_OF_DAY) < checkFromHour1) {
            cal.set(HOUR_OF_DAY, checkFromHour1);
            cal.set(MINUTE, 0);
        } else if (cal.get(HOUR_OF_DAY) < checkFromHour2) {
            while (!minutes1.contains(cal.get(MINUTE))) {
                cal.add(MINUTE, 1);
            }
        } else {
            while (!minutes2.contains(cal.get(MINUTE))) {
                cal.add(MINUTE, 1);
            }
        }
        log.debug("nächste Zeit: {}", cal.getTime()
                                         .toString());
        log.debug("nächste Zeit: {}", cal.getTimeInMillis());
        return cal.getTimeInMillis();
    }

    public static void setAlarmManager(Context context, boolean notToday) {
        long nextRunAt = getNextServiceTime(notToday);
        setAlarmManager(context,nextRunAt);
    }

    public static void setAlarmManager(Context context, long runAt) {
        TazSettings.getInstance(context)
                   .setSyncServiceNextRun(runAt);
        Intent serviceIntent = new Intent(context.getApplicationContext(), SyncService.class);
        PendingIntent pendingServiceIntent = PendingIntent.getService(context.getApplicationContext(), 0, serviceIntent,
                                                                      PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //TODO maybe we want to use alarm.setAndAllowWhileIdle() because of Doze mode?
        alarm.set(AlarmManager.RTC_WAKEUP, runAt, pendingServiceIntent);

    }

}
