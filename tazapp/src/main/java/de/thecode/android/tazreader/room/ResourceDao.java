package de.thecode.android.tazreader.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import de.thecode.android.tazreader.data.Paper;
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
