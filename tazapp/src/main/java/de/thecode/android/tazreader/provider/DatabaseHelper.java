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
    private static final int DB_VERSION = 6;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PAPER_V6);
        db.execSQL(CREATE_STORE_V6);
        //db.execSQL(CREATE_PUBLICATION_V6);
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
            default:
                break;
        }
    }

    private void upgradeFrom5To6(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Resource.TABLE_NAME + " ADD COLUMN " + Resource.Columns.FILEHASH + " TEXT;");
        db.execSQL("ALTER TABLE " + Resource.TABLE_NAME + " ADD COLUMN " + Resource.Columns.LEN + " INTEGER;");
        db.execSQL("ALTER TABLE " + Resource.TABLE_NAME + " ADD COLUMN " + Resource.Columns.URL + " TEXT;");
//        db.execSQL("ALTER TABLE " + Paper.TABLE_NAME + " ADD COLUMN " + Paper.Columns.RESOURCELEN + " INTEGER;");
    }

    private void upgradeFrom4To5(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Paper.TABLE_NAME + " ADD COLUMN " + Paper.Columns.VALIDUNTIL + " INTEGER;");
    }

    private void upgradeFrom3To4(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Paper.TABLE_NAME + " ADD COLUMN " + Paper.Columns.RESOURCE + " TEXT;");
