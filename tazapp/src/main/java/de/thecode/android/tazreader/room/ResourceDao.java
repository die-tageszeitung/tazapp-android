package de.thecode.android.tazreader.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import de.thecode.android.tazreader.data.Resource;

import java.util.List;

@Dao
public abstract class ResourceDao implements BaseDao<Resource> {

    @Query("SELECT * FROM RESOURCE WHERE `key` LIKE :key")
    public abstract Resource resourceWithKey(String key);

    @Query("SELECT * FROM RESOURCE")
    public abstract List<Resource> resources();


    @Query("SELECT * FROM RESOURCE WHERE downloadId = :downloadId")
    public abstract Resource resourceWithDownloadId(long downloadId);



    @Query("SELECT * FROM RESOURCE WHERE `key` LIKE :key")
    public abstract LiveData<Resource> liveResourceWithKey(String key);

}
