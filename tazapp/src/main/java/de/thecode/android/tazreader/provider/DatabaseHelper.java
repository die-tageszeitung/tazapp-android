package de.thecode.android.tazreader.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.Store;

import timber.log.Timber;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "db";
    private static final int    DB_VERSION    = 8;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PAPER_V8);
        db.execSQL(CREATE_STORE_V7);
        db.execSQL(CREATE_PUBLICATION_V8);
        db.execSQL(CREATE_RESOURCE_V6);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.d("DB Version %s", oldVersion, newVersion);
        switch (oldVersion) {
            case 1:
                upgradeFrom1To2(db);
                if (newVersion == 2) break;
            case 2:
                upgradeFrom2To3(db);
                if (newVersion == 3) break;
            case 3:
                upgradeFrom3To4(db);
                if (newVersion == 4) break;
            case 4:
                upgradeFrom4To5(db);
                if (newVersion == 5) break;
            case 5:
                upgradeFrom5To6(db);
                if (newVersion == 6) break;
            case 6:
                upgradeFrom6To7(db);
                if (newVersion == 7) break;
            case 7:
                upgradeFrom7To8(db);
                if (newVersion == 8) break;

            default:
                break;
        }
    }

    private void upgradeFrom7To8(SQLiteDatabase db) {
        //TODO Update PAPER / muss zuerst
        db.execSQL("ALTER TABLE PAPER RENAME TO PAPER_REN;");
        db.execSQL(CREATE_PAPER_V8);
        db.execSQL(
                "INSERT INTO PAPER (_id,date,image,imageHash,link,fileHash,len,lastModified,bookId,resource,isDemo,hasUpdate,downloadId,isDownloaded,kiosk,imported,title,validUntil,publication) SELECT _id,date,image,imageHash,link,fileHash,len,lastModified,bookId,resource,isDemo,hasUpdate,downloadId,isDownloaded,kiosk,imported,title,validUntil,publicationId FROM PAPER_REN;");
        db.execSQL("DROP TABLE PAPER_REN;");
        Cursor cursor = db.query("PUBLICATION", null, null, null, null, null, "_id ASC");
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
        db.execSQL(CREATE_PUBLICATION_V8);
        db.execSQL(
                "INSERT INTO PUBLICATION (issueName,created,image,name,typeName,url,validUntil) SELECT issueName,created,image,name,typeName,url,validUntil FROM PUBLICATION_REN");
        db.execSQL("DROP TABLE PUBLICATION_REN;");
    }

    private void upgradeFrom6To7(SQLiteDatabase db) {
        Timber.d("Umbenennen der alten Store Tabelle");
        db.execSQL("ALTER TABLE STORE RENAME TO STORE_REN;");
        Timber.d("Anlegen der neuen Store Tabelle");
        db.execSQL(CREATE_STORE_V7);
        db.execSQL("INSERT INTO STORE (key, value) SELECT key, value FROM STORE_REN;");
        db.execSQL("DROP TABLE STORE_REN;");
    }

    private void upgradeFrom5To6(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE RESOURCE ADD COLUMN fileHash TEXT;");
        db.execSQL("ALTER TABLE RESOURCE ADD COLUMN len INTEGER;");
        db.execSQL("ALTER TABLE RESOURCE ADD COLUMN url TEXT;");
//        db.execSQL("ALTER TABLE PAPER ADD COLUMN " + Paper.Columns.RESOURCELEN + " INTEGER;");
    }

    private void upgradeFrom4To5(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE PAPER ADD COLUMN validUntil INTEGER;");
    }

    private void upgradeFrom3To4(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE PAPER ADD COLUMN resource TEXT;");
//        db.execSQL("ALTER TABLE PAPER ADD COLUMN " + Paper.Columns.RESOURCEFILEHASH + " TEXT;");
//        db.execSQL("ALTER TABLE PAPER ADD COLUMN " + Paper.Columns.RESOURCEURL + " TEXT;");
        db.execSQL(CREATE_RESOURCE_V4);
    }

    private void upgradeFrom2To3(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE PAPER ADD COLUMN downloadId INTEGER;");
    }


    private void upgradeFrom1To2(SQLiteDatabase db) {
        Timber.d("Umbenennen der alten Paper Tabelle");
        db.execSQL("ALTER TABLE PAPER RENAME TO PAPER_REN;");

        Timber.d("Anlegen der neuen Paper Tabelle");
        db.execSQL(CREATE_PAPER_V2);

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
        db.execSQL(CREATE_STORE_V2);


        Timber.d("Kopieren der Werte");
        Cursor cursor = db.query("STORE_REN", null, null, null, null, null, "_id ASC");
        try {
            while (cursor.moveToNext()) {
                String oldKey = cursor.getString(cursor.getColumnIndex("KEY"));
                String oldValue = cursor.getString(cursor.getColumnIndex("VALUE"));

                String newKey = oldKey.substring(oldKey.indexOf("/"));
                Timber.d(newKey);

                Cursor cursorNew = db.query("STORE", null, "key LIKE '" + newKey + "'", null, null, null, null);
                try {
                    if (cursorNew.moveToNext()) {
                        Store newStore = new Store(cursorNew);
                        newStore.setValue(oldValue);
                        db.update("STORE", newStore.getContentValues(), "key LIKE '" + newKey + "'", null);
                    } else {
                        Store newStore = new Store(newKey, oldValue);
                        db.insert("STORE", null, newStore.getContentValues());
                    }
                } finally {
                    cursorNew.close();
                }
            }
        } finally {
            cursor.close();
        }


        Timber.d("Löschen der alten Store Tabelle");
        db.execSQL("DROP TABLE STORE_REN;");


        Timber.d("Anlegen der Publications Tabelle");
        db.execSQL(CREATE_PUBLICATION_V2);
    }

    //---Version 8---
    private static final String CREATE_PUBLICATION_V8 = "CREATE TABLE IF NOT EXISTS PUBLICATION (issueName TEXT PRIMARY KEY,created INTEGER,image TEXT,name TEXT,typeName TEXT,url TEXT,validUntil INTEGER, appAndroidVersion INTEGER);";
    private static final String CREATE_PAPER_V8       = "CREATE TABLE PAPER (_id INTEGER PRIMARY KEY,date TEXT,image TEXT,imageHash TEXT,link TEXT,fileHash TEXT,len INTEGER,lastModified INTEGER,bookId TEXT,resource TEXT,isDemo INTEGER,hasUpdate INTEGER,downloadId INTEGER,isDownloaded INTEGER,kiosk INTEGER,imported INTEGER,title TEXT,validUntil INTEGER,publication TEXT);";

    //---Version 7---
    private static final String CREATE_STORE_V7 = "CREATE TABLE IF NOT EXISTS STORE (key TEXT NOT NULL PRIMARY KEY, value TEXT);";


    //---Version 6---
    private static final String CREATE_PAPER_V6 = "CREATE TABLE PAPER (_id INTEGER PRIMARY KEY,date TEXT,image TEXT,imageHash TEXT,link TEXT,fileHash TEXT,len INTEGER,lastModified INTEGER,bookId TEXT,resource TEXT,isDemo INTEGER,hasUpdate INTEGER,downloadId INTEGER,isDownloaded INTEGER,kiosk INTEGER,imported INTEGER,title TEXT,validUntil INTEGER,publicationId INTEGER);";

    private static final String CREATE_STORE_V6 = "CREATE TABLE STORE (_id INTEGER PRIMARY KEY,key TEXT,value TEXT" + ");";

    private static final String CREATE_PUBLICATION_V6 = "CREATE TABLE PUBLICATION (_id INTEGER PRIMARY KEY,created INTEGER,image TEXT,issueName TEXT,name TEXT,typeName TEXT,url TEXT,validUntil INTEGER);";

    private static final String CREATE_RESOURCE_V6 = "CREATE TABLE RESOURCE (key TEXT PRIMARY KEY,downloadID INTEGER,downloaded INTEGER,len INTEGER,fileHash TEXT,url TEXT)";

    //---Version 5---
    private static final String CREATE_PAPER_V5 = "CREATE TABLE PAPER (_id INTEGER PRIMARY KEY,date TEXT,image TEXT,imageHash TEXT,link TEXT,fileHash TEXT,len INTEGER,lastModified INTEGER,bookId TEXT,resource TEXT,isDemo INTEGER,hasUpdate INTEGER,downloadId INTEGER,isDownloaded INTEGER,kiosk INTEGER,imported INTEGER,title TEXT,validUntil INTEGER,publicationId INTEGER);";

    private static final String CREATE_STORE_V5 = "CREATE TABLE STORE (_id INTEGER PRIMARY KEY,key TEXT,value TEXT" + ");";

    private static final String CREATE_PUBLICATION_V5 = "CREATE TABLE PUBLICATION (_id INTEGER PRIMARY KEY,created INTEGER,image TEXT,issueName TEXT,name TEXT,typeName TEXT,url TEXT,validUntil INTEGER" + ");";

    private static final String CREATE_RESOURCE_V5 = "CREATE TABLE RESOURCE (key TEXT PRIMARY KEY,downloadID INTEGER,downloaded INTEGER" + ")";


    //---Version 4---
    private static final String CREATE_PAPER_V4 = "CREATE TABLE PAPER (_id INTEGER PRIMARY KEY,date TEXT,image TEXT,imageHash TEXT,link TEXT,fileHash TEXT,len INTEGER,lastModified INTEGER,bookId TEXT,resource TEXT," +