//        db.execSQL("ALTER TABLE " + Paper.TABLE_NAME + " ADD COLUMN " + Paper.Columns.RESOURCEFILEHASH + " TEXT;");
//        db.execSQL("ALTER TABLE " + Paper.TABLE_NAME + " ADD COLUMN " + Paper.Columns.RESOURCEURL + " TEXT;");
        db.execSQL(CREATE_RESOURCE_V4);
    }

    private void upgradeFrom2To3(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Paper.TABLE_NAME + " ADD COLUMN " + Paper.Columns.DOWNLOADID + " INTEGER;");
    }


    private void upgradeFrom1To2(SQLiteDatabase db) {
        Timber.d("Umbenennen der alten Paper Tabelle");
        db.execSQL("ALTER TABLE " + Paper.TABLE_NAME + " RENAME TO " + Paper.TABLE_NAME + "_REN;");

        Timber.d("Anlegen der neuen Paper Tabelle");
        db.execSQL(CREATE_PAPER_V2);

        db.execSQL("UPDATE " + Paper.TABLE_NAME + "_REN SET DEMO=0 WHERE DEMO IS NULL;");
        db.execSQL("UPDATE " + Paper.TABLE_NAME + "_REN SET KIOSK=0 WHERE KIOSK IS NULL;");
        db.execSQL("UPDATE " + Paper.TABLE_NAME + "_REN SET IMPORTED=0 WHERE IMPORTED IS NULL;");
        db.execSQL("UPDATE " + Paper.TABLE_NAME + "_REN SET DOWNLOADPROGRESS=0 WHERE DOWNLOADPROGRESS IS NULL;");
        db.execSQL("UPDATE " + Paper.TABLE_NAME + "_REN SET DOWNLOADPROGRESS=100 WHERE ISDOWNLOADED=1;");

        db.execSQL("DELETE FROM " + Paper.TABLE_NAME + "_REN WHERE " + Paper.Columns.ISDOWNLOADED + " != 1 AND " + "isDownloading" + " != 1 AND " + Paper.Columns.IMPORTED + " != 1 AND " + Paper.Columns.KIOSK + " != 1;");

        Timber.d("Kopieren der Werte");
        db.execSQL("INSERT INTO " + Paper.TABLE_NAME + " (" +
                           BaseColumns._ID + "," +
                Paper.Columns.BOOKID + "," +
                Paper.Columns.DATE + "," +
                Paper.Columns.LINK + "," +
                "filename" + "," +
                //                Paper.Columns.TEMPFILEPATH + "," +
                //                Paper.Columns.TEMPFILENAME + "," +
                Paper.Columns.HASUPDATE + "," +
                "isDownloading" + "," +
                "downloadProgress" + "," +
                Paper.Columns.ISDOWNLOADED + "," +
                Paper.Columns.KIOSK + "," +
                Paper.Columns.IMPORTED + "," +
                Paper.Columns.LEN + "," +
                Paper.Columns.IMAGE + "," +
                Paper.Columns.ISDEMO + "," +
                Paper.Columns.TITLE + "" +
                ") " +
                "SELECT " +
                           BaseColumns._ID + "," +
                Paper.Columns.BOOKID + "," +
                Paper.Columns.DATE + "," +
                Paper.Columns.LINK + "," +
                "filename" + "," +
                //                Paper.Columns.TEMPFILEPATH + "," +
                //                Paper.Columns.TEMPFILENAME + "," +
                Paper.Columns.HASUPDATE + "," +
                "isDownloading" + "," +
                "downloadProgress" + "," +
                Paper.Columns.ISDOWNLOADED + "," +
                Paper.Columns.KIOSK + "," +
                Paper.Columns.IMPORTED + "," +
                "SIZE," +
                "IMAGELINK," +
                "DEMO," +
                Paper.Columns.TITLE +
                " FROM " + Paper.TABLE_NAME + "_REN;");

        Timber.d("Löschen der alten PaperTabelle");
        db.execSQL("DROP TABLE " + Paper.TABLE_NAME + "_REN;");


        Timber.d("Umbenennen der alten Store Tabelle");
        db.execSQL("ALTER TABLE " + Store.TABLE_NAME + " RENAME TO " + Store.TABLE_NAME + "_REN;");

        Timber.d("Anlegen der neuen Store Tabelle");
        db.execSQL(CREATE_STORE_V2);


        Timber.d("Kopieren der Werte");
        Cursor cursor = db.query(Store.TABLE_NAME + "_REN", null, null, null, null, null, BaseColumns._ID + " ASC");
        try {
            while (cursor.moveToNext()) {
                String oldKey = cursor.getString(cursor.getColumnIndex("KEY"));
                String oldValue = cursor.getString(cursor.getColumnIndex("VALUE"));


                String newKey = oldKey.substring(oldKey.indexOf("/"));
                Timber.d(newKey);

                Cursor cursorNew = db.query(Store.TABLE_NAME, null, Store.Columns.KEY + " LIKE '" + newKey + "'", null, null, null, null);
                try {
                    if (cursorNew.moveToNext()) {
                        Store newStore = new Store(cursorNew);
                        newStore.setValue(oldValue);
                        db.update(Store.TABLE_NAME, newStore.getContentValues(), Store.Columns.KEY + " LIKE '" + newKey + "'", null);
                    } else {
                        Store newStore = new Store(newKey, oldValue);
                        db.insert(Store.TABLE_NAME, null, newStore.getContentValues());
                    }
                } finally {
                    cursorNew.close();
                }
            }
        } finally {
            cursor.close();
        }


        Timber.d("Löschen der alten Store Tabelle");
        db.execSQL("DROP TABLE " + Store.TABLE_NAME + "_REN;");


        Timber.d("Anlegen der Publications Tabelle");
        db.execSQL(CREATE_PUBLICATION_V2);
    }

    //---Version 6---
    private static final String CREATE_PAPER_V6 = "CREATE TABLE " + Paper.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Paper.Columns.DATE + " TEXT," +
            Paper.Columns.IMAGE + " TEXT," +
            Paper.Columns.IMAGEHASH + " TEXT," +
            Paper.Columns.LINK + " TEXT," +
            Paper.Columns.FILEHASH + " TEXT," +
            Paper.Columns.LEN + " INTEGER," +
            Paper.Columns.LASTMODIFIED + " INTEGER," +
            Paper.Columns.BOOKID + " TEXT," +
            Paper.Columns.RESOURCE + " TEXT," +
