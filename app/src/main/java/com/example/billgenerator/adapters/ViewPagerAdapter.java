package com.example.billgenerator.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.billgenerator.fragments.BillHistoryFragment;
import com.example.billgenerator.fragments.CustomerDetailsFragment;
import com.example.billgenerator.fragments.StockManagementFragment;
import com.google.android.material.tabs.GenerateBillFragment;


public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new BillHistoryFragment();
            case 2:
                return new CustomerDetailsFragment();
            case 3:
                return new StockManagementFragment();
            case 0:
            default:
                return new GenerateBillFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // Total number of tabs
    }
}