package de.thecode.android.tazreader.room;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

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
