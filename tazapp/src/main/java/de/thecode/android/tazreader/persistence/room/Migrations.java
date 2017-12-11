package de.thecode.android.tazreader.persistence.room;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

import timber.log.Timber;

/**
 * Created by mate on 11.12.2017.
 */

public class Migrations {

    static final Migration MIGRATION_0_1 = new Migration(0, 1) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("Migration 0->1");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.i("Migration 6->7");
        }
    };

}
