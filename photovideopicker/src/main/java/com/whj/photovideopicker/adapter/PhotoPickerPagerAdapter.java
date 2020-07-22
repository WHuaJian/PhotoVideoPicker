package com.whj.photovideopicker.adapter;

import android.annotation.SuppressLint;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import android.view.ViewGroup;

import java.util.List;


/**
 * 解决viewpager刷新问题
 * 在平滑中使用缓存视图
 *
 * @author William
 * @Github WHuaJian
 * Created at 2018/4/24 上午11:26
 */

public abstract class PhotoPickerPagerAdapter extends FragmentStatePagerAdapter {

    private int mChildCount = 0;
    private ViewPager viewPager;
    private FragmentManager fm;

    public PhotoPickerPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public PhotoPickerPagerAdapter(FragmentManager fm, ViewPager viewPager) {
        super(fm);
        this.viewPager = viewPager;
        this.fm = fm;
    }

    @Override
    public Fragment getItem(int position) {
        return getItemFragment(position);
    }

    @Override
    public int getCount() {
        return getFragmentSize();
    }

    @Override
    public int getItemPosition(Object object) {
        if (mChildCount > 0) {
            mChildCount--;
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return setTabTitle(position);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        return super.instantiateItem(container, position);
    }

    @Override
    public void notifyDataSetChanged() {
        mChildCount = getFragmentSize();
        removeLastFragments();
        super.notifyDataSetChanged();
    }

    public abstract CharSequence setTabTitle(int position);

    public abstract int getFragmentSize();

    public abstract Fragment getItemFragment(int position);


    /**
     * 在notifyDataSetChanged之前对FragmentManager进行相应的删除
     */
    private void removeLastFragments() {
        if (viewPager.getAdapter() != null) {
            FragmentTransaction ft = fm.beginTransaction();
            @SuppressLint("RestrictedApi") List<Fragment> fragments = fm.getFragments();
            if (fragments != null && fragments.size() > 0) {
                for (int i = 0; i < fragments.size(); i++) {
                    ft.remove(fragments.get(i));
                }
            }
            ft.commit();
        }
    }
}
