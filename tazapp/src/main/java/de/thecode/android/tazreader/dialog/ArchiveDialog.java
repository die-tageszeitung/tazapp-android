package de.thecode.android.tazreader.dialog;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import de.mateware.dialog.DialogAdapterList;

import java.util.ArrayList;

/**
 * Created by mate on 19.03.2015.
 */
public class ArchiveDialog extends DialogAdapterList<ArchiveEntry> {

    @Override
    public ListAdapter getAdapter(final ArrayList<ArchiveEntry> entries) {
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
                    convertView = LayoutInflater.from(getContext())
                                                             .inflate(android.R.layout.simple_list_item_1, parent, false);
                    viewHolder = new ViewHolder();
                    viewHolder.text = convertView.findViewById(android.R.id.text1);
                    viewHolder.text.setGravity(Gravity.CENTER);
                    convertView.setTag(viewHolder);
                } else viewHolder = (ViewHolder) convertView.getTag();
                viewHolder.text.setText(entries.get(position)
                                               .getName());
                return convertView;
            }

            class ViewHolder {
                TextView text;
            }
        };
    }


    public static class Builder extends DialogAdapterList.AbstractBuilder<ArchiveEntry,Builder,ArchiveDialog>{

        public Builder() {
            super(ArchiveDialog.class);
        }
    }


}
