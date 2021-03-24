package com.whj.photovideopicker.adapter;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.whj.photovideopicker.R;
import com.whj.photovideopicker.model.VideoDirectory;
import com.whj.photovideopicker.utils.PickerUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by user
 */
public class PopupVideoDirectoryListAdapter extends BaseAdapter {

    private Context context;

    private List<VideoDirectory> videoDirectories = new ArrayList<>();

    private LayoutInflater mLayoutInflater;

    private int mCheckPosition = 0;

    private boolean isVideo =false;

    public PopupVideoDirectoryListAdapter(Context context, List<VideoDirectory> directories) {
        this.context = context;
        this.videoDirectories = directories;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return videoDirectories.size();

    }


    @Override
    public VideoDirectory getItem(int position) {
        return videoDirectories.get(position);
    }


    @Override
    public long getItemId(int position) {
        return videoDirectories.get(position).hashCode();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.item_photo_album, parent, false);
            holder.ivAlbum = (ImageView) convertView.findViewById(R.id.iv_album);
            holder.ivIndex = (ImageView) convertView.findViewById(R.id.iv_index);
            holder.tvName = (TextView) convertView.findViewById(R.id.tv_name_la);
            holder.tvCount = (TextView) convertView.findViewById(R.id.tv_count_la);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.bindData(videoDirectories.get(position), position);

        return convertView;
    }


    private class ViewHolder {
        ImageView ivAlbum;
        ImageView ivIndex;
        TextView tvName;
        TextView tvCount;

        public void bindData(VideoDirectory directory, int position) {
            if (context instanceof Activity && ((Activity) context).isFinishing()) {
                return;
            }


            PickerUtils.loadImage(context,ivAlbum, "file://" + directory.getCoverPath());

            tvName.setText(directory.getName());
            tvCount.setText(context.getString(R.string.image_count, directory.getVideos().size()));
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
