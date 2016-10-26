package de.thecode.android.tazreader.data;

import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thecode.android.tazreader.download.DownloadFinishedPaperService;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;

/**
 * Created by mate on 11.08.2015.
 */
public abstract class DeleteTask extends AsyncTaskWithExecption<Long, Void, Void> {

    private static final Logger log = LoggerFactory.getLogger(DeleteTask.class);

    private Context context;

    public DeleteTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Void doInBackgroundWithException(Long... params) throws Exception {
        if (params != null) {
            for (Long paperId : params) {
                try {
                    Paper deletePaper = new Paper(context, paperId);
                    if (deletePaper.isDownloading()) {
                        DownloadManager downloadManager = DownloadManager.getInstance(context);
                        DownloadManager.DownloadState state = downloadManager.getDownloadState(deletePaper.getDownloadId());
                        if (state != null && state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
                            Intent cancelIntent = new Intent(context, DownloadFinishedPaperService.class);
                            cancelIntent.putExtra(DownloadFinishedPaperService.PARAM_CANCEL_BOOL,true);
                            cancelIntent.putExtra(DownloadFinishedPaperService.PARAM_PAPER_ID, deletePaper.getId());
                            context.startService(cancelIntent);
                        }
                        else
                            DownloadManager.getInstance(context).cancelDownload(deletePaper.getDownloadId());

                    }
                        deletePaper.delete(context);

                } catch (Paper.PaperNotFoundException e) {
                    log.warn("Cannot delete Paper:",e);
                }
            }
        }
        return null;
    }
}
