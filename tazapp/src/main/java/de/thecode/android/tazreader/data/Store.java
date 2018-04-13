package de.thecode.android.tazreader.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.text.TextUtils;

@Entity(tableName = "STORE")
public class Store {


    @PrimaryKey
    @NonNull
    private String path;
    private String value;

    public Store(String path, String value) {
        if (!path.startsWith("/")) path = "/" + path;
        this.path = path;
        this.value = value;
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }

    public String getValue(String defaultValue) {
        return TextUtils.isEmpty(value) ? defaultValue : value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean hasValue() {
        return TextUtils.isEmpty(getValue());
    }

    public static String getPath(String bookId, String key) {
        return "/" + bookId + "/" + key;
    }

}
