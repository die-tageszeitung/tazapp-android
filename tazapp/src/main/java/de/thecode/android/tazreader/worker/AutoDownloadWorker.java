package de.thecode.android.tazreader.worker;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.evernote.android.job.util.support.PersistableBundleCompat;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.notifications.NotificationUtils;

import java.util.concurrent.TimeUnit;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.Worker;

public class AutoDownloadWorker extends Worker {

    public static final  String TAG              = BuildConfig.FLAVOR + "_autodownload_job";
    private static final String ARG_PAPER_BOOKID = "paper_bookid";

    @NonNull
    @Override
    public Result doWork() {
        TazSettings settings = TazSettings.getInstance(getApplicationContext());
        if (settings.getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD, false)) {

            String bookId = getInputData().getString(ARG_PAPER_BOOKID, null);
            if (!TextUtils.isEmpty(bookId)) {
                PaperRepository paperRepository = PaperRepository.getInstance(getApplicationContext());
                Paper paper = paperRepository.getPaperWithBookId(bookId);
                if (paper != null) {
                    Store autoDownloadedStore = StoreRepository.getInstance(getApplicationContext())
                                                               .getStore(paper.getBookId(), Paper.STORE_KEY_AUTO_DOWNLOADED);
                    boolean isAutoDownloaded = Boolean.parseBoolean(autoDownloadedStore.getValue("false"));
                    if (!isAutoDownloaded && (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) < paper.getDateInMillis()) {
                        boolean wifiOnly = settings.getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD_WIFI, false);
                        if (!(paper.isDownloaded() || paper.isDownloading())) {
                            DownloadManager.DownloadManagerResult result = DownloadManager.getInstance(getApplicationContext())
                                                                                          .downloadPaper(bookId, wifiOnly);
                            if (result.getState() == DownloadManager.DownloadManagerResult.STATE.SUCCESS) {
                                autoDownloadedStore.setValue("true");
                                StoreRepository.getInstance(getApplicationContext())
                                               .saveStore(autoDownloadedStore);
                            } else if (result.getState() == DownloadManager.DownloadManagerResult.STATE.NOSPACE) {
                                new NotificationUtils(getApplicationContext()).showDownloadErrorNotification(paper,
                                                                                                             getApplicationContext().getString(
                                                                                                                     R.string.message_not_enough_space));
                            }
                        }
                    }
                }
            }
        }
        return Result.SUCCESS;
    }

    public static WorkRequest buildRequest(@NonNull Paper paper) {
        Data data = new Data.Builder().putString(ARG_PAPER_BOOKID, paper.getBookId())
                                      .build();
        return new OneTimeWorkRequest.Builder(AutoDownloadWorker.class).setInputData(data)
                                                                       .build();
    }
}