//            Paper.Columns.RESOURCEFILEHASH + " TEXT," +
//            Paper.Columns.RESOURCEURL + " TEXT," +
//            Paper.Columns.RESOURCELEN + " INTEGER," +
            Paper.Columns.ISDEMO + " INTEGER," +
            Paper.Columns.HASUPDATE + " INTEGER," +
            Paper.Columns.DOWNLOADID + " INTEGER," +
            Paper.Columns.ISDOWNLOADED + " INTEGER," +
            Paper.Columns.KIOSK + " INTEGER," +
            Paper.Columns.IMPORTED + " INTEGER," +
            Paper.Columns.TITLE + " TEXT," +
            Paper.Columns.VALIDUNTIL + " INTEGER," +
            Paper.Columns.PUBLICATIONID + " INTEGER" +
            ");";

    private static final String CREATE_STORE_V6 = "CREATE TABLE " + Store.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Store.Columns.KEY + " TEXT," +
            Store.Columns.VALUE + " TEXT" +
            ");";

//    private static final String CREATE_PUBLICATION_V6 = "CREATE TABLE " + Publication.TABLE_NAME + " (" +
//            Publication.Columns._ID + " INTEGER PRIMARY KEY," +
//            Publication.Columns.CREATED + " INTEGER," +
//            Publication.Columns.IMAGE + " TEXT," +
//            Publication.Columns.ISSUENAME + " TEXT," +
//            Publication.Columns.NAME + " TEXT," +
//            Publication.Columns.TYPENAME + " TEXT," +
//            Publication.Columns.URL + " TEXT," +
//            Publication.Columns.VALIDUNTIL + " INTEGER" +
//            ");";

    private static final String CREATE_RESOURCE_V6 = "CREATE TABLE " + Resource.TABLE_NAME + " (" +
            Resource.Columns.KEY + " TEXT PRIMARY KEY," +
            Resource.Columns.DOWNLOADID + " INTEGER," +
            Resource.Columns.DOWNLOADED + " INTEGER," +
            Resource.Columns.LEN + " INTEGER," +
            Resource.Columns.FILEHASH + " TEXT," +
            Resource.Columns.URL + " TEXT" +
            ")";

    //---Version 5---
    private static final String CREATE_PAPER_V5 = "CREATE TABLE " + Paper.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Paper.Columns.DATE + " TEXT," +
            Paper.Columns.IMAGE + " TEXT," +
            Paper.Columns.IMAGEHASH + " TEXT," +
            Paper.Columns.LINK + " TEXT," +
            Paper.Columns.FILEHASH + " TEXT," +
            Paper.Columns.LEN + " INTEGER," +
            Paper.Columns.LASTMODIFIED + " INTEGER," +
            Paper.Columns.BOOKID + " TEXT," +
            Paper.Columns.RESOURCE + " TEXT," +
//            Paper.Columns.RESOURCEFILEHASH + " TEXT," +
//            Paper.Columns.RESOURCEURL + " TEXT," +
            Paper.Columns.ISDEMO + " INTEGER," +
            Paper.Columns.HASUPDATE + " INTEGER," +
            Paper.Columns.DOWNLOADID + " INTEGER," +
            Paper.Columns.ISDOWNLOADED + " INTEGER," +
            Paper.Columns.KIOSK + " INTEGER," +
            Paper.Columns.IMPORTED + " INTEGER," +
            Paper.Columns.TITLE + " TEXT," +
            Paper.Columns.VALIDUNTIL + " INTEGER," +
            Paper.Columns.PUBLICATIONID + " INTEGER" +
            ");";

    private static final String CREATE_STORE_V5 = "CREATE TABLE " + Store.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Store.Columns.KEY + " TEXT," +
            Store.Columns.VALUE + " TEXT" +
            ");";

