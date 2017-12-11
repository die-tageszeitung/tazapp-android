package de.thecode.android.tazreader.persistence.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import de.thecode.android.tazreader.data.Paper;

import java.util.List;

/**
 * Created by mate on 11.12.2017.
 */

@Dao
public interface PaperDao {

    @Query("SELECT * FROM Paper")
    LiveData<List<Paper>> getPapers();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Long insertPaper(Paper paper);


}
