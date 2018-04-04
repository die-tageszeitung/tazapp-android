package de.thecode.android.tazreader.room;

import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;

import java.util.List;

public interface BaseDao<T> {

    @SuppressWarnings("unchecked")
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(T... items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(List<T> itemList);

    @SuppressWarnings("unchecked")
    @Delete
    public void delete(T... items);

    @Delete
    public void delete(List<T> items);

}
