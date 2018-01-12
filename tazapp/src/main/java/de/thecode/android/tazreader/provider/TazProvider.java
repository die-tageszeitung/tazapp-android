package de.thecode.android.tazreader.provider;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.room.AppDatabase;

import java.util.Set;

import timber.log.Timber;
//import de.thecode.android.tazreader.utils.Log;


public class TazProvider extends ContentProvider {

    private static final int PAPER_DIR       = 1;
    private static final int PAPER_ID        = 2;
    private static final int STORE_KEY       = 3;
    private static final int STORE_DIR       = 4;
    private static final int STORE_PATH      = 10;
    private static final int PAPER_BOOKID    = 5;
    private static final int PUBLICATION_DIR = 6;
    private static final int PUBLICATION_ID  = 7;
    private static final int RESOURCE_DIR    = 8;
    private static final int RESOURCE_KEY    = 9;

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    private static UriMatcher sUriMatcher;

    //private SQLiteDatabase mDb;
    private SupportSQLiteDatabase mDb;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, Paper.TABLE_NAME, PAPER_DIR);
        sUriMatcher.addURI(AUTHORITY, Paper.TABLE_NAME + "/#", PAPER_ID);
        sUriMatcher.addURI(AUTHORITY, Paper.TABLE_NAME + "/*", PAPER_BOOKID);
        sUriMatcher.addURI(AUTHORITY, Store.TABLE_NAME + "/*/*", STORE_KEY);
        sUriMatcher.addURI(AUTHORITY, Store.TABLE_NAME + "/*", STORE_PATH);
        sUriMatcher.addURI(AUTHORITY, Store.TABLE_NAME, STORE_DIR);
        sUriMatcher.addURI(AUTHORITY, Publication.TABLE_NAME, PUBLICATION_DIR);
        sUriMatcher.addURI(AUTHORITY, Publication.TABLE_NAME + "/#", PUBLICATION_ID);
        sUriMatcher.addURI(AUTHORITY, Resource.TABLE_NAME, RESOURCE_DIR);
        sUriMatcher.addURI(AUTHORITY, Resource.TABLE_NAME + "/*", RESOURCE_KEY);

    }

    @Override
    public boolean onCreate() {
        mDb = AppDatabase.getInstance(getContext()).getOpenHelper()
                         .getWritableDatabase();
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        int match = sUriMatcher.match(uri);

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (match) {

            case PAPER_DIR:
                queryBuilder.setTables(Paper.TABLE_NAME);
                break;

            case PAPER_ID:
                queryBuilder.setTables(Paper.TABLE_NAME);
                long paperId = ContentUris.parseId(uri);
                queryBuilder.appendWhere(Paper.Columns._ID + " = " + paperId);
                break;

            case PAPER_BOOKID:
                queryBuilder.setTables(Paper.TABLE_NAME);
                String bookId = uri.getLastPathSegment();
                queryBuilder.appendWhere(Paper.Columns.BOOKID + " LIKE '" + bookId + "'");
                break;

            case STORE_KEY:
                queryBuilder.setTables(Store.TABLE_NAME);
                String key = uri.toString();
                key = key.replace(Store.CONTENT_URI.toString(), "");
                queryBuilder.appendWhere(Store.Columns.KEY + " LIKE '" + key + "'");
                break;

            case STORE_DIR:
                queryBuilder.setTables(Store.TABLE_NAME);
                break;

            case PUBLICATION_DIR:
                queryBuilder.setTables(Publication.TABLE_NAME);
                break;

            case PUBLICATION_ID:
                queryBuilder.setTables(Publication.TABLE_NAME);
                long publicationId = ContentUris.parseId(uri);
                queryBuilder.appendWhere(Publication.Columns._ID + " = " + publicationId);
                break;

            case RESOURCE_DIR:
                queryBuilder.setTables(Resource.TABLE_NAME);
                break;

            case RESOURCE_KEY:
                queryBuilder.setTables(Resource.TABLE_NAME);
                String resourceKey = uri.getLastPathSegment();
                queryBuilder.appendWhere(Resource.Columns.KEY + " LIKE '" + resourceKey + "'");
                break;


            default:
                throw new IllegalArgumentException("unsupported uri: " + uri);
        }

        String query = queryBuilder.buildQuery(projection, selection, null, null, sortOrder, null);
        Timber.d("query: %s", query);
        Cursor queryCursor = mDb.query(query);
        queryCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return queryCursor;
    }

    @Override
    public String getType(Uri uri) {
        //Log.v();
        switch (sUriMatcher.match(uri)) {
            case PAPER_DIR:
                return Paper.CONTENT_TYPE;
            case PAPER_ID:
                return Paper.CONTENT_ITEM_TYPE;
            case STORE_DIR:
                return Store.CONTENT_TYPE;
            case STORE_KEY:
                return Store.CONTENT_ITEM_TYPE;
            case PUBLICATION_DIR:
                return Publication.CONTENT_TYPE;
            case PUBLICATION_ID:
                return Publication.CONTENT_ITEM_TYPE;
            case RESOURCE_DIR:
                return Resource.CONTENT_TYPE;
            case RESOURCE_KEY:
                return Resource.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown taz-provider type: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {

        Timber.d("--------------------------------------------------------------------");
        Timber.d("uri: %s", uri);

        int match = sUriMatcher.match(uri);
        long rowId = -1;
        switch (match) {
            case STORE_DIR:
                //FIXME Prüfen ob Key vorhanden
                rowId = mDb.insert(Store.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                break;
            case PAPER_DIR:
                rowId = mDb.insert(Paper.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                break;
            case PUBLICATION_DIR:
                rowId = mDb.insert(Publication.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                break;
            case RESOURCE_DIR:
                rowId = mDb.insert(Resource.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                if (rowId > -1) {
                    Uri contentUri = Uri.withAppendedPath(uri, values.getAsString(Resource.Columns.KEY));
                    return contentUri;
                }
                break;

            default:
                throw new IllegalArgumentException("unsupported uri: " + uri);
        }
        if (rowId > -1) {
            Uri contenUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver()
                        .notifyChange(contenUri, null);
            Timber.d("%s", contenUri);
            return contenUri;
        }

        throw new SQLException("Could not insert in " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int match = sUriMatcher.match(uri);
        //Log.d(match,uri,selection,selectionArgs);
        int affected;
        switch (match) {
            case STORE_KEY:
                String key = uri.toString();
                key = key.replace(Store.CONTENT_URI.toString(), "");
                affected = mDb.delete(Store.TABLE_NAME,
                                      Store.Columns.KEY + " LIKE '" + key + "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;

            case STORE_DIR:
                affected = mDb.delete(Store.TABLE_NAME, selection, selectionArgs);
                break;

            case STORE_PATH:
                String path = uri.toString();
                path = path.replace(Store.CONTENT_URI.toString(), "");
                affected = mDb.delete(Store.TABLE_NAME,
                                      Store.Columns.KEY + " LIKE '" + path + "%'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;

            case PAPER_DIR:
                affected = mDb.delete(Paper.TABLE_NAME, selection, selectionArgs);
                break;

            case PAPER_ID:
                long paperId = ContentUris.parseId(uri);
                affected = mDb.delete(Paper.TABLE_NAME,
                                      Paper.Columns._ID + " = " + paperId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;

            case PUBLICATION_DIR:
                affected = mDb.delete(Publication.TABLE_NAME, selection, selectionArgs);
                break;

            case PUBLICATION_ID:
                long publicationId = ContentUris.parseId(uri);
                affected = mDb.delete(Publication.TABLE_NAME,
                                      Publication.Columns._ID + " = " + publicationId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;

            case RESOURCE_DIR:
                affected = mDb.delete(Resource.TABLE_NAME, selection, selectionArgs);
                break;

            case RESOURCE_KEY:
                String resourceKey = uri.getLastPathSegment();
                affected = mDb.delete(Resource.TABLE_NAME,
                                      Resource.Columns.KEY + " LIKE '" + resourceKey + "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("unsupported uri: " + uri);
        }
        if (affected > 0) getContext().getContentResolver()
                                      .notifyChange(uri, null);
        Timber.d("%d %s", affected, uri);
        return affected;
    }




    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values != null) {
            Set<String> keys = values.keySet();
            for (String key : keys) {
                if (values.get(key) instanceof Boolean) {
                    Boolean value = values.getAsBoolean(key);
                    values.put(key,value?1:0);
                }
            }
        }



        Timber.d("--------------------------------------------------------------------");
        Timber.d("uri: %s", uri);
        Timber.d("selection: %s", selection);
        Timber.d("selectionArgs[]: %s", selectionArgs == null ? "null" : selectionArgs);
        int match = sUriMatcher.match(uri);
        int affected;
        switch (match) {
            case STORE_KEY:
                String key = uri.toString();
                key = key.replace(Store.CONTENT_URI.toString(), "");
                affected = mDb.update(Store.TABLE_NAME,
                                      SQLiteDatabase.CONFLICT_FAIL,
                                      values,
                                      Store.Columns.KEY + " LIKE '" + key + "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;
            case PAPER_ID:
                long paperId = ContentUris.parseId(uri);
                affected = mDb.update(Paper.TABLE_NAME,
                                      SQLiteDatabase.CONFLICT_FAIL,
                                      values,
                                      Paper.Columns._ID + " = " + paperId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;
            case PAPER_DIR:
                affected = mDb.update(Paper.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, values, selection, selectionArgs);
                break;
            case PUBLICATION_ID:
                long publicationId = ContentUris.parseId(uri);
                affected = mDb.update(Publication.TABLE_NAME,
                                      SQLiteDatabase.CONFLICT_FAIL,
                                      values,
                                      Publication.Columns._ID + " = " + publicationId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;

            case RESOURCE_DIR:
                affected = mDb.update(Resource.TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, values, selection, selectionArgs);
                break;

            case RESOURCE_KEY:
                String resourceKey = uri.getLastPathSegment();
                affected = mDb.update(Resource.TABLE_NAME,
                                      SQLiteDatabase.CONFLICT_FAIL,
                                      values,
                                      Resource.Columns.KEY + " LIKE '" + resourceKey + "'" + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                                      selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("unsupported uri: " + uri);
        }
        if (affected > 0) getContext().getContentResolver()
                                      .notifyChange(uri, null);

        Timber.d("%d %s", affected, uri);

        return affected;
    }
}
