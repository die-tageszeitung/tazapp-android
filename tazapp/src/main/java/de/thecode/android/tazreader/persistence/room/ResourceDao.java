package de.thecode.android.tazreader.persistence.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;

/**
 * Created by mate on 13.12.2017.
 */

@Dao
public interface ResourceDao {

    @Query("SELECT * FROM " + Resource.TABLE_NAME + " WHERE " + Resource.Columns.KEY + " LIKE :key")
    LiveData<Resource> getResourceWithKey(String key);

    @Insert
    void insert(Resource resource);

    @Update
    void update(Resource resource);

}
