package org.hwyl.sexytopo.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import org.hwyl.sexytopo.R;

import java.util.List;

public class CustomSpinnerAdapter extends ArrayAdapter implements SpinnerAdapter {

    private final List<Integer> objects; // android.graphics.Color list
    private final Context context;

    public CustomSpinnerAdapter(Context context, List<Integer> objects) {
        super(context, R.layout.test_layout, objects);
        this.context = context;
        this.objects = objects;

    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        super.getDropDownView(position, convertView, parent);

        View rowView = convertView;


        if (rowView == null) {
            // Get a new instance of the row layout view
            LayoutInflater inflater = LayoutInflater.from(context);
            //LayoutInflater inflater = this.activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.test_layout, null);

            rowView.setBackgroundColor(objects.get(position));

        } else {
            rowView.setBackgroundColor(objects.get(position));
        }


        return rowView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;


        if (rowView == null) {
            // Get a new instance of the row layout view
            LayoutInflater inflater = LayoutInflater.from(context);
            //LayoutInflater inflater = this.activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.test_layout, null);

            rowView.setBackgroundColor(objects.get(position));

        } else {
            rowView.setBackgroundColor(objects.get(position));
        }


        return rowView;
    }
}