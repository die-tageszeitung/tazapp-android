package de.thecode.android.tazreader.data;

import android.content.Context;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;

import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.job.DownloadFinishedPaperJob;
import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;

import java.util.Set;

import timber.log.Timber;

/**
 * Created by mate on 11.08.2015.
 */
public abstract class DeleteTask extends AsyncTaskWithExecption<Long, Void, Void> {

    private Context context;

    public DeleteTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Void doInBackgroundWithException(Long... params) throws Exception {
        if (params != null) {
            for (Long paperId : params) {
                try {
                    Paper deletePaper = Paper.getPaperWithId(context, paperId);
                    if (deletePaper == null) throw new Paper.PaperNotFoundException();
                    if (deletePaper.isDownloading()) {
                        DownloadManager downloadManager = DownloadManager.getInstance(context);
                        DownloadManager.DownloadState state = downloadManager.getDownloadState(deletePaper.getDownloadId());
                        if (state != null && state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
                            Set<Job> unzipJobSet = JobManager.instance().getAllJobsForTag(DownloadFinishedPaperJob.TAG);
                            for (Job unzipJob : unzipJobSet) {
                                if (unzipJob != null && unzipJob instanceof DownloadFinishedPaperJob) {
                                    if (((DownloadFinishedPaperJob) unzipJob).getCurrentPaperId() == paperId) {
                                        ((DownloadFinishedPaperJob) unzipJob).cancelIt();
                                    }
                                }
                            }

//                            Intent cancelIntent = new Intent(context, DownloadFinishedPaperService.class);
//                            cancelIntent.putExtra(DownloadFinishedPaperService.PARAM_CANCEL_BOOL,true);
//                            cancelIntent.putExtra(DownloadFinishedPaperService.PARAM_PAPER_ID, deletePaper.getId());
//                            context.startService(cancelIntent);
                        }
                        else
                            DownloadManager.getInstance(context).cancelDownload(deletePaper.getDownloadId());

                    }
                        deletePaper.delete(context);

                } catch (Paper.PaperNotFoundException e) {
                    Timber.w(e, "Cannot delete Paper");
                }
            }
        }
        return null;
    }
}
