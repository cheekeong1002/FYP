package com.example.fypmock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class AddPoiAdapter extends FragmentPagerAdapter {

    private final ArrayList<Fragment> fragArrayList = new ArrayList<>();
    private final ArrayList<String> fragTitle = new ArrayList<>();

    public AddPoiAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragArrayList.get(position);
    }

    @Override
    public int getCount() {
        return fragArrayList.size();
    }

    public void addFragment(Fragment fragment, String title){
        fragArrayList.add(fragment);
        fragTitle.add(title);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return fragTitle.get(position);
    }
}
