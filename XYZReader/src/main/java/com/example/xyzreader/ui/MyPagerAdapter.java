package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.database.Cursor;
import android.support.v13.app.FragmentStatePagerAdapter;

import com.example.xyzreader.data.ArticleLoader;

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    private Cursor mCursor;

    MyPagerAdapter(FragmentManager fm, Cursor cursor) {
        super(fm);
        mCursor = cursor;
    }

    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        if (mCursor == null) {
            return null;
        }
        mCursor.moveToPosition(position);
        return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
    }

    @Override
    public int getCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }
}