package de.thecode.android.tazreader.room;

import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;

import java.util.List;

public interface BaseDao<T> {

    @SuppressWarnings("unchecked")
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(T... items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<T> itemList);

    @SuppressWarnings("unchecked")
    @Delete
    void delete(T... items);

    @Delete
    void delete(List<T> items);

}
