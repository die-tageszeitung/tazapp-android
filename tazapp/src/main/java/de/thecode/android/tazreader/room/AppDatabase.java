package de.thecode.android.tazreader.room;

import android.content.Context;

import de.thecode.android.tazreader.data.Download;
import de.thecode.android.tazreader.data.DownloadStateTypeConverter;
import de.thecode.android.tazreader.data.DownloadTypeTypeConverter;
import de.thecode.android.tazreader.data.FileTypeConverter;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.UuidTypeConverter;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Paper.class, Resource.class, Store.class, Publication.class, Download.class}, version = AppDatabase.VERSION)
@TypeConverters({DownloadStateTypeConverter.class, DownloadTypeTypeConverter.class, UuidTypeConverter.class, FileTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public static final  int    VERSION = 10;
    private static final String DB_NAME = "db";

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
        }
        return instance;
    }

    private static AppDatabase create(final Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, DB_NAME)
                   .addMigrations(Migrations.getAllmigrations())
                   .build();
    }

    public abstract PaperDao paperDao();

    public abstract PublicationDao publicationDao();

    public abstract ResourceDao resourceDao();

    public abstract StoreDao storeDao();

    public abstract DownloadsDao downloadsDao();
}
