package de.thecode.android.tazreader.worker;

import android.content.Context;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.OldDownloadManager;
import de.thecode.android.tazreader.notifications.NotificationUtils;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Result;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

public class AutoDownloadWorker extends LoggingWorker {

    private static final String TAG_PREFIX       = BuildConfig.FLAVOR + "_autodownload_job_";
    private static final String ARG_PAPER_BOOKID = "paper_bookid";

    private final TazSettings        settings;
    private final PaperRepository    paperRepository;
    private final StoreRepository    storeRepository;
    private final OldDownloadManager downloadManager;
    private final NotificationUtils  notificationUtils;

    public AutoDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        settings = TazSettings.getInstance(context);
        paperRepository = PaperRepository.getInstance(context);
        storeRepository = StoreRepository.getInstance(context);
        downloadManager = OldDownloadManager.getInstance(context);
        notificationUtils = NotificationUtils.getInstance(context);
    }

    @NonNull
    @Override
    public Result doBackgroundWork() {
        if (settings.getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false)) {
            String bookId = getInputData().getString(ARG_PAPER_BOOKID);
            if (!TextUtils.isEmpty(bookId)) {
                Paper paper = paperRepository.getPaperWithBookId(bookId);
                if (paper != null) {
                    Store autoDownloadedStore = storeRepository.getStore(paper.getBookId(), Paper.STORE_KEY_AUTO_DOWNLOADED);
                    boolean isAutoDownloaded = Boolean.parseBoolean(autoDownloadedStore.getValue("false"));
                    if (!isAutoDownloaded && (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) < paper.getDateInMillis()) {
                        boolean wifiOnly = settings.getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD_WIFI, false);
                        if (paper.hasNoneState()) {
                            OldDownloadManager.DownloadManagerResult result = downloadManager.downloadPaper(bookId, wifiOnly);
                            if (result.getState() == OldDownloadManager.DownloadManagerResult.STATE.SUCCESS) {
                                autoDownloadedStore.setValue("true");
                                storeRepository.saveStore(autoDownloadedStore);
                            } else if (result.getState() == OldDownloadManager.DownloadManagerResult.STATE.NOSPACE) {
                                notificationUtils.showDownloadErrorNotification(paper,
                                                                                getApplicationContext().getString(R.string.message_not_enough_space));
                            }
                        }
                    }
                }
            }
        }
        return Result.success();
    }

    public static String getTag(@NonNull String bookId) {
        return TAG_PREFIX + bookId;
    }

    public static void scheduleNow(@NonNull Paper paper) {

        String tag = getTag(paper.getBookId());

        WorkManager.getInstance().cancelAllWorkByTag(tag);

        Data data = new Data.Builder().putString(ARG_PAPER_BOOKID, paper.getBookId())
                                      .build();

        WorkRequest request = new OneTimeWorkRequest.Builder(AutoDownloadWorker.class).setInputData(data)
                                                                                      .addTag(tag)
                                                                                      .addTag(paper.getBookId())
                                                                                      .build();
        WorkManager.getInstance()
                   .enqueue(request);
    }
}
