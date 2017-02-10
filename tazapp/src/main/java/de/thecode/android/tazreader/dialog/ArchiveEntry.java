package de.thecode.android.tazreader.dialog;

import com.google.common.base.Strings;

import android.os.Parcel;

import de.mateware.dialog.DialogAdapterList;

/**
 * Created by mate on 10.02.2017.
 */
public class ArchiveEntry extends DialogAdapterList.DialogAdapterListEntry {
    int    number;
    String name;

    public ArchiveEntry(int number) {
        this.number = number;
    }

    public ArchiveEntry(int number, String name) {
        this.number = number;
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        if (Strings.isNullOrEmpty(name)) return String.valueOf(number);
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.number);
        dest.writeString(this.name);
    }

    private ArchiveEntry(Parcel in) {
        this.number = in.readInt();
        this.name = in.readString();
    }

    public static final Creator<ArchiveEntry> CREATOR = new Creator<ArchiveEntry>() {
        public ArchiveEntry createFromParcel(Parcel source) {
            return new ArchiveEntry(source);
        }

        public ArchiveEntry[] newArray(int size) {
            return new ArchiveEntry[size];
        }
    };

}
