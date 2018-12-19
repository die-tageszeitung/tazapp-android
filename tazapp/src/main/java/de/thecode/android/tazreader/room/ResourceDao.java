package de.thecode.android.tazreader.room;

import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceWithDownloadState;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

@Dao
public abstract class ResourceDao implements BaseDao<Resource> {

    @Query("SELECT RESOURCE.*,DOWNLOADS.state as downloadState FROM RESOURCE LEFT OUTER JOIN DOWNLOADS ON DOWNLOADS.`key` = RESOURCE.`key` WHERE RESOURCE.`key` LIKE :key")
    public abstract ResourceWithDownloadState resourceWithKey(String key);

    @Query("SELECT * FROM RESOURCE")
    public abstract List<Resource> resources();

    @Query("SELECT * FROM RESOURCE WHERE `key` LIKE :key")
    public abstract LiveData<Resource> liveResourceWithKey(String key);

}
