package de.thecode.android.tazreader.data;

import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.download.DownloadFinishedPaperService;
import de.thecode.android.tazreader.download.DownloadHelper;
import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;

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
                        DownloadHelper.DownloadState state = DownloadHelper.getDownloadState(context, deletePaper.getDownloadId());
                        if (state != null && state.getStatus() == DownloadHelper.DownloadState.STATUS_SUCCESSFUL) {
                            Intent cancelIntent = new Intent(context, DownloadFinishedPaperService.class);
                            cancelIntent.putExtra(DownloadFinishedPaperService.PARAM_CANCEL_BOOL,true);
                            cancelIntent.putExtra(DownloadFinishedPaperService.PARAM_PAPER_ID, deletePaper.getId());
                            context.startService(cancelIntent);
                        }
                        else
                            DownloadHelper.cancelDownload(context, deletePaper.getDownloadId());

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
