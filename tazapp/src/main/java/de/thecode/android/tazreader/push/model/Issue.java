package de.thecode.android.tazreader.push.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mate on 14.08.2017.
 */

public class Issue implements Parcelable {

    private static final Gson gson;

    static {
        gson = new GsonBuilder().create();
    }

    long    validUntil;
    boolean isDemo;
    boolean isUpdate;
    String  date;
    String  image;
    String  imageHash;
    String  link;
    String  fileHash;
    long    len;
    long    lastModified;
    String  bookId;
    String  resource;
    String  resourceFileHash;
    String  resourceLen;
    String  resourceUrl;

    public static Issue fromJson(String json) {
        return gson.fromJson(json,Issue.class);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.validUntil);
        dest.writeByte(this.isDemo ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isUpdate ? (byte) 1 : (byte) 0);
        dest.writeString(this.date);
        dest.writeString(this.image);
        dest.writeString(this.imageHash);
        dest.writeString(this.link);
        dest.writeString(this.fileHash);
        dest.writeLong(this.len);
        dest.writeLong(this.lastModified);
        dest.writeString(this.bookId);
        dest.writeString(this.resource);
        dest.writeString(this.resourceFileHash);
        dest.writeString(this.resourceLen);
        dest.writeString(this.resourceUrl);
    }

    protected Issue(Parcel in) {
        this.validUntil = in.readLong();
        this.isDemo = in.readByte() != 0;
        this.isUpdate = in.readByte() != 0;
        this.date = in.readString();
        this.image = in.readString();
        this.imageHash = in.readString();
        this.link = in.readString();
        this.fileHash = in.readString();
        this.len = in.readLong();
        this.lastModified = in.readLong();
        this.bookId = in.readString();
        this.resource = in.readString();
        this.resourceFileHash = in.readString();
        this.resourceLen = in.readString();
        this.resourceUrl = in.readString();
    }

    public static final Parcelable.Creator<Issue> CREATOR = new Parcelable.Creator<Issue>() {
        @Override
        public Issue createFromParcel(Parcel source) {
            return new Issue(source);
        }

        @Override
        public Issue[] newArray(int size) {
            return new Issue[size];
        }
    };
}
