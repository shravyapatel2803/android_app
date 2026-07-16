package com.example.billgenerator.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.billgenerator.fragments.BillHistoryFragment;
import com.example.billgenerator.fragments.CustomerDetailsFragment;
import com.example.billgenerator.fragments.DashboardFragment;
import com.example.billgenerator.fragments.CollectionModeFragment;
import com.example.billgenerator.fragments.GenerateBillFragment;
import com.example.billgenerator.fragments.NotificationHistoryFragment;
import com.example.billgenerator.fragments.ShopProfileFragment;
import com.example.billgenerator.fragments.StatsFragment;
import com.example.billgenerator.fragments.DebtCustomersFragment;
import com.example.billgenerator.fragments.SupplierManagementFragment;
import com.example.billgenerator.fragments.StockManagementFragment;
import com.example.billgenerator.fragments.WorkerLedgerFragment;


import com.example.billgenerator.fragments.SettingsFragment;


public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new DashboardFragment();
            case 1: return new GenerateBillFragment();
            case 2: return new BillHistoryFragment();
            case 3: return new CustomerDetailsFragment();
            case 4: return new StockManagementFragment();
            case 5: return new NotificationHistoryFragment();
            case 6: return new StatsFragment();
            case 7: return new DebtCustomersFragment();
            case 8: return new CollectionModeFragment();
            case 9: return new ShopProfileFragment();
            case 10: return new SupplierManagementFragment();
            case 11: return new WorkerLedgerFragment();
            case 12: return new SettingsFragment();
            default: return new DashboardFragment();
        }
    }

    @Override
    public int getItemCount() { return 13; }
}
