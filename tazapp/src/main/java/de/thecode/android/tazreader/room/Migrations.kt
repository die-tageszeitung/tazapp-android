package de.thecode.android.tazreader.room

import android.provider.BaseColumns
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.thecode.android.tazreader.storageManager
import timber.log.Timber

object Migrations {

    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11
        )
    }

    private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE DOWNLOADS ADD COLUMN `unmeteredOnly` INTEGER;")
        }
    }

    private val MIGRATION_9_10: Migration = object : Migration(9, 10) {

        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Migration 9->10")

            val downloadCacheDir = storageManager.downloadCache.absolutePath

            val CREATE_SQL_DOWNLOADS = "CREATE TABLE IF NOT EXISTS `DOWNLOADS` (`key` TEXT NOT NULL, `title` TEXT NOT NULL, `file` TEXT NOT NULL, `type` TEXT NOT NULL, `downloadManagerId` INTEGER NOT NULL, `progress` INTEGER NOT NULL, `workerUuid` TEXT, `state` TEXT NOT NULL, PRIMARY KEY(`key`))"
            db.execSQL(CREATE_SQL_DOWNLOADS)

            val CREATE_SQL_PAPER = "CREATE TABLE IF NOT EXISTS `PAPER` (`fileHash` TEXT, `len` INTEGER NOT NULL, `bookId` TEXT NOT NULL, `date` TEXT, `image` TEXT, `imageHash` TEXT, `link` TEXT, `lastModified` INTEGER NOT NULL, `resource` TEXT, `demo` INTEGER NOT NULL, `title` TEXT, `validUntil` INTEGER NOT NULL, `publication` TEXT, PRIMARY KEY(`bookId`))"
            db.execSQL("ALTER TABLE PAPER RENAME TO PAPER_REN;")
            db.execSQL(CREATE_SQL_PAPER)
            db.execSQL("INSERT INTO PAPER (fileHash,len,bookId,date,image,imageHash,link,lastModified,resource,demo,title,validUntil,publication) SELECT fileHash,len,bookId,date,image,imageHash,link,lastModified,resource,demo,title,validUntil,publication FROM PAPER_REN;")

            db.execSQL("INSERT INTO DOWNLOADS (`key`,title,file,type,downloadManagerId,progress,state) SELECT bookId,'migrated','$downloadCacheDir/' || bookId || '.paper.zip','PAPER',downloadId,100,'READY' FROM PAPER_REN WHERE state = 4 OR state = 5;")
            db.execSQL("DROP TABLE PAPER_REN;")

            val CREATE_SQL_RESOURCE = "CREATE TABLE IF NOT EXISTS `RESOURCE` (`fileHash` TEXT, `len` INTEGER NOT NULL, `key` TEXT NOT NULL, `url` TEXT, PRIMARY KEY(`key`))"
            db.execSQL("ALTER TABLE RESOURCE RENAME TO RESOURCE_REN;")
            db.execSQL(CREATE_SQL_RESOURCE)
            db.execSQL("INSERT INTO RESOURCE (fileHash,len,`key`,url) SELECT fileHash,len,`key`,url FROM RESOURCE_REN;")

            db.execSQL("INSERT INTO DOWNLOADS (`key`,title,file,type,downloadManagerId,progress,state) SELECT `key`,'migrated','$downloadCacheDir/' || `key` || '.res.zip','RESOURCE',downloadId,100,'READY' FROM RESOURCE_REN WHERE downloaded = 1;")
            db.execSQL("DROP TABLE RESOURCE_REN;")
        }

    }


    private val MIGRATION_8_9: Migration = object : Migration(8, 9) {

        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Migration 8->9")
            val CREATE_SQL_PAPER = "CREATE TABLE IF NOT EXISTS `PAPER` (`bookId` TEXT NOT NULL, `date` TEXT, `image` TEXT, `imageHash` TEXT, `link` TEXT, `fileHash` TEXT, `len` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, `resource` TEXT, `demo` INTEGER NOT NULL, `state` INTEGER NOT NULL, `downloadId` INTEGER NOT NULL, `title` TEXT, `validUntil` INTEGER NOT NULL, `publication` TEXT, PRIMARY KEY(`bookId`))"
            db.execSQL("ALTER TABLE PAPER RENAME TO PAPER_REN;")
            db.execSQL("ALTER TABLE PAPER_REN ADD COLUMN `state` INTEGER NOT NULL DEFAULT 0;")
            db.execSQL("UPDATE PAPER_REN SET state=4 WHERE downloaded = 1;")
            db.execSQL("UPDATE PAPER_REN SET state=5 WHERE downloaded = 1 AND hasUpdate = 1;")
            db.execSQL(CREATE_SQL_PAPER)
            db.execSQL("INSERT INTO PAPER (bookId,date,image,imageHash,link,fileHash,len,lastModified,resource,demo,state,downloadId,title,validUntil,publication) SELECT bookId,date,image,imageHash,link,fileHash,len,lastModified,resource,demo,state,downloadId,title,validUntil,publication FROM PAPER_REN;")
            db.execSQL("DROP TABLE PAPER_REN;")
        }

    }

    private val MIGRATION_7_8: Migration = object : Migration(7, 8) {

        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Migration 7->8")
            val CREATE_SQL_PAPER = "CREATE TABLE IF NOT EXISTS `PAPER` (`bookId` TEXT NOT NULL, `date` TEXT, `image` TEXT, `imageHash` TEXT, `link` TEXT, `fileHash` TEXT, `len` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, `resource` TEXT, `demo` INTEGER NOT NULL, `hasUpdate` INTEGER NOT NULL, `downloadId` INTEGER NOT NULL, `downloaded` INTEGER NOT NULL, `kiosk` INTEGER NOT NULL, `imported` INTEGER NOT NULL, `title` TEXT, `validUntil` INTEGER NOT NULL, `publication` TEXT, PRIMARY KEY(`bookId`))"
            val CREATE_SQL_RESOURCE = "CREATE TABLE IF NOT EXISTS `RESOURCE` (`key` TEXT NOT NULL, `downloadId` INTEGER NOT NULL, `downloaded` INTEGER NOT NULL, `url` TEXT, `fileHash` TEXT, `len` INTEGER NOT NULL, PRIMARY KEY(`key`))"
            val CREATE_SQL_STORE = "CREATE TABLE IF NOT EXISTS `STORE` (`path` TEXT NOT NULL, `value` TEXT, PRIMARY KEY(`path`))"
            val CREATE_SQL_PUBLICATION = "CREATE TABLE IF NOT EXISTS `PUBLICATION` (`issueName` TEXT NOT NULL, `typeName` TEXT, `name` TEXT, `url` TEXT, `image` TEXT, `created` INTEGER NOT NULL, `validUntil` INTEGER NOT NULL, `appAndroidVersion` TEXT, PRIMARY KEY(`issueName`))"

            //TODO Update PAPER / muss zuerst
            db.execSQL("ALTER TABLE PAPER RENAME TO PAPER_REN;")
            db.execSQL("DELETE FROM PAPER_REN WHERE bookId IS NULL OR len IS NULL OR lastModified IS NULL OR isDemo OR hasUpdate IS NULL OR downloadId IS NULL OR isDownloaded IS NULL OR kiosk IS NULL OR imported IS NULL OR validUntil IS NULL;")
            //Lösche alle doppelten
            db.execSQL("DELETE FROM PAPER_REN WHERE bookId IN (SELECT bookId FROM PAPER_REN GROUP BY bookId HAVING COUNT(bookId) > 1) AND isDownloaded = 0;")
            db.execSQL("DELETE FROM PAPER_REN WHERE bookId IN (SELECT bookId FROM PAPER_REN GROUP BY bookId HAVING COUNT(bookId) > 1);")

            db.execSQL(CREATE_SQL_PAPER)
            db.execSQL(
                    "INSERT INTO PAPER (bookId,date,image,imageHash,link,fileHash,len,lastModified,resource,demo,hasUpdate,downloadId,downloaded,kiosk,imported,title,validUntil,publication) SELECT bookId,date,image,imageHash,link,fileHash,len,lastModified,resource,isDemo,hasUpdate,downloadId,isDownloaded,kiosk,imported,title,validUntil,publicationId FROM PAPER_REN;")
            db.execSQL("DROP TABLE PAPER_REN;")

            val cursor = db.query("SELECT * FROM PUBLICATION")
            try {
                while (cursor.moveToNext()) {
                    val publicationId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
                    val issueName = cursor.getString(cursor.getColumnIndex("issueName"))
                    db.execSQL("UPDATE PAPER SET publication = '$issueName' WHERE publication LIKE '$publicationId';")
                }
            } finally {
                cursor.close()
            }

            //TODO Update Publication
            db.execSQL("ALTER TABLE PUBLICATION RENAME TO PUBLICATION_REN;")
            db.execSQL("DELETE FROM PUBLICATION_REN WHERE issueName IS NULL OR created IS NULL OR validUntil IS NULL;")
            //Doppelte löschen
            db.execSQL("DELETE FROM PUBLICATION_REN WHERE issueName IN (SELECT issueName FROM PUBLICATION_REN GROUP BY issueName HAVING COUNT(issueName) > 1);")
            db.execSQL(CREATE_SQL_PUBLICATION)
            db.execSQL(
                    "INSERT INTO PUBLICATION (issueName,created,image,name,typeName,url,validUntil) SELECT issueName,created,image,name,typeName,url,validUntil FROM PUBLICATION_REN")
            db.execSQL("DROP TABLE PUBLICATION_REN;")


            Timber.d("Umbenennen der alten Store Tabelle")
            db.execSQL("ALTER TABLE STORE RENAME TO STORE_REN;")
            db.execSQL("DELETE FROM STORE_REN WHERE `key` IS NULL;")

            val cursor2 = db.query("SELECT * FROM STORE_REN WHERE key LIKE '%/currentPosition' AND value LIKE '%?%'")
            try {
                while (cursor2.moveToNext()) {
                    val key = cursor2.getString(cursor2.getColumnIndex("key"))
                    var value = cursor2.getString(cursor2.getColumnIndex("value"))
                    value = value.substring(0, value.indexOf('?'))
                    db.execSQL("UPDATE STORE_REN SET value = '$value' WHERE key LIKE '$key';")
                }
            } finally {
                cursor2.close()
            }

            Timber.d("Anlegen der neuen Store Tabelle")
            db.execSQL(CREATE_SQL_STORE)
            db.execSQL("INSERT INTO STORE (path, value) SELECT `key`, value FROM STORE_REN;")
            db.execSQL("DROP TABLE STORE_REN;")

            db.execSQL("ALTER TABLE RESOURCE RENAME TO RESOURCE_REN;")
            db.execSQL("DELETE FROM RESOURCE_REN WHERE `key` IS NULL OR downloadID IS NULL OR downloaded IS NULL OR len IS NULL;")
            db.execSQL(CREATE_SQL_RESOURCE)
            db.execSQL(
                    "INSERT INTO RESOURCE (`key`,downloadId,downloaded,len,fileHash,url) SELECT `key`,downloadID,downloaded,len,fileHash,url FROM RESOURCE_REN")
            db.execSQL("DROP TABLE RESOURCE_REN;")
        }
    }

    private val MIGRATION_6_7: Migration = object : Migration(6, 7) {

        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Migration 6->7")
            Timber.d("Umbenennen der alten Store Tabelle")
            db.execSQL("ALTER TABLE STORE RENAME TO STORE_REN;")
            Timber.d("Anlegen der neuen Store Tabelle")
            db.execSQL("CREATE TABLE IF NOT EXISTS STORE (`key` TEXT NOT NULL PRIMARY KEY, value TEXT);")
            db.execSQL("INSERT INTO STORE (`key`, value) SELECT `key`, value FROM STORE_REN;")
            db.execSQL("DROP TABLE STORE_REN;")
        }
    }

    private val MIGRATION_5_6: Migration = object : Migration(5, 6) {

        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE RESOURCE ADD COLUMN fileHash TEXT;")
            db.execSQL("ALTER TABLE RESOURCE ADD COLUMN len INTEGER;")
            db.execSQL("ALTER TABLE RESOURCE ADD COLUMN url TEXT;")
        }
    }

    private val MIGRATION_4_5: Migration = object : Migration(4, 5) {

        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE PAPER ADD COLUMN validUntil INTEGER;")
        }
    }


    internal val MIGRATION_3_4: Migration = object : Migration(3, 4) {

        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE PAPER ADD COLUMN resource TEXT;")
            db.execSQL("CREATE TABLE RESOURCE (`key` TEXT PRIMARY KEY,downloadID INTEGER,downloaded INTEGER)")
        }
    }


    private val MIGRATION_2_3: Migration = object : Migration(2, 3) {

        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE PAPER ADD COLUMN downloadId INTEGER;")
        }
    }


    private val MIGRATION_1_2: Migration = object : Migration(1, 2) {

        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.d("Umbenennen der alten Paper Tabelle")
            db.execSQL("ALTER TABLE PAPER RENAME TO PAPER_REN;")

            Timber.d("Anlegen der neuen Paper Tabelle")
            db.execSQL("CREATE TABLE PAPER (_id INTEGER PRIMARY KEY,date TEXT,image TEXT,imageHash TEXT,link TEXT,fileHash TEXT,len INTEGER,lastModified INTEGER,bookId TEXT,isDemo INTEGER,filename TEXT,hasUpdate INTEGER,isDownloading INTEGER,isDownloaded INTEGER,kiosk INTEGER,imported INTEGER,title TEXT,publicationId INTEGER);")

            db.execSQL("UPDATE PAPER_REN SET DEMO=0 WHERE DEMO IS NULL;")
            db.execSQL("UPDATE PAPER_REN SET KIOSK=0 WHERE KIOSK IS NULL;")
            db.execSQL("UPDATE PAPER_REN SET IMPORTED=0 WHERE IMPORTED IS NULL;")
            db.execSQL("UPDATE PAPER_REN SET DOWNLOADPROGRESS=0 WHERE DOWNLOADPROGRESS IS NULL;")
            db.execSQL("UPDATE PAPER_REN SET DOWNLOADPROGRESS=100 WHERE ISDOWNLOADED=1;")

            db.execSQL("DELETE FROM PAPER_REN WHERE isDownloaded != 1 AND isDownloading != 1 AND imported != 1 AND kiosk != 1;")

            Timber.d("Kopieren der Werte")
            db.execSQL(
                    "INSERT INTO PAPER (_id,bookId,date,link,filename,hasUpdate,isDownloading,downloadProgress,isDownloaded,kiosk,imported,len,image,isDemo,title) SELECT _id,bookId,date,link,filename,hasUpdate,isDownloading,downloadProgress,isDownloaded,kiosk,imported,SIZE,IMAGELINK,DEMO,title FROM PAPER_REN;")

            Timber.d("Löschen der alten PaperTabelle")
            db.execSQL("DROP TABLE PAPER_REN;")


            Timber.d("Umbenennen der alten Store Tabelle")
            db.execSQL("ALTER TABLE STORE RENAME TO STORE_REN;")

            Timber.d("Anlegen der neuen Store Tabelle")
            db.execSQL("CREATE TABLE RESOURCE (`key` TEXT PRIMARY KEY,downloadID INTEGER,downloaded INTEGER)")


            Timber.d("Löschen der alten Store Tabelle")
            db.execSQL("DROP TABLE STORE_REN;")


            Timber.d("Anlegen der Publications Tabelle")
            db.execSQL("CREATE TABLE PUBLICATION (_id INTEGER PRIMARY KEY,created INTEGER,image TEXT,issueName TEXT,name TEXT,typeName TEXT,url TEXT,validUntil INTEGER);")
        }
    }
}
