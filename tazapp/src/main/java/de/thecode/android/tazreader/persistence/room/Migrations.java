package de.thecode.android.tazreader.persistence.room;

import android.arch.persistence.room.migration.Migration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by mate on 11.12.2017.
 */

public class Migrations {

//    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            Timber.i("Migration 6->7");
//        }
//    };

    public static Migration[] getAllmigrations() {
        Field[] fields = Migrations.class.getDeclaredFields();
        List<Migration> migrations = new ArrayList<>();
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == Migration.class) {
                try {
                    migrations.add((Migration) f.get(null));
                } catch (IllegalAccessException e) {
                    Timber.w(e);
                }
            }
        }
        Migration[] result = new Migration[migrations.size()];
        return migrations.toArray(result);
    }
}
