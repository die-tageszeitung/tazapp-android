package de.thecode.android.tazreader.timber;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import com.bosphere.filelogger.FL;
import com.bosphere.filelogger.FLConfig;
import com.bosphere.filelogger.FileFormatter;

import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.UserDeviceInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class FLTimberTree extends Timber.DebugTree {

    FLTimberTree(Context context) {
        FL.init(new FLConfig.Builder(context).dir(StorageManager.getInstance(context)
                                                                .getLogCache())
                                             .formatter(new TazFileLogFormatter(UserDeviceInfo.getInstance(context)
                                                                                              .getInstallationId()))
                                             .logger(null) //no output to Catlog
                                             .logToFile(true)
                                             .maxTotalSize(10 * 1024 * 1024)
                                             .build());
        FL.setEnabled(true);


    }

    @Override
    protected String createStackElementTag(@NonNull StackTraceElement element) {
        return super.createStackElementTag(element) + "." + element.getMethodName() + ":" + element.getLineNumber() + "[" + Thread.currentThread()
                                                                                                                                  .getName() + "]";
    }

    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        switch (priority) {
            case Log.VERBOSE:
                FL.v(tag, message);
                break;
            case Log.DEBUG:
                FL.d(tag, message);
                break;
            case Log.INFO:
                FL.i(tag, message);
                break;
            case Log.WARN:
                FL.w(tag, message);
                break;
            case Log.ERROR:
                FL.e(tag, t, message);
                break;
        }
    }

    public static class TazFileLogFormatter implements FileFormatter {

        private final String installationsId;

        private final ThreadLocal<SimpleDateFormat> mTimeFmt = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat("dd.MM HH:mm:ss.SSS", Locale.GERMAN);
            }
        };

        private final ThreadLocal<SimpleDateFormat> mFileNameFmt = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
            }
        };

        private final ThreadLocal<Date> mDate = new ThreadLocal<Date>() {
            @Override
            protected Date initialValue() {
                return new Date();
            }
        };

        // 09-23 12:31:53.839 PROCESS_ID-THREAD_ID LEVEL/TAG: LOG
        private final String mLineFmt = "%s %d-%d %s/%s: %s";

        TazFileLogFormatter(String installationsId) {
            this.installationsId = installationsId;
        }

        @Override
        public String formatLine(long timeInMillis, String level, String tag, String log) {
            mDate.get()
                 .setTime(timeInMillis);
            String timestamp = mTimeFmt.get()
                                       .format(mDate.get());
            int processId = Process.myPid();
            int threadId = Process.myTid();
            return String.format(Locale.GERMAN, mLineFmt, timestamp, processId, threadId, level, tag, log);
        }

        @Override
        public String formatFileName(long timeInMillis) {
            mDate.get()
                 .setTime(timeInMillis);
            return installationsId + "_" + mFileNameFmt.get()
                               .format(mDate.get()) + ".log";
        }
    }
}
