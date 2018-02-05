package de.thecode.android.tazreader.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;

/**
 * Created by mate on 12.01.2018.
 */

@Dao
public interface ResourceDao {

    @Query("SELECT * FROM " + Resource.TABLE_NAME + " WHERE " + Resource.Columns.KEY + " LIKE :key")
    Resource getResourceByKey(String key);

}
