package de.thecode.android.tazreader.room;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQueryBuilder;
import android.arch.persistence.room.migration.Migration;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import de.thecode.android.tazreader.data.Store;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class Migrations {

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            Timber.i("Migration 7->8");
            String CREATE_SQL_PAPER = "CREATE TABLE IF NOT EXISTS `PAPER` (`bookId` TEXT NOT NULL, `date` TEXT, `image` TEXT, `imageHash` TEXT, `link` TEXT, `fileHash` TEXT, `len` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, `resource` TEXT, `demo` INTEGER NOT NULL, `hasUpdate` INTEGER NOT NULL, `downloadId` INTEGER NOT NULL, `downloaded` INTEGER NOT NULL, `kiosk` INTEGER NOT NULL, `imported` INTEGER NOT NULL, `title` TEXT, `validUntil` INTEGER NOT NULL, `publication` TEXT, PRIMARY KEY(`bookId`))";
            String CREATE_SQL_RESOURCE = "CREATE TABLE IF NOT EXISTS `RESOURCE` (`key` TEXT NOT NULL, `downloadId` INTEGER NOT NULL, `downloaded` INTEGER NOT NULL, `url` TEXT, `fileHash` TEXT, `len` INTEGER NOT NULL, PRIMARY KEY(`key`))";
            String CREATE_SQL_STORE = "CREATE TABLE IF NOT EXISTS `STORE` (`path` TEXT NOT NULL, `value` TEXT, PRIMARY KEY(`path`))";
            String CREATE_SQL_PUBLICATION = "CREATE TABLE IF NOT EXISTS `PUBLICATION` (`issueName` TEXT NOT NULL, `typeName` TEXT, `name` TEXT, `url` TEXT, `image` TEXT, `created` INTEGER NOT NULL, `validUntil` INTEGER NOT NULL, `appAndroidVersion` TEXT, PRIMARY KEY(`issueName`))";

            //TODO Update PAPER / muss zuerst
            db.execSQL("ALTER TABLE PAPER RENAME TO PAPER_REN;");
            //Lösche alle doppelten
            db.execSQL("DELETE FROM PAPER_REN WHERE bookId IN (SELECT bookId FROM PAPER_REN GROUP BY bookId HAVING COUNT(bookId) > 1) AND isDownloaded = 0;");
            db.execSQL("DELETE FROM PAPER_REN WHERE bookId IN (SELECT bookId FROM PAPER_REN GROUP BY bookId HAVING COUNT(bookId) > 1);");

            db.execSQL(CREATE_SQL_PAPER);
            db.execSQL(
                    "INSERT INTO PAPER (bookId,date,image,imageHash,link,fileHash,len,lastModified,resource,demo,hasUpdate,downloadId,downloaded,kiosk,imported,title,validUntil,publication) SELECT bookId,date,image,imageHash,link,fileHash,len,lastModified,resource,isDemo,hasUpdate,downloadId,isDownloaded,kiosk,imported,title,validUntil,publicationId FROM PAPER_REN;");
            db.execSQL("DROP TABLE PAPER_REN;");

            Cursor cursor = db.query("SELECT * FROM PUBLICATION");
            try {
                while (cursor.moveToNext()) {
                    long publicationId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                    String issueName = cursor.getString(cursor.getColumnIndex("issueName"));
                    db.execSQL("UPDATE PAPER SET publication = '" + issueName + "' WHERE publication LIKE '" + publicationId + "';");
                }
            } finally {
                cursor.close();
            }

            //TODO Update Publication
            db.execSQL("ALTER TABLE PUBLICATION RENAME TO PUBLICATION_REN;");
            //Doppelte löschen
            db.execSQL("DELETE FROM PUBLICATION_REN WHERE issueName IN (SELECT issueName FROM PUBLICATION_REN GROUP BY issueName HAVING COUNT(issueName) > 1);");
            db.execSQL(CREATE_SQL_PUBLICATION);
            db.execSQL(
                    "INSERT INTO PUBLICATION (issueName,created,image,name,typeName,url,validUntil) SELECT issueName,created,image,name,typeName,url,validUntil FROM PUBLICATION_REN");
            db.execSQL("DROP TABLE PUBLICATION_REN;");


            Timber.d("Umbenennen der alten Store Tabelle");
            db.execSQL("ALTER TABLE STORE RENAME TO STORE_REN;");

            Cursor cursor2 = db.query("SELECT * FROM STORE_REN WHERE key LIKE '%/currentPosition' AND value LIKE '%?%'");
            try {
                while (cursor2.moveToNext()) {
                    String key = cursor2.getString(cursor2.getColumnIndex("key"));
                    String value = cursor2.getString(cursor2.getColumnIndex("value"));
                    value = value.substring(0,value.indexOf('?'));
                    db.execSQL("UPDATE STORE_REN SET value = '"+value+"' WHERE key LIKE '"+key+"';");
                }
            } finally {
                cursor2.close();
            }

            Timber.d("Anlegen der neuen Store Tabelle");
            db.execSQL(CREATE_SQL_STORE);
            db.execSQL("INSERT INTO STORE (path, value) SELECT key, value FROM STORE_REN;");
            db.execSQL("DROP TABLE STORE_REN;");

            db.execSQL("ALTER TABLE RESOURCE RENAME TO RESOURCE_REN;");
            db.execSQL(CREATE_SQL_RESOURCE);
            db.execSQL(
                    "INSERT INTO RESOURCE (key,downloadId,downloaded,len,fileHash,url) SELECT key,downloadID,downloaded,len,fileHash,url FROM RESOURCE_REN");
            db.execSQL("DROP TABLE RESOURCE_REN;");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            Timber.i("Migration 6->7");
            Timber.d("Umbenennen der alten Store Tabelle");
            db.execSQL("ALTER TABLE STORE RENAME TO STORE_REN;");
            Timber.d("Anlegen der neuen Store Tabelle");
            db.execSQL("CREATE TABLE IF NOT EXISTS STORE (key TEXT NOT NULL PRIMARY KEY, value TEXT);");
            db.execSQL("INSERT INTO STORE (key, value) SELECT key, value FROM STORE_REN;");
            db.execSQL("DROP TABLE STORE_REN;");
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE RESOURCE ADD COLUMN fileHash TEXT;");
            db.execSQL("ALTER TABLE RESOURCE ADD COLUMN len INTEGER;");
            db.execSQL("ALTER TABLE RESOURCE ADD COLUMN url TEXT;");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE PAPER ADD COLUMN validUntil INTEGER;");
        }
    };


    static final Migration MIGRATION_3_4 = new Migration(3, 4) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE PAPER ADD COLUMN resource TEXT;");
            db.execSQL("CREATE TABLE RESOURCE (key TEXT PRIMARY KEY,downloadID INTEGER,downloaded INTEGER)");
        }
    };


    static final Migration MIGRATION_2_3 = new Migration(2, 3) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE PAPER ADD COLUMN downloadId INTEGER;");
        }
    };


    static final Migration MIGRATION_1_2 = new Migration(1, 2) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            Timber.d("Umbenennen der alten Paper Tabelle");
            db.execSQL("ALTER TABLE PAPER RENAME TO PAPER_REN;");

            Timber.d("Anlegen der neuen Paper Tabelle");
            db.execSQL("CREATE TABLE PAPER (_id INTEGER PRIMARY KEY,date TEXT,image TEXT,imageHash TEXT,link TEXT,fileHash TEXT,len INTEGER,lastModified INTEGER,bookId TEXT,isDemo INTEGER,filename TEXT,hasUpdate INTEGER,isDownloading INTEGER,isDownloaded INTEGER,kiosk INTEGER,imported INTEGER,title TEXT,publicationId INTEGER);");

            db.execSQL("UPDATE PAPER_REN SET DEMO=0 WHERE DEMO IS NULL;");
            db.execSQL("UPDATE PAPER_REN SET KIOSK=0 WHERE KIOSK IS NULL;");
            db.execSQL("UPDATE PAPER_REN SET IMPORTED=0 WHERE IMPORTED IS NULL;");
            db.execSQL("UPDATE PAPER_REN SET DOWNLOADPROGRESS=0 WHERE DOWNLOADPROGRESS IS NULL;");
            db.execSQL("UPDATE PAPER_REN SET DOWNLOADPROGRESS=100 WHERE ISDOWNLOADED=1;");

            db.execSQL("DELETE FROM PAPER_REN WHERE isDownloaded != 1 AND isDownloading != 1 AND imported != 1 AND kiosk != 1;");

            Timber.d("Kopieren der Werte");
            db.execSQL(
                    "INSERT INTO PAPER (_id,bookId,date,link,filename,hasUpdate,isDownloading,downloadProgress,isDownloaded,kiosk,imported,len,image,isDemo,title) SELECT _id,bookId,date,link,filename,hasUpdate,isDownloading,downloadProgress,isDownloaded,kiosk,imported,SIZE,IMAGELINK,DEMO,title FROM PAPER_REN;");

            Timber.d("Löschen der alten PaperTabelle");
            db.execSQL("DROP TABLE PAPER_REN;");


            Timber.d("Umbenennen der alten Store Tabelle");
            db.execSQL("ALTER TABLE STORE RENAME TO STORE_REN;");

            Timber.d("Anlegen der neuen Store Tabelle");
            db.execSQL("CREATE TABLE RESOURCE (key TEXT PRIMARY KEY,downloadID INTEGER,downloaded INTEGER)");


            Timber.d("Löschen der alten Store Tabelle");
            db.execSQL("DROP TABLE STORE_REN;");


            Timber.d("Anlegen der Publications Tabelle");
            db.execSQL("CREATE TABLE PUBLICATION (_id INTEGER PRIMARY KEY,created INTEGER,image TEXT,issueName TEXT,name TEXT,typeName TEXT,url TEXT,validUntil INTEGER);");
        }
    };


//    static final Migration MIGRATION__ = new Migration(, ) {
//
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase db) {
//        }
//    };


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
