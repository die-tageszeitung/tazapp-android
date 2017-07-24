package de.thecode.android.tazreader.push;

/**
 * Created by mate on 24.07.2017.
 */

public class PushNotification {

    public enum Type {
        unknown, alert, debug, newIssue;
        public static Type fromString(String name) {
            try {
                return Type.valueOf(name);
            } catch (IllegalArgumentException ignored) {}
            return unknown;
        }
    }

    final Type   type;
    final String title;
    final String body;
    final String url;
    final String issue;

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
}
