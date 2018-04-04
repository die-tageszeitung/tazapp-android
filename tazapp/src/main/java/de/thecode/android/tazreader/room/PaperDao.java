package de.thecode.android.tazreader.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import de.thecode.android.tazreader.data.Paper;

import java.util.List;

@Dao
public abstract class PaperDao implements BaseDao<Paper>{

    @Query("SELECT * FROM PAPER ORDER BY date DESC")
    public abstract List<Paper> getAllPapers();

    @Query("SELECT * FROM PAPER ORDER BY date DESC LIMIT 1")
    public abstract Paper getLatestPaper();

    @Query("SELECT * FROM PAPER WHERE bookId LIKE :bookId")
    public abstract Paper getPaper(String bookId);

    @Query("SELECT * FROM PAPER ORDER BY date DESC")
    public abstract LiveData<List<Paper>> getLivePapersForLibrary();

    @Query("SELECT * FROM PAPER WHERE demo = 1 ORDER BY date DESC")
    public abstract LiveData<List<Paper>> getLivePapersForDemoLibrary();




}
