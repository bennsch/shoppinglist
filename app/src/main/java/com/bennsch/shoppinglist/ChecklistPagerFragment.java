package com.bennsch.shoppinglist;

import android.content.Context;
import android.os.Bundle;


import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.bennsch.shoppinglist.databinding.FragmentChecklistPagerBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

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
    private OnBackPressedCallback mOnBackPressedCallback;
    private IMEHelper mIMEHelper;


    public ChecklistPagerFragment() {
        Log.d(TAG, "ChecklistPagerFragment: Ctor");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: " + mListTitle);
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        mListTitle = getArguments().getString(ARG_LIST_TITLE);
        mViewModel = new ViewModelProvider(this).get(AppViewModel.class);
        mViewPagerAdapter = new ViewPagerAdapter(this);
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
        setupItemNameBox(requireActivity(), requireContext(), this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static Bundle makeArgs(String listTitle) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_LIST_TITLE, listTitle);
        return bundle;
    }

    // TODO: Put all ItemNameBox related stuff into separate class.
    //  Or extend custom EditText class
    private void setupItemNameBox(@NonNull ComponentActivity activity,
                                  @NonNull Context context,
                                  @NonNull LifecycleOwner lifecycleOwner) {
        mIMEHelper = new IMEHelper(context);
        mIMEHelper.setOnIMEToggledListener(mBinding.itemNameBox, this::onIMEToggled);
        // TODO: animation looks weird. Wrap constrainedlayout in another layout?
//        mIMEHelper.enableIMETransitionAnimation(mBinding.getRoot());

        mOnBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                onBackPressed();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(lifecycleOwner, mOnBackPressedCallback);

        int itemActionId = EditorInfo.IME_ACTION_DONE;
        mBinding.itemNameBox.setImeOptions(itemActionId);
        mBinding.itemNameBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == itemActionId) {
                onFabClicked();
                return true;
            } else {
                return false;
            }
        });
    }

    private void showItemNameBox(boolean show) {
        if (show) {
            mBinding.itemNameBox.setVisibility(View.VISIBLE);
            mOnBackPressedCallback.setEnabled(true);
            mBinding.itemNameBox.requestFocus();
            mIMEHelper.showIME(mBinding.itemNameBox, true);
        } else {
            mBinding.itemNameBox.clearFocus();
            mBinding.itemNameBox.setText("");
            mBinding.itemNameBox.setVisibility(View.GONE);
            mOnBackPressedCallback.setEnabled(false);
        }
    }

    private boolean isItemNameBoxVisible() {
        return mBinding.itemNameBox.getVisibility() == View.VISIBLE;
    }

    private void onBackPressed() {
        showItemNameBox(false);
    }

    private void onIMEToggled(View view, boolean imeVisible, int imeHeight) {
        if (imeVisible) {
            scrollCurrentChecklist();
        } else {
            showItemNameBox(false);
        }
    }

    private void onFabClicked() {
        // TODO: Make "Return" button on IME behave like FAB
        // TODO: fix landscape layout for FAB and EditText
        if (isItemNameBoxVisible()) {
            insertNewItem();
        } else {
            showItemNameBox(true);
        }
    }

    private void scrollCurrentChecklist() {
        // TODO: don't use viewmodel. retrieve from global settings directly?
        ChecklistFragment currentFragment = mViewPagerAdapter.getFragment(mBinding.viewpager.getCurrentItem());
        if (currentFragment != null) {
            currentFragment.scrollTo(mViewModel.isNewItemInsertBottom());
        } else {
            Log.w(TAG, "scrollCurrentChecklist: currentFragment==null");
        }

    }

    private void insertNewItem() {
        boolean currentChecked = mViewPagerAdapter.isPageCheckedList(mBinding.viewpager.getCurrentItem());
        String itemName = mBinding.itemNameBox.getText().toString();
        ListenableFuture<Void> result = mViewModel.insertItem(
                mListTitle,
                new ChecklistItem(itemName, currentChecked));
        Futures.addCallback(result, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                mBinding.itemNameBox.setText("");
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                vibrate();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void vibrate() {
        Vibrator vibrator = requireContext().getSystemService(Vibrator.class);
        vibrator.vibrate(125);
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

        public boolean isPageCheckedList(int position) {
            return PAGE_MAP.get(position);
        }

        @Nullable
        public ChecklistFragment getFragment(int position) {
            return mCachedFragments[position];
        }

        @Override
        public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
            Log.d(TAG, "onBindViewHolder: " + position);
            super.onBindViewHolder(holder, position, payloads);
        }
    }
}
