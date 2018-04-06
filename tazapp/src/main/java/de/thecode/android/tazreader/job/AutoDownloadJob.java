package de.thecode.android.tazreader.job;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.notifications.NotificationUtils;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by mate on 10.10.2017.
 */

public class AutoDownloadJob extends Job {

    public static final String TAG = BuildConfig.FLAVOR + "_autodownload_job";

    private static final String ARG_PAPER_BOOKID = "paper_bookid";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {

        TazSettings settings = TazSettings.getInstance(getContext());
        if (settings.getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false)) {

            PersistableBundleCompat extras = params.getExtras();
            String bookId = extras.getString(ARG_PAPER_BOOKID,null);
            if (!TextUtils.isEmpty(bookId)) {
                Paper paper = Paper.getPaperWithBookId(getContext(), bookId);
                if (paper != null) {
                        Store autoDownloadedStore = StoreRepository.getInstance(getContext())
                                                                   .getStore(paper.getBookId(), Paper.STORE_KEY_AUTO_DOWNLOADED);
                        boolean isAutoDownloaded = Boolean.parseBoolean(autoDownloadedStore.getValue("false"));
                        if (!isAutoDownloaded && (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) < paper.getDateInMillis()) {
                            boolean wifiOnly = TazSettings.getInstance(getContext())
                                                          .getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD_WIFI, false);
                            if (!(paper.isDownloaded() || paper.isDownloading())) {
                                DownloadManager.DownloadManagerResult result = DownloadManager.getInstance(getContext()).downloadPaper(bookId, wifiOnly);
                                if (result.getState() == DownloadManager.DownloadManagerResult.STATE.SUCCESS){
                                    autoDownloadedStore.setValue("true");
                                    StoreRepository.getInstance(getContext())
                                                   .saveStore(autoDownloadedStore);
                                } else if (result.getState() == DownloadManager.DownloadManagerResult.STATE.NOSPACE) {
                                    new NotificationUtils(getContext()).showDownloadErrorNotification(paper, getContext().getString(R.string.message_not_enough_space));
                                }
                            }
                        }
                }
            }
        }
        return Result.SUCCESS;
    }

    public static void scheduleJob(@NonNull Paper paper) {
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putString(ARG_PAPER_BOOKID, paper.getBookId());

        new JobRequest.Builder(TAG).setExtras(extras)
                                   .startNow()
                                   .build()
                                   .schedule();
    }
}
