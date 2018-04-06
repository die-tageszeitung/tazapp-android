package de.thecode.android.tazreader.data;

import android.content.Context;
import android.support.annotation.WorkerThread;

import de.thecode.android.tazreader.room.AppDatabase;

public class PublicationRepository {


    private static volatile PublicationRepository mInstance;

    public static PublicationRepository getInstance(Context context) {
        if (mInstance == null) {
            synchronized (PublicationRepository.class) {
                if (mInstance == null) {
                    mInstance = new PublicationRepository(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private final AppDatabase appDatabase;

    private PublicationRepository(Context context) {
        appDatabase = AppDatabase.getInstance(context);
    }

    @WorkerThread
    public void savePublication(Publication publication){
        appDatabase.publicationDao().insert(publication);
    }


}
