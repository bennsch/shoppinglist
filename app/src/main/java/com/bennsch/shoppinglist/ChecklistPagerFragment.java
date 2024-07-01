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

    private void toggleItemNameBox(boolean show) {
        if (show) {
            mOnBackPressedCallback.setEnabled(true);
            mBinding.itemNameBox.setVisibility(View.VISIBLE);
            // Set layout_height to 0, so that the constraint will be used (to top of itemNameBox)
            ViewGroup.LayoutParams params = mBinding.viewpager.getLayoutParams();
            params.height = 0;
            mBinding.viewpager.setLayoutParams(params);
            mBinding.itemNameBox.requestFocus();
            mIMEHelper.showIME(mBinding.itemNameBox, true);
        } else {
            mOnBackPressedCallback.setEnabled(false);
            mBinding.itemNameBox.clearFocus();
            mBinding.itemNameBox.setText("");
            mBinding.itemNameBox.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = mBinding.viewpager.getLayoutParams();
            // Set layout_height to a value, so that the constraint (to top of itemNameBox)
            // will be ignored and viewpager expands to bottom of screen
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mBinding.viewpager.setLayoutParams(params);
        }
    }

    private boolean isItemNameBoxVisible() {
        return mBinding.itemNameBox.getVisibility() == View.VISIBLE;
    }

    private void onBackPressed() {
        toggleItemNameBox(false);
    }

    private void onIMEToggled(View view, boolean imeVisible, int imeHeight) {
        if (imeVisible) {
            scrollCurrentChecklist();
        } else {
            toggleItemNameBox(false);
        }
    }

    private void onFabClicked() {
        // TODO: Make "Return" button on IME behave like FAB
        // TODO: fix landscape layout for FAB and EditText
        if (isItemNameBoxVisible()) {
            insertNewItem();
        } else {
            toggleItemNameBox(true);
        }
    }

    private void scrollCurrentChecklist() {
        // TODO: don't use viewmodel. retrieve from global settings directly?
        getCurrentFragment().scrollTo(mViewModel.isNewItemInsertBottom());
    }

    private void insertNewItem() {
        boolean currentChecked = getCurrentFragment().isDisplayChecked();
        String itemName = mBinding.itemNameBox.getText().toString();
        ListenableFuture<Void> result = mViewModel.insertItem(
                mListTitle,
                currentChecked,
                new ChecklistItem(itemName));
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

    private ChecklistFragment getCurrentFragment() {
        return mViewPagerAdapter.getFragment(mBinding.viewpager.getCurrentItem());
    }

    private void vibrate() {
        Vibrator vibrator = requireContext().getSystemService(Vibrator.class);
        vibrator.vibrate(125);
    }


    private class ViewPagerAdapter extends FragmentStateAdapter {

        private final ChecklistFragment[] mCachedFragments = new ChecklistFragment[2];

        // Use this constructor if ViewPager2 lives directly in a Fragment.
        // Otherwise the child fragments of ViewPager2 will not be destroyed
        // when this fragment is being destroyed.
        public ViewPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            boolean displayChecked = position != 0;
            ChecklistFragment fragment = ChecklistFragment.newInstance(mListTitle, displayChecked);
            mCachedFragments[position] = fragment;
            return fragment;
        }

        @Override
        public int getItemCount() {
            return mCachedFragments.length;
        }

        public ChecklistFragment getFragment(int position) {
            return mCachedFragments[position];
        }
    }
}
