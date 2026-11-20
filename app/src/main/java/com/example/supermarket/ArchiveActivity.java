package com.example.supermarket;

import android.os.Bundle;
import android.widget.ImageView; // تم إضافة الاستيراد الخاص بـ ImageView
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ArchiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        // ربط زر الرجوع في الترويسة وإغلاق الشاشة عند النقر عليه
        ImageView btnBack = findViewById(R.id.btnBackArchive);
        btnBack.setOnClickListener(v -> finish());

        TabLayout tabLayout = findViewById(R.id.tabLayoutArchive);
        ViewPager2 viewPager = findViewById(R.id.viewPagerArchive);

        ArchivePagerAdapter pagerAdapter = new ArchivePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("الزبائن المؤرشفون");
            } else {
                tab.setText("الحركات المؤرشفة");
            }
        }).attach();
    }

    private static class ArchivePagerAdapter extends FragmentStateAdapter {
        public ArchivePagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new ArchivedCustomersFragment();
            } else {
                return new ArchivedTransactionsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}