package de.thecode.android.tazreader.push;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.os.Parcel;
import android.os.Parcelable;

import de.thecode.android.tazreader.push.model.Issue;

/**
 * Created by mate on 24.07.2017.
 */

public class PushNotification implements Parcelable {

    private static final Gson gson;

    static  {
        gson =  new GsonBuilder().setPrettyPrinting().setPrettyPrinting().create();
    }

    public enum Type {
        unknown, alert, debug, newIssue;

        public static Type fromString(String name) {
            try {
                return Type.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
            return unknown;
        }
    }

    private final Type   type;
    private       String body;
    private final String url;
    private final Issue issue;

    public PushNotification(String type, String body, String url, String issue) {
        this.type = Type.fromString(type);
        this.body = body;
        this.url = url;
        this.issue = Issue.fromJson(issue);
    }

    public Type getType() {
        return type;
    }

    public String getBody() {
        return body;
    }

    public String getUrl() {
        return url;
    }

    public Issue getIssue() {
        return issue;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeString(this.body);
        dest.writeString(this.url);
        dest.writeParcelable(this.issue, flags);
    }

    protected PushNotification(Parcel in) {
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : Type.values()[tmpType];
        this.body = in.readString();
        this.url = in.readString();
        this.issue = in.readParcelable(Issue.class.getClassLoader());
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
