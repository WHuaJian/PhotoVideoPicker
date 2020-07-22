package com.whj.photovideopicker.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import com.facebook.drawee.view.SimpleDraweeView;
import com.whj.photovideopicker.R;
import com.whj.photovideopicker.listener.OnItemCheckListener;
import com.whj.photovideopicker.listener.OnPhotoClickListener;
import com.whj.photovideopicker.model.Photo;
import com.whj.photovideopicker.model.PhotoDirectory;
import com.whj.photovideopicker.utils.MediaStoreHelper;
import com.whj.photovideopicker.utils.PickerUtils;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by user .
 */
public class PhotoGridAdapter extends SelectableAdapter<PhotoGridAdapter.PhotoViewHolder> {

    private LayoutInflater inflater;

    private Context mContext;

    private OnItemCheckListener onItemCheckListener = null;
    private OnPhotoClickListener onPhotoClickListener = null;
    private View.OnClickListener onCameraClickListener = null;

    public final static int ITEM_TYPE_CAMERA = 100;
    public final static int ITEM_TYPE_PHOTO = 101;

    private boolean hasCamera = true;

    public PhotoGridAdapter(Context mContext, List<PhotoDirectory> photoDirectories) {
        this.photoDirectories = photoDirectories;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
    }


    @Override
    public int getItemViewType(int position) {
        return (showCamera() && position == 0) ? ITEM_TYPE_CAMERA : ITEM_TYPE_PHOTO;
    }


    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.item_photoselector, parent, false);
        PhotoViewHolder holder = new PhotoViewHolder(itemView);
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
    public void onBindViewHolder(final PhotoViewHolder holder, final int position) {

        if (getItemViewType(position) == ITEM_TYPE_PHOTO) {

            List<Photo> photos = getCurrentPhotos();
            final Photo photo;

            if (showCamera()) {
                photo = photos.get(position - 1);
            } else {
                photo = photos.get(position);
            }
//            Uri uri = Uri.parse("file://" + photo.getPath());
//            ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
//                    .build();
//
//            ImageRequest request = ImageRequestBuilder
//                    .newBuilderWithSource(uri)
//                    .setImageDecodeOptions(decodeOptions)
//                    .setAutoRotateEnabled(true)
////                    .setImageDecodeOptions(ImageDecodeOptions.newBuilder().setDecodeAllFrames(true).build())
////                    .setImageType(ImageRequest.ImageType.SMALL)TODO... 待修改
//                    .setResizeOptions(new ResizeOptions(holder.ivPhoto.getLayoutParams().width, holder.ivPhoto.getLayoutParams().height))
//                    .build();
//
//            DraweeController controller = Fresco.newDraweeControllerBuilder()
//                    .setOldController(holder.ivPhoto.getController())
//                    .setImageRequest(request)
//                    .build();
//
//            holder.ivPhoto.setController(controller);

            PickerUtils.showThumb(holder.ivPhoto, "file://" + photo.getPath());


            final boolean isChecked = isSelected(photo);

            holder.vSelected.setSelected(isChecked);
            holder.ivPhoto.setSelected(isChecked);

//            holder.ivPhoto.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (onPhotoClickListener != null) {
//                        onPhotoClickListener.onClick(view, position, showCamera());
//                    }
//                }
//            });
            holder.vRoot.setClickable(true);
            holder.vRoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    boolean isEnable = true;

                    if (onItemCheckListener != null) {
                        isEnable = onItemCheckListener.OnItemCheck(position, photo, isChecked,
                                getSelectedPhotos().size());
                    }
                    if (isEnable) {
                        toggleSelection(photo);
                        notifyItemChanged(position);
                    }
                }
            });

        } else {
            holder.ivPhoto.setImageResource(R.drawable.camera);
        }
    }


    @Override
    public int getItemCount() {
        int photosCount =
                photoDirectories.size() == 0 ? 0 : getCurrentPhotos().size();
        if (showCamera()) {
            return photosCount + 1;
        }
        return photosCount;
    }


    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private SimpleDraweeView ivPhoto;
        private ImageView vSelected;
        private RelativeLayout vRoot;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            ivPhoto = (SimpleDraweeView) itemView.findViewById(R.id.iv_photo);
            vSelected = (ImageView) itemView.findViewById(R.id.cb_photo);
            vRoot = (RelativeLayout) itemView.findViewById(R.id.iv_root);
        }
    }


    public void setOnItemCheckListener(OnItemCheckListener onItemCheckListener) {
        this.onItemCheckListener = onItemCheckListener;
    }


//    public void setOnPhotoClickListener(OnPhotoClickListener onPhotoClickListener) {
//        this.onPhotoClickListener = onPhotoClickListener;
//    }


    public void setOnCameraClickListener(View.OnClickListener onCameraClickListener) {
        this.onCameraClickListener = onCameraClickListener;
    }


    public ArrayList<String> getSelectedPhotoPaths() {
        ArrayList<String> selectedPhotoPaths = new ArrayList<>(getSelectedItemCount());

        for (Photo photo : selectedPhotos) {
            selectedPhotoPaths.add(photo.getPath());
        }

        return selectedPhotoPaths;
    }


    public void setShowCamera(boolean hasCamera) {
        this.hasCamera = hasCamera;
    }


    public boolean showCamera() {
        return (hasCamera && currentDirectoryIndex == MediaStoreHelper.INDEX_ALL_PHOTOS);
    }
}
