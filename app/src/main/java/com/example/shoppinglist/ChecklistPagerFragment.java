package com.example.shoppinglist;

import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.shoppinglist.databinding.FragmentChecklistPagerBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Calendar;
import java.util.Map;


public class ChecklistPagerFragment extends Fragment {

    private static final String TAG = "ChecklistPagerFragment";
    private static final int OFFSCREEN_PAGE_LIMIT = 1;
    public static final String ARG_LIST_TITLE = "list_title";

    private FragmentChecklistPagerBinding mBinding;
    private ViewPagerAdapter mViewPagerAdapter;
    private ChecklistViewModel mViewModel;
    private String mListTitle;


    public ChecklistPagerFragment() {
        Log.d(TAG, "ChecklistPagerFragment: Ctor");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        mListTitle = getArguments().getString(ARG_LIST_TITLE);
        mViewModel = new ViewModelProvider(this).get(ChecklistViewModel.class);
        mViewPagerAdapter = new ViewPagerAdapter(this);
        Log.d(TAG, "onCreate: " + mListTitle);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        mBinding = FragmentChecklistPagerBinding.inflate(inflater, container, false);
        mBinding.viewpager.setAdapter(mViewPagerAdapter);
        mBinding.viewpager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
//        mBinding.viewpager.setPageTransformer(new FanTransformer());
        mBinding.fab.setOnClickListener(view -> this.onFabClicked());
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated: ");
        super.onViewCreated(view, savedInstanceState);
        new TabLayoutMediator(mBinding.tablayout, mBinding.viewpager,
                (tab, position) -> tab.setText("OBJECT " + (position + 1))
        ).attach();

        new TabLayoutMediator(mBinding.tablayout, mBinding.viewpager,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
//                        tab.setIcon(R.drawable.ic_launcher_foreground);
                    }
                }
        ).attach();
    }

    public static Bundle makeArgs(String listTitle) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_LIST_TITLE, listTitle);
        return bundle;
    }

    void onFabClicked() {
        boolean currentChecked = mViewPagerAdapter.getChecked(mBinding.viewpager.getCurrentItem());
        mViewModel.insertItem(mListTitle, new ChecklistItem("Item " + Calendar.getInstance().get(Calendar.MILLISECOND), currentChecked));
    }


    private class ViewPagerAdapter extends FragmentStateAdapter {

        private static final String TAG = "PagerAdapter";

        private final ChecklistFragment[] mCachedFragments = new ChecklistFragment[2];
        private final Map<Integer, Boolean> PAGE_MAP = Map.of(
                0, false,
                1, true
        );

        // Use this constructor if ViewPager2 lives directly in a Fragment.
        // Otherwise the child fragments of ViewPager2 will not be destroyed
        // when this fragment is being destroyed.
        public ViewPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "createFragment " + position);
            mCachedFragments[position] = ChecklistFragment.newInstance(mListTitle, PAGE_MAP.get(position));
            return mCachedFragments[position];
        }

        @Override
        public int getItemCount() {
            return mCachedFragments.length;
        }

        public boolean getChecked(int position) {
            return PAGE_MAP.get(position);
        }
//    public ListItem.Designation getDesignation(int position) {
//        if (position == 0) {
//            return ListItem.Designation.OPEN;
//        } else {
//            return ListItem.Designation.CLOSED;
//        }
//    }
    }
}