//            Paper.Columns.RESOURCEFILEHASH + " TEXT," +
//            Paper.Columns.RESOURCEURL + " TEXT," +
            Paper.Columns.ISDEMO + " INTEGER,hasUpdate INTEGER,downloadId INTEGER,isDownloaded INTEGER,kiosk INTEGER,imported INTEGER,title TEXT,publicationId INTEGER" + ");";

    private static final String CREATE_STORE_V4 = "CREATE TABLE STORE (_id INTEGER PRIMARY KEY,key TEXT,value TEXT" + ");";

    private static final String CREATE_PUBLICATION_V4 = "CREATE TABLE PUBLICATION (_id INTEGER PRIMARY KEY,created INTEGER,image TEXT,issueName TEXT,name TEXT,typeName TEXT,url TEXT,validUntil INTEGER" + ");";

    private static final String CREATE_RESOURCE_V4 = "CREATE TABLE RESOURCE (key TEXT PRIMARY KEY,downloadID INTEGER,downloaded INTEGER" + ")";


    //---Version 3---
    private static final String CREATE_PAPER_V3 = "CREATE TABLE PAPER (_id INTEGER PRIMARY KEY,date TEXT,image TEXT,imageHash TEXT,link TEXT,fileHash TEXT,len INTEGER,lastModified INTEGER,bookId TEXT,isDemo INTEGER," + "filename" + " TEXT," +
            //                    Paper.Columns.TEMPFILEPATH + " TEXT," +
            //                    Paper.Columns.TEMPFILENAME + " TEXT," +
            Paper.Columns.HASUPDATE + " INTEGER,downloadId INTEGER," +
            //                    Paper.Columns.DOWNLOADPROGRESS + " INTEGER," +
            Paper.Columns.ISDOWNLOADED + " INTEGER,kiosk INTEGER,imported INTEGER,title TEXT,publicationId INTEGER" + ");";

    private static final String CREATE_STORE_V3 = "CREATE TABLE STORE (_id INTEGER PRIMARY KEY,key TEXT,value TEXT" + ");";

    private static final String CREATE_PUBLICATION_V3 = "CREATE TABLE PUBLICATION (_id INTEGER PRIMARY KEY,created INTEGER,image TEXT,issueName TEXT,name TEXT,typeName TEXT,url TEXT,validUntil INTEGER" + ");";


    //---Version 2---
    private static final String CREATE_PAPER_V2 = "CREATE TABLE PAPER (_id INTEGER PRIMARY KEY,date TEXT,image TEXT,imageHash TEXT,link TEXT,fileHash TEXT,len INTEGER,lastModified INTEGER,bookId TEXT,isDemo INTEGER,filename TEXT,hasUpdate INTEGER,isDownloading INTEGER,isDownloaded INTEGER,kiosk INTEGER,imported INTEGER,title TEXT,publicationId INTEGER);";

    private static final String CREATE_STORE_V2 = "CREATE TABLE STORE (_id INTEGER PRIMARY KEY,key TEXT,value TEXT);";

    private static final String CREATE_PUBLICATION_V2 = "CREATE TABLE PUBLICATION (_id INTEGER PRIMARY KEY,created INTEGER,image TEXT,issueName TEXT,name TEXT,typeName TEXT,url TEXT,validUntil INTEGER);";

}
