package com.whj.photovideopicker.utils;

import android.content.Context;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * 绘制recyclerview grid 上下左右间距相同
 * @author William
 * @Github WHuaJian
 * Created at 2018/7/5 下午8:02
 */

public class PhotoGridAutofitDecoration extends RecyclerView.ItemDecoration {

    private int spanCount; //每行个数
    private int spacing; //上下左右间距

    public PhotoGridAutofitDecoration(int spanCount, int spacing) {
        this.spanCount = spanCount;
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        // top bottom left right 对应的值应该是dpi 而不是dp  dpi根据不同手机而不同
        int i = parent.getChildLayoutPosition(view) % spanCount;
        if (i == 0) {
            outRect.left = convertDpToPixel(view.getContext(), spacing);
            outRect.top = convertDpToPixel(view.getContext(), spacing);
            outRect.right = convertDpToPixel(view.getContext(), spacing);
            outRect.bottom = convertDpToPixel(view.getContext(), 0);
        } else if (i == spanCount - 1) {
            outRect.left = convertDpToPixel(view.getContext(), 0);
            outRect.top = convertDpToPixel(view.getContext(), spacing);
            outRect.right = convertDpToPixel(view.getContext(), spacing);
            outRect.bottom = convertDpToPixel(view.getContext(), 0);
        } else {
            outRect.left = convertDpToPixel(view.getContext(), 0);
            outRect.top = convertDpToPixel(view.getContext(), spacing);
            outRect.right = convertDpToPixel(view.getContext(), spacing);
            outRect.bottom = convertDpToPixel(view.getContext(), 0);
        }
    }

    private int convertDpToPixel(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (dp * displayMetrics.density);
    }
}
