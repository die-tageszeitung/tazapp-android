package de.thecode.android.tazreader.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by mate on 02.03.18.
 */

public class PaperRepository {


    private static volatile PaperRepository mInstance;

    public static PaperRepository getInstance(Context context) {
        if (mInstance == null) {
            synchronized (PaperRepository.class) {
                if (mInstance == null) {
                    mInstance = new PaperRepository(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    ContentResolver contentResolver;

    private PaperRepository(Context context) {
        contentResolver = context.getContentResolver();
    }

    public Paper getPaperWithBookId(String bookId) {
        Uri bookIdUri = Paper.CONTENT_URI.buildUpon()
                                   .appendPath(bookId)
                                   .build();
        Cursor cursor = contentResolver
                               .query(bookIdUri, null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return new Paper(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

}
