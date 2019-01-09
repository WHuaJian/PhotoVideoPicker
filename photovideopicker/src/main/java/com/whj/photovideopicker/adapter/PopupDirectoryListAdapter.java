package com.whj.photovideopicker.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.facebook.drawee.view.SimpleDraweeView;
import com.whj.photovideopicker.R;
import com.whj.photovideopicker.model.PhotoDirectory;
import com.whj.photovideopicker.utils.PickerUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by user
 */
public class PopupDirectoryListAdapter extends BaseAdapter {

    private Context context;

    private List<PhotoDirectory> directories = new ArrayList<>();

    private LayoutInflater mLayoutInflater;

    private int mCheckPosition = 0;

    public PopupDirectoryListAdapter(Context context, List<PhotoDirectory> directories) {
        this.context = context;
        this.directories = directories;

        mLayoutInflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return directories.size();
    }


    @Override
    public PhotoDirectory getItem(int position) {
        return directories.get(position);
    }


    @Override
    public long getItemId(int position) {
        return directories.get(position).hashCode();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.item_photo_album, parent, false);
            holder.ivAlbum = (SimpleDraweeView) convertView.findViewById(R.id.iv_album);
            holder.ivIndex = (ImageView) convertView.findViewById(R.id.iv_index);
            holder.tvName = (TextView) convertView.findViewById(R.id.tv_name_la);
            holder.tvCount = (TextView) convertView.findViewById(R.id.tv_count_la);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.bindData(directories.get(position), position);

        return convertView;
    }


    private class ViewHolder {
        SimpleDraweeView ivAlbum;
        ImageView ivIndex;
        TextView tvName;
        TextView tvCount;

        public void bindData(PhotoDirectory directory, int position) {
            if (context instanceof Activity && ((Activity) context).isFinishing()) {
                return;
            }

//            Uri uri = Uri.parse("file://" + directory.getCoverPath());
            PickerUtils.loadImage(ivAlbum, "file://" + directory.getCoverPath());
//            ivAlbum.setImageURI(uri);
            tvName.setText(directory.getName());
            tvCount.setText(context.getString(R.string.image_count, directory.getPhotos().size()));
            if (mCheckPosition == position) {
                ivIndex.setVisibility(View.VISIBLE);
            } else {
                ivIndex.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void setCheckPosition(int pos) {
        this.mCheckPosition = pos;
    }
}
