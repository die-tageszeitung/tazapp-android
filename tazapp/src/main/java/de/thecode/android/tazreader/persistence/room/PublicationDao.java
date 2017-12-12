package de.thecode.android.tazreader.persistence.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import de.thecode.android.tazreader.data.Publication;

/**
 * Created by mate on 11.12.2017.
 */

@Dao
public interface PublicationDao {

    @Query("SELECT * FROM " + Publication.TABLE_NAME + " WHERE " + Publication.Columns.ISSUENAME + " LIKE :issueName")
    Publication getPuplicationByIssueName(String issueName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Publication publication);

}
