package de.thecode.android.tazreader.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import de.thecode.android.tazreader.data.Paper;

/**
 * Created by mate on 12.01.2018.
 */

@Dao
public interface PaperDao {

    @Query("SELECT * FROM " + Paper.TABLE_NAME + " WHERE " + Paper.Columns.BOOKID + " LIKE :bookId")
    Paper getPaperByBookId(String bookId);

}
