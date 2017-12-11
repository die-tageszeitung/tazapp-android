package de.thecode.android.tazreader.persistence.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import de.thecode.android.tazreader.data.Paper;

/**
 * Created by mate on 11.12.2017.
 */

@Database(entities = {Paper.class}, version = 1)
public abstract class TazappDatabase extends RoomDatabase {

    public abstract PaperDao paperDao();

}
