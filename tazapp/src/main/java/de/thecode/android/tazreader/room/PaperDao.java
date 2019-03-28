package de.thecode.android.tazreader.room;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperWithDownloadState;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

@Dao
public abstract class PaperDao implements BaseDao<Paper>{

    @Query("SELECT * FROM PAPER ORDER BY date DESC")
    public abstract List<Paper> getAllPapers();

    @Query("SELECT * FROM PAPER ORDER BY date DESC LIMIT 1")
    public abstract Paper getLatestPaper();

    @Query("SELECT * FROM PAPER WHERE bookId LIKE :bookId")
    public abstract Paper getPaper(String bookId);

    @Query("SELECT * FROM PAPER WHERE bookId IN (:bookIds)")
    public abstract List<Paper> getPapers(String... bookIds);

    @Query("SELECT PAPER.*,DOWNLOADS.state as downloadState, DOWNLOADS.progress as progress FROM PAPER LEFT OUTER JOIN DOWNLOADS ON DOWNLOADS.`key` = :bookId WHERE bookId = :bookId")
    public abstract PaperWithDownloadState get(String bookId);


    @Query("SELECT PAPER.*,DOWNLOADS.state as downloadState, DOWNLOADS.progress as progress FROM PAPER LEFT OUTER JOIN DOWNLOADS ON DOWNLOADS.`key` = PAPER.bookId ORDER BY date DESC")
    public abstract LiveData<List<PaperWithDownloadState>> getLiveForLibrary();

    @Query("SELECT PAPER.*,DOWNLOADS.state as downloadState, DOWNLOADS.progress as progress FROM PAPER LEFT OUTER JOIN DOWNLOADS ON DOWNLOADS.`key` = PAPER.bookId WHERE demo = 1 ORDER BY date DESC")
    public abstract LiveData<List<PaperWithDownloadState>> getLiveForDemoLibrary();

    @Query("SELECT PAPER.*,DOWNLOADS.state as downloadState, DOWNLOADS.progress as progress FROM PAPER LEFT OUTER JOIN DOWNLOADS ON DOWNLOADS.`key` = PAPER.bookId WHERE bookId LIKE :bookId")
    public abstract LiveData<PaperWithDownloadState> getPaperLiveData(String bookId);

}
