package de.thecode.android.tazreader.utils;

import com.crashlytics.android.Crashlytics;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Created by mate on 25.01.2016.
 */
public class CrashlyticsAppender extends AppenderBase<ILoggingEvent> {

    private PatternLayoutEncoder encoder;

    @Override
    public void start()
    {
        if (encoder == null || encoder.getLayout() == null)
        {
            addError("No layout set for the appender named [" + name + "].");
            return;
        }

        super.start();
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        if (!isStarted())
            return;

        String layoutString = encoder.getLayout().doLayout(loggingEvent);

        Crashlytics.log(layoutString);
    }

    public void setEncoder(PatternLayoutEncoder encoder)
    {
        this.encoder = encoder;
    }
}
