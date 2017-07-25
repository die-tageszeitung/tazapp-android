package de.thecode.android.tazreader.push;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mate on 24.07.2017.
 */

public class PushNotification implements Parcelable {

    public enum Type {
        unknown, alert, debug, newIssue;
        public static Type fromString(String name) {
            try {
                return Type.valueOf(name);
            } catch (IllegalArgumentException ignored) {}
            return unknown;
        }
    }

    private final Type   type;
    private final String title;
    private final String body;
    private final String url;
    private final String issue;

    public PushNotification(String type, String title, String body, String url, String issue) {
        this.type = Type.fromString(type);
        this.title = title;
        this.body = body;
        this.url = url;
        this.issue = issue;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getUrl() {
        return url;
    }

    public String getIssue() {
        return issue;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeString(this.title);
        dest.writeString(this.body);
        dest.writeString(this.url);
        dest.writeString(this.issue);
    }

    protected PushNotification(Parcel in) {
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : Type.values()[tmpType];
        this.title = in.readString();
        this.body = in.readString();
        this.url = in.readString();
        this.issue = in.readString();
    }

    public static final Parcelable.Creator<PushNotification> CREATOR = new Parcelable.Creator<PushNotification>() {
        @Override
        public PushNotification createFromParcel(Parcel source) {
            return new PushNotification(source);
        }

        @Override
        public PushNotification[] newArray(int size) {
            return new PushNotification[size];
        }
    };
}
