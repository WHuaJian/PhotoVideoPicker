package com.whj.photovideopicker.listener;


import com.whj.photovideopicker.model.Photo;
import com.whj.photovideopicker.model.Video;

/**
 * Created by user.
 */
public interface OnItemCheckListener {

  /***
   *
   * @param position 所选图片的位置
   * @param path     所选的图片
   *@param isCheck   当前状态
   * @param selectedItemCount  已选数量
   * @return enable check
   */
  boolean OnItemCheck(int position, Photo path, boolean isCheck, int selectedItemCount);

  boolean OnItemCheck(int position, Video video, boolean isCheck, int selectedItemCount);
}
