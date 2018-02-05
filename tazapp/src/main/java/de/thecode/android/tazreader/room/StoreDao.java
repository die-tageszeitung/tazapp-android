package de.thecode.android.tazreader.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.net.Uri;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Store;

/**
 * Created by mate on 12.01.2018.
 */

@Dao
public abstract class StoreDao {

    @Query("SELECT "+Store.Columns.VALUE +" FROM "+ Store.TABLE_NAME + " WHERE `"+Store.Columns.KEY+"` LIKE :path")
    public abstract String getValueByPath(String path);


    public static String getPathForPaperAndKey(Paper paper, String key) {
        return getPathForKey(paper.getBookId(),key);
    }

    public static String getPathForKey(String path, String key) {
        Uri.Builder builder = Uri.parse("/").buildUpon();
        builder.appendPath(path);
        builder.appendPath(key);
        return builder.build().toString();
    }


    @Query("SELECT "+Store.Columns.VALUE +" FROM "+ Store.TABLE_NAME + " WHERE `"+Store.Columns.KEY+"` LIKE :path")
    public abstract LiveData<String> getLiveValueByPath(String path);



}
