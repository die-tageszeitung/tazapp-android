package de.thecode.android.tazreader.dialog;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.common.base.Strings;

import java.util.ArrayList;

/**
 * Created by mate on 19.03.2015.
 */
public class ArchiveDialog extends DialogAdapterList {

    @Override
    public ArrayList<ArchiveEntry> getEntries() {
        return getArguments().getParcelableArrayList(ARG_ARRAY_PARCABLE_ARCHIVEENTRIES);
    }

    @Override
    public ListAdapter getAdapter(final Activity activity) {
        final ArrayList<ArchiveEntry> entries = getEntries();
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return entries.size();
            }

            @Override
            public ArchiveEntry getItem(int position) {
                if (entries != null) {
                    if (entries.size() > position) return entries.get(position);
                }
                return null;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder viewHolder;
                if (convertView == null) {
                    convertView = activity.getLayoutInflater()
                                          .inflate(android.R.layout.simple_list_item_1, parent, false);
                    viewHolder = new ViewHolder();
                    viewHolder.text = (TextView) convertView.findViewById(android.R.id.text1);
                    viewHolder.text.setGravity(Gravity.CENTER);
                    convertView.setTag(viewHolder);
                } else viewHolder = (ViewHolder) convertView.getTag();
                viewHolder.text.setText(entries.get(position).getName());
                return convertView;
            }

            class ViewHolder {
                TextView text;
            }
        };
    }


    public static class ArchiveEntry extends TcDialogAdapterListEntry {
        int number;
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

        public static final Parcelable.Creator<ArchiveEntry> CREATOR = new Parcelable.Creator<ArchiveEntry>() {
            public ArchiveEntry createFromParcel(Parcel source) {
                return new ArchiveEntry(source);
            }

            public ArchiveEntry[] newArray(int size) {
                return new ArchiveEntry[size];
            }
        };

    }



}
