package de.thecode.android.tazreader.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import de.thecode.android.tazreader.data.Store;

import java.util.List;

@Dao
public abstract class StoreDao implements BaseDao<Store> {

    @Query("SELECT * FROM STORE WHERE path LIKE :path")
    public abstract Store withPath(String path);

    @Query("SELECT * FROM STORE")
    public abstract List<Store> getAll();

    @Query("SELECT * FROM STORE WHERE path LIKE '/' || :bookId || '/%'")
    public abstract List<Store> getAllForBookId(String bookId);


    @Query("DELETE FROM STORE WHERE path LIKE :path")
    public abstract void deleteWithPath(String path);

    @Query("SELECT * FROM STORE WHERE path LIKE :path")
    public abstract LiveData<Store> liveWithPath(String path);

    @Query("SELECT * FROM STORE WHERE path LIKE '/' || :bookId || '/%'")
    public abstract LiveData<List<Store>> liveAllForBookId(String bookId);


}
