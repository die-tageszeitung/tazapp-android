package de.thecode.android.tazreader.timber;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.TazSettings;

import timber.log.Timber;

public class TimberHelper extends ContextWrapper {

    private static volatile TimberHelper mInstance;

    public static void initialize(Context context) {
        if (mInstance == null) {
            synchronized (TimberHelper.class) {
                if (mInstance == null) {
                    mInstance = new TimberHelper(context.getApplicationContext());
                }
            }
        }
    }


    private Timber.Tree loggingTree;
    private Timber.Tree fileLoggingTree;

    private TimberHelper(Context base) {
        super(base);
        TazSettings settings = TazSettings.getInstance(this);

        settings.addOnPreferenceChangeListener(TazSettings.PREFKEY.LOGFILE, this::forrestFileLoggingTree);

        forestLoggingTree();
        forrestFileLoggingTree(settings.isWriteLogfile());
    }

    private void forestLoggingTree() {
        if (loggingTree != null) Timber.uproot(loggingTree);

        loggingTree = new TazTimberTree(BuildConfig.DEBUG ? Log.VERBOSE : Log.WARN);
        Timber.plant(loggingTree);
    }

    private void forrestFileLoggingTree(boolean writeToFile) {
        if (fileLoggingTree != null) {
            Timber.uproot(fileLoggingTree);
            fileLoggingTree = null;
        }
        if (writeToFile) {
            fileLoggingTree = new FLTimberTree(this);
            Timber.plant(fileLoggingTree);
        }
    }

}
