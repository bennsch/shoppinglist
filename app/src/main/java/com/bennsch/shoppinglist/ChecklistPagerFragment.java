package com.bennsch.shoppinglist;

import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
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

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
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
    /*
     *  This fragment is the container which contains the FAB, the input
     *  box to create new items and the ViewPager to swipe between the two
     *  ChecklistFragments (checked, unchecked items).
     */

    public static final String ARG_LIST_TITLE = "list_title";

    private static final int OFFSCREEN_PAGE_LIMIT = 1; // Only one page will ever be offscreen.

    private FragmentChecklistPagerBinding mBinding;
    private ViewPagerAdapter mViewPagerAdapter;
    private MainViewModel mViewModel;
    private String mListTitle;
    private OnBackPressedCallback mOnBackPressedCallback;
    private IMEHelper mIMEHelper;
    private OnboardingPopup mOnboardingPopup;


    public ChecklistPagerFragment() {
        // Required empty public constructor.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        mListTitle = getArguments().getString(ARG_LIST_TITLE);
        // Retrieve the MainActivity's ViewModel instance, to communicate and share data between the
        // fragments easily.
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mViewPagerAdapter = new ViewPagerAdapter(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentChecklistPagerBinding.inflate(inflater, container, false);
        mBinding.viewpager.setAdapter(mViewPagerAdapter);
        mBinding.viewpager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
        // mBinding.viewpager.setPageTransformer(new FanTransformer());
        mBinding.fab.setOnClickListener(view -> this.onFabClicked());
        mViewModel.isChecklistEmpty(mListTitle)
                .observe(getViewLifecycleOwner(),
                        this::onChecklistEmptyChanged);
        mViewModel.getDeleteItemsMode()
                .observe(getViewLifecycleOwner(), deleteItemsMode -> {
                    // hide() and show() will animate the transition.
                    if (deleteItemsMode == MainViewModel.DeleteItemsMode.ACTIVATED) {
                        mBinding.fab.hide();
                    } else {
                        mBinding.fab.show();
                    }
        });
        mViewModel.getSimpleOnboarding().getHint().observe(getViewLifecycleOwner(), hint -> {
            switch (hint) {
                case INIT:
                case COMPLETED:
                    mOnboardingPopup.hide();
                    break;
                case TAP_ITEM_TO_CHECK:
                    mOnboardingPopup.show("Tap an item to cross it off the list");
                    break;
                case TAP_ITEM_TO_UNCHECK:
                    mOnboardingPopup.show("Tap a crossed out item to use it again");
                    break;
                case SWIPE_TO_CHECKED:
                    mOnboardingPopup.show(
                            "Swipe the screen to the left to reveal the items that you crossed off");
                    break;
                case SWIPE_TO_UNCHECKED:
                    mOnboardingPopup.show("Swipe the screen to the right to go back");
                    break;
                default:
                    throw new AssertionError("Invalid hint " + hint);
            }
        });
        mViewModel.areItemsDragged().observe(getViewLifecycleOwner(), itemDragged -> {
            if (itemDragged) {
                mBinding.fab.hide();
            } else {
                mBinding.fab.show();
            }
        });
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new TabLayoutMediator(
                mBinding.tablayout,
                mBinding.viewpager,
                (tab, position) -> {})
                .attach();
        setupItemNameBox(requireActivity(), requireContext(), this);
        Context context = getContext();
        assert context != null: "getContext() returned null";
        mOnboardingPopup = new OnboardingPopup(context, mBinding.viewpager);
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

    private void setupItemNameBox(@NonNull ComponentActivity activity,
                                  @NonNull Context context,
                                  @NonNull LifecycleOwner lifecycleOwner) {
        mIMEHelper = new IMEHelper(context);
        mIMEHelper.setOnIMEToggledListener(mBinding.itemNameBox, this::onIMEToggled);
        // TODO: animation looks weird. Maybe wrap ConstrainedLayout in another layout?
        // mIMEHelper.enableIMETransitionAnimation(mBinding.getRoot());

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

        // Setup the text input style:
        mBinding.itemNameBox.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        // The outline provider determines where to draw the shadow (elevation).
        // A custom outline is required to allow rounded corners.
        mBinding.itemNameBox.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // Since the item_name_box has no margin to the left of the screen, we need to
                // define the start of the outline "outside" of the screen (negative value) so that
                // the shadow of rounded corners on the left is not visible.
                outline.setRoundRect(
                        -getResources().getDimensionPixelSize(R.dimen.fab_radius),
                        0,
                        view.getWidth(),
                        view.getHeight(),
                        getResources().getDimensionPixelSize(R.dimen.fab_radius));
            }
        });

        // Scroll to the bottom of the list whenever a new items is added:
        mBinding.itemNameBox.setOnItemClickListener((parent, view, position, id) -> {
            insertNewItem((String) parent.getItemAtPosition(position));
            // TODO: Race condition: The scroll should be triggered after the list has been updated.
            //  (sometimes it won't scroll to the very last item).
            scrollCurrentChecklist();
        });

        // Provide an adapter to enable auto-complete suggestions:
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line
        );
        // Provide different autoCompleteItems, depending on which page is currently displayed.
        MutableLiveData<Boolean> isCurrentPageChecked = new MutableLiveData<>();
        LiveData<List<String>> autoCompleteItems = Transformations.switchMap(
                isCurrentPageChecked,
                currPageChecked -> {
                    if (currPageChecked == null) {
                        return null;
                    } else {
                        return mViewModel.getAutoCompleteDataset(mListTitle, currPageChecked);
                    }
                }
        );
        // Update the autoCompleteAdapter whenever autoCompleteItems changes.
        autoCompleteItems.observe(
                getViewLifecycleOwner(),
                strings -> {
                    autoCompleteAdapter.clear();
                    autoCompleteAdapter.addAll(strings);
                }
        );
        // Update "isCurrentPageChecked" whenever the user changes the page.
        mBinding.viewpager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mBinding.itemNameBox.dismissDropDown();
                Boolean currentAdapterPageChecked = isCurrentAdapterPageChecked();
                // Check if the current page actually changed
                if ((isCurrentPageChecked.getValue() != null) &&
                    (currentAdapterPageChecked != null) &&
                    (isCurrentPageChecked.getValue() != currentAdapterPageChecked)) {
                    mViewModel.getSimpleOnboarding().notify(
                            currentAdapterPageChecked ?
                                    MainViewModel.Onboarding.Event.SWIPED_TO_CHECKED :
                                    MainViewModel.Onboarding.Event.SWIPED_TO_UNCHECKED
                    );
                }
                isCurrentPageChecked.setValue(currentAdapterPageChecked); // TODO: postValue()?
            }
        });
        mBinding.itemNameBox.setAdapter(autoCompleteAdapter);
        mBinding.itemNameBox.setThreshold(MainViewModel.AUTOCOMPLETE_THRESHOLD);
    }

    private void toggleItemNameBox(boolean show) {
        if (show) {
            mOnBackPressedCallback.setEnabled(true);
            mBinding.itemNameBox.setVisibility(View.VISIBLE);
            // Set constraint to expand to top of "item_name_box".
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
        // TODO: Fix landscape layout for FAB and EditText
        if (isItemNameBoxVisible()) {
            insertNewItem(mBinding.itemNameBox.getText().toString());
        } else {
            toggleItemNameBox(true);
        }
    }

    private void onChecklistEmptyChanged(boolean empty) {
        if (empty) {
            mBinding.emptyListPlaceholderBoth.setVisibility(View.VISIBLE);
            // We need to hide the ViewPager, because it's possible to change the current page by
            // swiping, even if the list is empty and the placeholder is visible.
            mBinding.viewpager.setVisibility(View.GONE);
            // Change ViewPager page, so that the first item will be added to "unchecked" items.
            mBinding.viewpager.setCurrentItem(ViewPagerAdapter.POS_UNCHECKED);
            mViewModel.getSimpleOnboarding().notify(MainViewModel.Onboarding.Event.LIST_EMPTY);
        } else {
            mBinding.emptyListPlaceholderBoth.setVisibility(View.GONE);
            mBinding.viewpager.setVisibility(View.VISIBLE);
            mViewModel.getSimpleOnboarding().notify(MainViewModel.Onboarding.Event.LIST_NOT_EMPTY);
        }
    }

    private void scrollCurrentChecklist() {
        ChecklistFragment fragment = mViewPagerAdapter.getFragment(
                mBinding.viewpager.getCurrentItem());
        if (fragment != null) {
            fragment.scrollTo(true);
        }
    }

    private void insertNewItem(@NonNull String name) {
        Boolean currentChecked = isCurrentAdapterPageChecked();
        assert currentChecked != null;
        ListenableFuture<Void> result = mViewModel.insertItem(
                mListTitle,
                currentChecked,
                name);
        Futures.addCallback(
                result,
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Clear the prompt.
                        mBinding.itemNameBox.setText("");
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        vibrate();
                    }
                },
                ContextCompat.getMainExecutor(requireContext()));
    }

    @Nullable Boolean isCurrentAdapterPageChecked() {
        return mViewPagerAdapter.isPageChecked(
                mBinding.viewpager.getCurrentItem());
    }

    @SuppressWarnings("deprecation")
    private void vibrate() {
        Vibrator vibrator = requireContext().getSystemService(Vibrator.class);
        if (Build.VERSION.SDK_INT < 29) {
            vibrator.vibrate(150);
        } else {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
        }
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

        // Use this constructor if ViewPager2 lives directly in a Fragment. Otherwise the child
        // fragments of ViewPager2 will not be destroyed when this fragment is being destroyed.
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
