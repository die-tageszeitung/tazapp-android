package de.thecode.android.tazreader.persistence.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.database.Cursor;

import de.thecode.android.tazreader.data.Paper;

import java.util.List;

/**
 * Created by mate on 11.12.2017.
 */

@Dao
public interface PaperDao {

    @Query("SELECT * FROM " + Paper.TABLE_NAME + " ORDER BY " + Paper.Columns.DATE + " DESC")
    LiveData<List<Paper>> getPapers();

    @Query("SELECT * FROM " + Paper.TABLE_NAME + " WHERE " + Paper.Columns.VALIDUNTIL + " >= :validUntil ORDER BY " + Paper.Columns.DATE + " DESC")
    LiveData<List<Paper>> getPapers(long validUntil);


    @Query("SELECT * FROM " + Paper.TABLE_NAME + " WHERE " + Paper.Columns.ISDOWNLOADED + " = :isDownloaded AND " + Paper.Columns.KIOSK + " = :isKiosk AND " + Paper.Columns.IMPORTED + " = :isImported ORDER BY " + Paper.Columns.DATE + " DESC")
    List<Paper> getPapersAsList(boolean isDownloaded, boolean isKiosk, boolean isImported);


    @Query("SELECT * FROM " + Paper.TABLE_NAME + " ORDER BY " + Paper.Columns.DATE + " DESC")
    Cursor getPapersAsCursor();

    @Query("SELECT * FROM " + Paper.TABLE_NAME + " WHERE " + Paper.Columns.BOOKID + " LIKE :bookId")
    Paper getPaperWithBookId(String bookId);

    @Insert(onConflict = OnConflictStrategy.FAIL)
    void insertPaper(Paper paper);

    @Update(onConflict = OnConflictStrategy.FAIL)
    void updatePaper(Paper paper);

    @Delete
    void deletePaper(Paper paper);

}
