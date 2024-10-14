package com.bennsch.shoppinglist;

import android.content.Context;
import android.graphics.Outline;
import android.os.Bundle;


import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;

import com.bennsch.shoppinglist.databinding.FragmentChecklistPagerBinding;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;


public class ChecklistPagerFragment extends Fragment {

    private static final String TAG = "ChecklistPagerFragment";
    private static final int OFFSCREEN_PAGE_LIMIT = 1;
    public static final String ARG_LIST_TITLE = "list_title";

    private FragmentChecklistPagerBinding mBinding;
    private ViewPagerAdapter mViewPagerAdapter;
    private MainViewModel mViewModel;
    private String mListTitle;
    private OnBackPressedCallback mOnBackPressedCallback;
    private IMEHelper mIMEHelper;
    private LiveData<Boolean> mIsDeleteItemsActive;


    public ChecklistPagerFragment() {
        Log.d(TAG, "ChecklistPagerFragment: Ctor");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: " + mListTitle);
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        mListTitle = getArguments().getString(ARG_LIST_TITLE);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
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

        mIsDeleteItemsActive = mViewModel.getDeleteItemsActive();

        mViewModel.isChecklistEmpty(mListTitle)
                .observe(getViewLifecycleOwner(), this::onChecklistEmptyChanged);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated: ");
        super.onViewCreated(view, savedInstanceState);
        new TabLayoutMediator(
                mBinding.tablayout,
                mBinding.viewpager,
                (tab, position) -> {})
                .attach();
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

        mBinding.itemNameBox.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        // The outline provider determines where to draw the shadow (elevation).
        // A custom outline is required to allow rounded corners.
        mBinding.itemNameBox.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // Since the item_name_box has no margin to the left of the screen
                // we need to define the start of the outline "outside" of the screen
                // (negative value) so that the shadow of rounded corners on the left
                // is not visible.
                outline.setRoundRect(
                        -getResources().getDimensionPixelSize(R.dimen.fab_radius),
                        0,
                        view.getWidth(),
                        view.getHeight(),
                        getResources().getDimensionPixelSize(R.dimen.fab_radius));
            }
        });

        ArrayAdapter<String> autoComplAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line);

        // The content of the auto complete popup depends on which
        // page is currently visible (checked or unchecked).
        // Whenever isCurrentPageChecked changes its value, Transformations.switchMap()
        // will replace autoCompleteItems so that the observer is be triggered with
        // the correct data.
        MutableLiveData<Boolean> isCurrentPageChecked = new MutableLiveData<>(null);
        LiveData<List<String>> autoCompleteItems = Transformations.switchMap(
                isCurrentPageChecked,
                currPageChecked ->
                        mViewModel.getAutoCompleteItems(mListTitle, currPageChecked));
        autoCompleteItems.observe(
                getViewLifecycleOwner(),
                strings -> {
                    autoComplAdapter.clear();
                    autoComplAdapter.addAll(strings);
                });
        // Update isCurrentPageChecked whenever the user changes the page.
        mBinding.viewpager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected: " + position);
                super.onPageSelected(position);
                mBinding.itemNameBox.dismissDropDown();
                isCurrentPageChecked.setValue(isCurrentPageChecked()); // TODO: or postValue()?
            }
        });

        mBinding.itemNameBox.setOnItemClickListener((parent, view, position, id) -> {
            insertNewItem((String) parent.getItemAtPosition(position));
            // TODO: Race condition (will sometimes not scroll to last item).
            //  The scroll should be triggered after the list has been updated
            scrollCurrentChecklist();
        });
        mBinding.itemNameBox.setAdapter(autoComplAdapter);
        mBinding.itemNameBox.setThreshold(MainViewModel.AUTOCOMPLETE_THRESHOLD);
    }

    private void toggleItemNameBox(boolean show) {
        if (show) {
            mOnBackPressedCallback.setEnabled(true);
            mBinding.itemNameBox.setVisibility(View.VISIBLE);
            // Set constraint to expand to top of item_name_box.
            updateConstraint(
                    mBinding.checklistPagerRoot,
                    R.id.viewpager, ConstraintSet.BOTTOM, R.id.item_name_box, ConstraintSet.TOP);
            mBinding.itemNameBox.requestFocus();
            mIMEHelper.showIME(mBinding.itemNameBox, true);
        } else {
            mOnBackPressedCallback.setEnabled(false);
            mBinding.itemNameBox.clearFocus();
            mBinding.itemNameBox.setText("");
            mBinding.itemNameBox.setVisibility(View.GONE);
            // Set constraint to expand to bottom of parent.
            updateConstraint(
                    mBinding.checklistPagerRoot,
                    R.id.viewpager, ConstraintSet.BOTTOM, R.id.checklist_pager_root, ConstraintSet.BOTTOM);
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
        // TODO: fix landscape layout for FAB and EditText
        if (isItemNameBoxVisible()) {
            insertNewItem(mBinding.itemNameBox.getText().toString());
        } else {
            toggleItemNameBox(true);
        }
    }

    private void onChecklistEmptyChanged(boolean empty) {
        if (empty) {
            mBinding.emptyListPlaceholderBoth.setVisibility(View.VISIBLE);
            // Need to hide the ViewPager, because it's possible to change
            // the current page by swiping, even if the placeholder is visible.
            mBinding.viewpager.setVisibility(View.GONE);
            // Change ViewPager page so that the first item will be added to "Unchecked".
            mBinding.viewpager.setCurrentItem(ViewPagerAdapter.POS_UNCHECKED);
            Log.d(TAG, "onChecklistEmptyChanged: " + isCurrentPageChecked());
            // TODO: This has to be done in ViewModel!
            if (mIsDeleteItemsActive.getValue() != null && mIsDeleteItemsActive.getValue()) {
                mViewModel.toggleDeleteItemsActive();
            }
        } else {
            mBinding.emptyListPlaceholderBoth.setVisibility(View.GONE);
            mBinding.viewpager.setVisibility(View.VISIBLE);
        }
    }

    private void scrollCurrentChecklist() {
        // retrieve from global settings?
        ChecklistFragment fragment = mViewPagerAdapter.getFragment(
                mBinding.viewpager.getCurrentItem());
        if (fragment != null) {
            fragment.scrollTo(true);
        } else {
            Log.e(TAG, "scrollCurrentChecklist: current fragment == null");
        }
    }

    private void insertNewItem(@NonNull String name) {
        Boolean currentChecked = isCurrentPageChecked();
        assert currentChecked != null;
        ListenableFuture<Void> result = mViewModel.insertItem(
                mListTitle,
                currentChecked,
                name);
        Futures.addCallback(result, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                mBinding.itemNameBox.setText("");
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
//                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                vibrate();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Nullable Boolean isCurrentPageChecked() {
        return mViewPagerAdapter.isPageChecked(
                mBinding.viewpager.getCurrentItem());
    }

    private void vibrate() {
        Vibrator vibrator = requireContext().getSystemService(Vibrator.class);
        vibrator.vibrate(125);
    }

    private static void updateConstraint(ConstraintLayout parent, int startID, int startSide, int endID, int endSide) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parent);
        constraintSet.connect(startID, startSide, endID, endSide);
        constraintSet.applyTo(parent);
    }


    private class ViewPagerAdapter extends FragmentStateAdapter {

        // TODO: Combine ViewPagerAdapter and Viewpager (since they depend on each other)?
        //  (and TabLayoutMediator as well)

        public static final int POS_UNCHECKED = 0;
        public static final int POS_CHECKED = 1;

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
            assert (position == POS_CHECKED) || (position == POS_UNCHECKED);
            ChecklistFragment fragment = ChecklistFragment.newInstance(
                    mListTitle,
                    position == POS_CHECKED);
            mCachedFragments[position] = fragment;
            return fragment;
        }

        @Override
        public int getItemCount() {
            return mCachedFragments.length;
        }

        @Nullable
        public ChecklistFragment getFragment(int position) {
            try {
                return mCachedFragments[position];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }

        @Nullable
        public Boolean isPageChecked(int position) {
            if (getFragment(position) == null) {
                return null;
            } else {
                return position == POS_CHECKED;
            }
        }
    }
}
