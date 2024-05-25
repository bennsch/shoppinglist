package com.example.shoppinglist;

import android.content.Context;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.shoppinglist.databinding.FragmentChecklistPagerBinding;
import com.example.shoppinglist.viewmodel.AppViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Calendar;
import java.util.List;
import java.util.Map;


public class ChecklistPagerFragment extends Fragment {

    private static final String TAG = "ChecklistPagerFragment";
    private static final int OFFSCREEN_PAGE_LIMIT = 1;
    public static final String ARG_LIST_TITLE = "list_title";

    private FragmentChecklistPagerBinding mBinding;
    private ViewPagerAdapter mViewPagerAdapter;
    private AppViewModel mViewModel;
    private String mListTitle;


    public ChecklistPagerFragment() {
        Log.d(TAG, "ChecklistPagerFragment: Ctor");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        mListTitle = getArguments().getString(ARG_LIST_TITLE);
        mViewModel = new ViewModelProvider(this).get(AppViewModel.class);
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
//        mViewModel.getAllItemsSorted().observe(getViewLifecycleOwner(), this::onItemsChanged);
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

    private void onFabClicked() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mBinding.edittext.getVisibility() == View.VISIBLE) {
            imm.hideSoftInputFromWindow(mBinding.edittext.getWindowToken(), 0);
            mBinding.edittext.setVisibility(View.GONE);
            insertNewItem();
        } else {
            mBinding.edittext.setVisibility(View.VISIBLE);
            mBinding.edittext.requestFocus();
            imm.showSoftInput(mBinding.edittext, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void insertNewItem() {
        boolean currentChecked = mViewPagerAdapter.getChecked(mBinding.viewpager.getCurrentItem());
        ListenableFuture<Void> result = mViewModel.insertItem(
                mListTitle,
                new ChecklistItem("Item " + Calendar.getInstance().get(Calendar.SECOND), currentChecked));
        Futures.addCallback(result, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {

            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                vibrate();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void vibrate() {
        Vibrator vibrator = getContext().getSystemService(Vibrator.class);
        vibrator.vibrate(250);
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

        @Override
        public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
            super.onBindViewHolder(holder, position, payloads);
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
