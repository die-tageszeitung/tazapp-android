package de.thecode.android.tazreader;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

import de.thecode.android.tazreader.push.PushRestApiJob;

/**
 * Created by mate on 25.07.2017.
 */

public class TazJobCreator implements JobCreator {

    @Override
    public Job create(String tag) {
        switch (tag) {
            case PushRestApiJob.TAG:
                return new PushRestApiJob();
            default:
                return null;
        }
    }

}
