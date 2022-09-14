package com.example.fypmock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

public class AddPoi extends AppCompatActivity {

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_poi);

        mTabLayout = findViewById(R.id.tabLayout);
        mViewPager = findViewById(R.id.viewPager);

        mTabLayout.setupWithViewPager(mViewPager);

        AddPoiAdapter vpAdapter = new AddPoiAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        vpAdapter.addFragment(new FavouriteFragment(), "Favourite");
        vpAdapter.addFragment(new AllPoiFragment(), "All POIs");
        mViewPager.setAdapter(vpAdapter);

//        Intent data = new Intent();
//        data.putExtra("myData1", "Data 1 value");
//        // Activity finished ok, return the data
//        setResult(RESULT_OK, data);
//        finish();
    }
}