package de.thecode.android.tazreader.room;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.Resource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by mate on 10.01.2018.
 */

public class Migrations {

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("Migration 6->7");
            database.execSQL("ALTER TABLE " + Publication.TABLE_NAME + " RENAME TO " + Publication.TABLE_NAME + "_OLD");
            database.execSQL("CREATE TABLE `" + Publication.TABLE_NAME + "` (`_id` INTEGER, `issueName` TEXT, `typeName` TEXT, `name` TEXT, `url` TEXT, `image` TEXT, `created` INTEGER NOT NULL, `validUntil` INTEGER NOT NULL, PRIMARY KEY(`_id`))");
            database.execSQL("INSERT INTO " + Publication.TABLE_NAME + " SELECT _id, issueName, typeName, name, url, image, created, validUntil FROM " + Publication.TABLE_NAME + "_OLD;");
            database.execSQL("DROP TABLE " + Publication.TABLE_NAME + "_OLD");

            database.execSQL("ALTER TABLE " + Paper.TABLE_NAME + " RENAME TO " + Paper.TABLE_NAME + "_OLD");
            database.execSQL("CREATE TABLE `" + Paper.TABLE_NAME + "` (`_id` INTEGER, `date` TEXT, `image` TEXT, `imageHash` TEXT, `link` TEXT, `fileHash` TEXT, `len` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, `bookId` TEXT, `isDemo` INTEGER NOT NULL, `hasUpdate` INTEGER NOT NULL, `downloadId` INTEGER NOT NULL, `isDownloaded` INTEGER NOT NULL, `kiosk` INTEGER NOT NULL, `imported` INTEGER NOT NULL, `title` TEXT, `publicationId` INTEGER, `resource` TEXT, `validUntil` INTEGER NOT NULL, PRIMARY KEY(`_id`))");
            database.execSQL("INSERT INTO " + Paper.TABLE_NAME + " SELECT _id, date, image, imageHash, link, fileHash, len, lastModified, bookId, isDemo, hasUpdate, downloadId, isDownloaded, kiosk, imported, title, publicationId, resource, validUntil FROM " + Paper.TABLE_NAME + "_OLD;");
            database.execSQL("DROP TABLE " + Paper.TABLE_NAME + "_OLD");

            database.execSQL("ALTER TABLE " + Resource.TABLE_NAME + " RENAME TO " + Resource.TABLE_NAME + "_OLD");
            database.execSQL("CREATE TABLE `" + Resource.TABLE_NAME + "` (`key` TEXT NOT NULL, `downloadID` INTEGER NOT NULL, `downloaded` INTEGER NOT NULL, `url` TEXT, `fileHash` TEXT, `len` INTEGER NOT NULL, PRIMARY KEY(`key`))");
            database.execSQL("INSERT INTO " + Resource.TABLE_NAME + " SELECT key, downloadID, downloaded, url, fileHash, len FROM " + Resource.TABLE_NAME + "_OLD;");
            database.execSQL("DROP TABLE " + Resource.TABLE_NAME + "_OLD");

        }
    };

    public static Migration[] getAllmigrations() {
        Field[] fields = Migrations.class.getDeclaredFields();
        List<Migration> migrations = new ArrayList<>();
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == Migration.class) {
                try {
                    migrations.add((Migration) f.get(null));
                } catch (IllegalAccessException e) {
                    Timber.w(e);
                }
            }
        }
        Migration[] result = new Migration[migrations.size()];
        return migrations.toArray(result);
    }
}
