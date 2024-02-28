package com.example.shoppinglist;

import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.shoppinglist.databinding.FragmentChecklistPagerBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Map;


public class ChecklistPagerFragment extends Fragment {

    private static final int OFFSCREEN_PAGE_LIMIT = 1;

    private FragmentChecklistPagerBinding mBinding;
    private ViewPagerAdapter mViewPagerAdapter;

    public ChecklistPagerFragment() {
        // Required empty public constructor
    }

    public static ChecklistPagerFragment newInstance() {
        ChecklistPagerFragment fragment = new ChecklistPagerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mBinding = FragmentChecklistPagerBinding.inflate(inflater, container, false);
        mViewPagerAdapter = new ViewPagerAdapter(getActivity());
        mBinding.viewpager.setAdapter(mViewPagerAdapter);
        mBinding.viewpager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
//        mBinding.viewpager.setPageTransformer(new FanTransformer());
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
}


class ViewPagerAdapter extends FragmentStateAdapter {

    private static final String TAG = "PagerAdapter";

    private final ChecklistFragment[] mCachedFragments = new ChecklistFragment[2];
    private static final Map<Integer, Boolean> mPageMap = Map.of(
            0, false,
            1, true
    );

    public ViewPagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d(TAG, "createFragment " + position);
        mCachedFragments[position] = ChecklistFragment.newInstance(mPageMap.get(position));
        return mCachedFragments[position];
    }

    @Override
    public int getItemCount() {
        return mCachedFragments.length;
    }

//    public ListItem.Designation getDesignation(int position) {
//        if (position == 0) {
//            return ListItem.Designation.OPEN;
//        } else {
//            return ListItem.Designation.CLOSED;
//        }
//    }
}