//    private static final String CREATE_PUBLICATION_V5 = "CREATE TABLE " + Publication.TABLE_NAME + " (" +
//            Publication.Columns._ID + " INTEGER PRIMARY KEY," +
//            Publication.Columns.CREATED + " INTEGER," +
//            Publication.Columns.IMAGE + " TEXT," +
//            Publication.Columns.ISSUENAME + " TEXT," +
//            Publication.Columns.NAME + " TEXT," +
//            Publication.Columns.TYPENAME + " TEXT," +
//            Publication.Columns.URL + " TEXT," +
//            Publication.Columns.VALIDUNTIL + " INTEGER" +
//            ");";

    private static final String CREATE_RESOURCE_V5 = "CREATE TABLE " + Resource.TABLE_NAME + " (" +
            Resource.Columns.KEY + " TEXT PRIMARY KEY," +
            Resource.Columns.DOWNLOADID + " INTEGER," +
            Resource.Columns.DOWNLOADED + " INTEGER" +
            ")";


    //---Version 4---
    private static final String CREATE_PAPER_V4 = "CREATE TABLE " + Paper.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Paper.Columns.DATE + " TEXT," +
            Paper.Columns.IMAGE + " TEXT," +
            Paper.Columns.IMAGEHASH + " TEXT," +
            Paper.Columns.LINK + " TEXT," +
            Paper.Columns.FILEHASH + " TEXT," +
            Paper.Columns.LEN + " INTEGER," +
            Paper.Columns.LASTMODIFIED + " INTEGER," +
            Paper.Columns.BOOKID + " TEXT," +
            Paper.Columns.RESOURCE + " TEXT," +
//            Paper.Columns.RESOURCEFILEHASH + " TEXT," +
//            Paper.Columns.RESOURCEURL + " TEXT," +
            Paper.Columns.ISDEMO + " INTEGER," +
            Paper.Columns.HASUPDATE + " INTEGER," +
            Paper.Columns.DOWNLOADID + " INTEGER," +
            Paper.Columns.ISDOWNLOADED + " INTEGER," +
            Paper.Columns.KIOSK + " INTEGER," +
            Paper.Columns.IMPORTED + " INTEGER," +
            Paper.Columns.TITLE + " TEXT," +
            Paper.Columns.PUBLICATIONID + " INTEGER" +
            ");";

    private static final String CREATE_STORE_V4 = "CREATE TABLE " + Store.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Store.Columns.KEY + " TEXT," +
            Store.Columns.VALUE + " TEXT" +
            ");";

//    private static final String CREATE_PUBLICATION_V4 = "CREATE TABLE " + Publication.TABLE_NAME + " (" +
//            Publication.Columns._ID + " INTEGER PRIMARY KEY," +
//            Publication.Columns.CREATED + " INTEGER," +
//            Publication.Columns.IMAGE + " TEXT," +
//            Publication.Columns.ISSUENAME + " TEXT," +
//            Publication.Columns.NAME + " TEXT," +
//            Publication.Columns.TYPENAME + " TEXT," +
//            Publication.Columns.URL + " TEXT," +
//            Publication.Columns.VALIDUNTIL + " INTEGER" +
//            ");";

    private static final String CREATE_RESOURCE_V4 = "CREATE TABLE " + Resource.TABLE_NAME + " (" +
            Resource.Columns.KEY + " TEXT PRIMARY KEY," +
            Resource.Columns.DOWNLOADID + " INTEGER," +
            Resource.Columns.DOWNLOADED + " INTEGER" +
            ")";


    //---Version 3---
    private static final String CREATE_PAPER_V3 = "CREATE TABLE " + Paper.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Paper.Columns.DATE + " TEXT," +
            Paper.Columns.IMAGE + " TEXT," +
            Paper.Columns.IMAGEHASH + " TEXT," +
            Paper.Columns.LINK + " TEXT," +
            Paper.Columns.FILEHASH + " TEXT," +
            Paper.Columns.LEN + " INTEGER," +
            Paper.Columns.LASTMODIFIED + " INTEGER," +
            Paper.Columns.BOOKID + " TEXT," +
            Paper.Columns.ISDEMO + " INTEGER," +
            "filename" + " TEXT," +
            //                    Paper.Columns.TEMPFILEPATH + " TEXT," +
            //                    Paper.Columns.TEMPFILENAME + " TEXT," +
            Paper.Columns.HASUPDATE + " INTEGER," +
            Paper.Columns.DOWNLOADID + " INTEGER," +
            //                    Paper.Columns.DOWNLOADPROGRESS + " INTEGER," +
            Paper.Columns.ISDOWNLOADED + " INTEGER," +
            Paper.Columns.KIOSK + " INTEGER," +
            Paper.Columns.IMPORTED + " INTEGER," +
            Paper.Columns.TITLE + " TEXT," +
            Paper.Columns.PUBLICATIONID + " INTEGER" +
            ");";

    private static final String CREATE_STORE_V3 = "CREATE TABLE " + Store.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Store.Columns.KEY + " TEXT," +
            Store.Columns.VALUE + " TEXT" +
            ");";

