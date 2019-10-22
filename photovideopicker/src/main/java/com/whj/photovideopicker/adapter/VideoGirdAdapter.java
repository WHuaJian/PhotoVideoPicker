/**
 * created by jiang, 12/3/15
 * Copyright (c) 2015, jyuesong@gmail.com All Rights Reserved.
 * *                #                                                   #
 * #                       _oo0oo_                     #
 * #                      o8888888o                    #
 * #                      88" . "88                    #
 * #                      (| -_- |)                    #
 * #                      0\  =  /0                    #
 * #                    ___/`---'\___                  #
 * #                  .' \\|     |# '.                 #
 * #                 / \\|||  :  |||# \                #
 * #                / _||||| -:- |||||- \              #
 * #               |   | \\\  -  #/ |   |              #
 * #               | \_|  ''\---/''  |_/ |             #
 * #               \  .-\__  '-'  ___/-. /             #
 * #             ___'. .'  /--.--\  `. .'___           #
 * #          ."" '<  `.___\_<|>_/___.' >' "".         #
 * #         | | :  `- \`.;`\ _ /`;.`/ - ` : | |       #
 * #         \  \ `_.   \_ __\ /__ _/   .-` /  /       #
 * #     =====`-.____`.___ \_____/___.-`___.-'=====    #
 * #                       `=---='                     #
 * #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #
 * #                                                   #
 * #               佛祖保佑         永无BUG              #
 * #                                                   #
 */

package com.whj.photovideopicker.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.facebook.drawee.view.SimpleDraweeView;
import com.whj.photovideopicker.R;
import com.whj.photovideopicker.listener.OnItemCheckListener;
import com.whj.photovideopicker.model.Video;
import com.whj.photovideopicker.model.VideoDirectory;
import com.whj.photovideopicker.utils.PickerUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by jiang on 2/19/16.
 */
public class VideoGirdAdapter extends RecyclerView.Adapter<VideoGirdAdapter.VideoHolder> {


    public final static int ITEM_TYPE_CAMERA = 100;
    public final static int ITEM_TYPE_PHOTO = 101;

    private OnItemCheckListener onItemCheckListener;

    public int currentDirectoryIndex = 0;

    private List<VideoDirectory> directories;
    private Context mContext;
    private List<Video> selectedVideos = new ArrayList<>();
    private View.OnClickListener onCameraClickListener = null;

    public void setOnCameraClickListener(View.OnClickListener onCameraClickListener) {
        this.onCameraClickListener = onCameraClickListener;
    }


    public VideoGirdAdapter(Context context, List<VideoDirectory> directories) {
        mContext = context;
        this.directories = directories;
    }


    @Override
    public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_photoselector, parent, false);
        VideoHolder holder = new VideoHolder(itemView);

        if (viewType == ITEM_TYPE_CAMERA) {
            holder.vSelected.setVisibility(View.GONE);
            holder.ivPhoto.setScaleType(ImageView.ScaleType.CENTER);
            holder.ivPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onCameraClickListener != null) {
                        onCameraClickListener.onClick(view);
                    }
                }
            });
        }

        return holder;

    }

    @Override
    public void onBindViewHolder(final VideoHolder holder, final int position) {
        if (getItemViewType(position) == ITEM_TYPE_PHOTO) {
            List<Video> videos = getCurrentVideos();
            final Video video = videos.get(position - 1);


            PickerUtils.showThumb(holder.ivPhoto, "file://" + video.getPath());

            if (video.getDuration() <= 0) {
                holder.tvVideoTime.setVisibility(View.GONE);
            } else {
                holder.tvVideoTime.setVisibility(View.VISIBLE);
                holder.tvVideoTime.setText(PickerUtils.transferTimers(video.getDuration()/1000));
            }


            final boolean isChecked = isSelected(video);

            holder.vSelected.setSelected(isChecked);
            holder.ivPhoto.setSelected(isChecked);

            holder.vRoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    boolean isEnable = true;

                    if (onItemCheckListener != null) {
                        isEnable = onItemCheckListener.OnItemCheck(position, video, isChecked,
                                getselectedVideos().size());
                    }
                    if (isEnable) {
                        toggleSelection(video);
                        notifyItemChanged(position);
                    }
                }
            });
        } else {
            holder.ivPhoto.setImageResource(R.drawable.video);
        }
    }

    @Override
    public int getItemCount() {
        return directories.size() == 0 ? 1 : getCurrentVideos().size() + 1;
    }

    protected int getLayoutID() {
        return R.layout.item_photoselector;
    }

    @Override
    public int getItemViewType(int position) {
//        return getLayoutID();
        return (position == 0) ? ITEM_TYPE_CAMERA : ITEM_TYPE_PHOTO;
    }

    public void setCurrentDirectoryIndex(int currentDirectoryIndex) {
        this.currentDirectoryIndex = currentDirectoryIndex;
    }

    public List<Video> getCurrentVideos() {
        return directories.get(currentDirectoryIndex).getVideos();
    }

    public void setOnItemCheckListener(OnItemCheckListener onItemCheckListener) {
        this.onItemCheckListener = onItemCheckListener;
    }

    public static class VideoHolder extends RecyclerView.ViewHolder {
        private SimpleDraweeView ivPhoto;
        private ImageView vSelected;
        private RelativeLayout vRoot;
        private TextView tvVideoTime;

        public VideoHolder(View itemView) {
            super(itemView);
            ivPhoto = (SimpleDraweeView) itemView.findViewById(R.id.iv_photo);
            vSelected = (ImageView) itemView.findViewById(R.id.cb_photo);
            vRoot = (RelativeLayout) itemView.findViewById(R.id.iv_root);
            tvVideoTime = itemView.findViewById(R.id.tvVideoTime);
        }
    }

    public boolean isSelected(Video video) {
        return getselectedVideos().contains(video);
    }

    public List<Video> getselectedVideos() {
        return selectedVideos;
    }

    public void toggleSelection(Video video) {
        if (selectedVideos.contains(video)) {
            selectedVideos.remove(video);
        } else {
            selectedVideos.add(video);
        }
    }
}
