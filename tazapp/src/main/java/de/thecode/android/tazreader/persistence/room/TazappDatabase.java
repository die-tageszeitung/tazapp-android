package de.thecode.android.tazreader.persistence.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.Store;

/**
 * Created by mate on 11.12.2017.
 */

@Database(entities = {Paper.class, Publication.class, Store.class, Resource.class}, version = 1)
public abstract class TazappDatabase extends RoomDatabase {

    private static volatile TazappDatabase instance;

    public static final String DB_NAME = BuildConfig.FLAVOR+".db";

    public static synchronized TazappDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
        }
        return instance;
    }

    private static TazappDatabase create(final Context context) {
        return Room.databaseBuilder(context, TazappDatabase.class, DB_NAME)
                   .addMigrations(Migrations.getAllmigrations())
                   .build();
    }

    public abstract PaperDao paperDao();
    public abstract PublicationDao publicationDao();

}