//    private static final String CREATE_PUBLICATION_V3 = "CREATE TABLE " + Publication.TABLE_NAME + " (" +
//            Publication.Columns._ID + " INTEGER PRIMARY KEY," +
//            Publication.Columns.CREATED + " INTEGER," +
//            Publication.Columns.IMAGE + " TEXT," +
//            Publication.Columns.ISSUENAME + " TEXT," +
//            Publication.Columns.NAME + " TEXT," +
//            Publication.Columns.TYPENAME + " TEXT," +
//            Publication.Columns.URL + " TEXT," +
//            Publication.Columns.VALIDUNTIL + " INTEGER" +
//            ");";


    //---Version 2---
    private static final String CREATE_PAPER_V2 = "CREATE TABLE " + Paper.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Paper.Columns.DATE + " TEXT," +
            Paper.Columns.IMAGE + " TEXT," +
            Paper.Columns.IMAGEHASH + " TEXT," +
            Paper.Columns.LINK + " TEXT," +
            Paper.Columns.FILEHASH + " TEXT," +
            Paper.Columns.LEN + " INTEGER," +
            Paper.Columns.LASTMODIFIED + " INTEGER," +
            Paper.Columns.BOOKID + " TEXT," +
            Paper.Columns.ISDEMO + " INTEGER," +
            "filename" + " TEXT," +
            //                    Paper.Columns.TEMPFILEPATH + " TEXT," +
            //                    Paper.Columns.TEMPFILENAME + " TEXT," +
            Paper.Columns.HASUPDATE + " INTEGER," +
            "isDownloading" + " INTEGER," +
            //                    Paper.Columns.DOWNLOADPROGRESS + " INTEGER," +
            Paper.Columns.ISDOWNLOADED + " INTEGER," +
            Paper.Columns.KIOSK + " INTEGER," +
            Paper.Columns.IMPORTED + " INTEGER," +
            Paper.Columns.TITLE + " TEXT," +
            Paper.Columns.PUBLICATIONID + " INTEGER" +
            ");";

    private static final String CREATE_STORE_V2 = "CREATE TABLE " + Store.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Store.Columns.KEY + " TEXT," +
            Store.Columns.VALUE + " TEXT" +
            ");";

    private static final String CREATE_PUBLICATION_V2 = "CREATE TABLE " + Publication.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            Publication.Columns.CREATED + " INTEGER," +
            Publication.Columns.IMAGE + " TEXT," +
            Publication.Columns.ISSUENAME + " TEXT," +
            Publication.Columns.NAME + " TEXT," +
            Publication.Columns.TYPENAME + " TEXT," +
            Publication.Columns.URL + " TEXT," +
            Publication.Columns.VALIDUNTIL + " INTEGER" +
            ");";

}
