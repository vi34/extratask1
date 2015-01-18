package com.example.vi34.flickrv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by vi34 on 16.01.15.
 */
public class PhotoAdapter extends BaseAdapter {

    public List<MyPhoto> mData;

    public PhotoAdapter(List<MyPhoto> data) {
        this.mData = data;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View cell = LayoutInflater.from(parent.getContext()).inflate(R.layout.cellgrid, parent, false);
        ImageView imageView = (ImageView) cell.findViewById(R.id.imagepart);
        TextView textView = (TextView) cell.findViewById(R.id.textpart);
        imageView.setImageBitmap(mData.get(position).getBitmap());
        textView.setText("by " + mData.get(position).author);
        return cell;
    }

}