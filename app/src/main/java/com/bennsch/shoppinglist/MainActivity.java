package com.bennsch.shoppinglist;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bennsch.shoppinglist.data.PreferencesRepository;
import com.bennsch.shoppinglist.databinding.ActivityMainBinding;
import com.bennsch.shoppinglist.dialog.AboutDialog;
import com.bennsch.shoppinglist.dialog.EditListDialog;
import com.bennsch.shoppinglist.dialog.NewListDialog;
import com.bennsch.shoppinglist.dialog.WelcomeDialog;

import java.util.List;



public class MainActivity extends AppCompatActivity
        implements  NewListDialog.DialogListener,
                    EditListDialog.DialogListener{

    private ActivityMainBinding mBinding;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private IMEHelper mIMEHelper;
    private MainViewModel mViewModel;
    // Null if no Checklist selected.
    private LiveData<String> mActiveChecklist;
    // Null if no Checklist selected.
    private MainViewModel.DeleteItemsMode mDeleteItemsMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Needs to be called before onCreate().
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        mIMEHelper = new IMEHelper(this);
        // Initialize view.
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);
        setupNavDrawer();
        addNavViewPadding();
        // Retrieve global ViewModel instance.
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mActiveChecklist = mViewModel.getActiveChecklist();
        mDeleteItemsMode = mViewModel.getDeleteItemsMode();
        // Register observers.
        mActiveChecklist.observe(this,
                this::onActiveChecklistChanged);
        mDeleteItemsMode.observe(this,
                this::onDeleteItemsModeChanged);
        mViewModel.getAllChecklistTitles(PreferencesRepository.DBG_SHOW_TRASH).observe(this,
                this::onChecklistTitlesChanged);
        // Perform certain actions only the first time the app has been launched.
        PreferencesRepository preferencesRepo = PreferencesRepository.getInstance(getApplication());
        if (preferencesRepo.getPrefFirstStartup()) {
            // showWelcomeDialog();
            mViewModel.getSimpleOnboarding()
                    .notify(MainViewModel.Onboarding.Event.START_ONBOARDING);
            preferencesRepo.setPrefFirstStartup(false);
        }
        // Register callback for the  device's "back" button.
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackButtonPressed();
            }
        });
        if (BuildConfig.DEBUG){
            mBinding.versionLabel.setText("v" + mViewModel.getVersionName());
            mBinding.versionLabel.setVisibility(View.VISIBLE);
            if (PreferencesRepository.DBG_PRETEND_FIRST_STARTUP){
                mViewModel.getSimpleOnboarding().notify(MainViewModel.Onboarding.Event.START_ONBOARDING);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Set the icon for the "Delete Items" toolbar item depending on
        // on the current DeleteItemsMode:
        MenuItem menuItem = menu.findItem(R.id.clmenu_delete_items);
        Drawable icon;
        if (mDeleteItemsMode.getValue() == MainViewModel.DeleteItemsMode.DISABLED) {
            menuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_delete));
            menuItem.setEnabled(false);
            icon = menuItem.getIcon();
            if (icon != null) {
                icon.setTint(ThemeHelper.getColor(
                        com.google.android.material.R.attr.colorOutlineVariant, this));
            }
        } else {
            int drawable = (mDeleteItemsMode.getValue() == MainViewModel.DeleteItemsMode.ACTIVATED)
                    ? R.drawable.ic_not_delete
                    : R.drawable.ic_delete;
            menuItem.setIcon(ContextCompat.getDrawable(this, drawable));
            menuItem.setEnabled(true);
            icon = menuItem.getIcon();
            if (icon != null) {
                icon.setTint(ThemeHelper.getColor(
                        com.google.android.material.R.attr.colorOnSurfaceVariant, this));
            }
        }
        // Hide toolbar icons if no Checklist is active.
        menu.setGroupVisible(0, mActiveChecklist.getValue() != null);
        // Return true for the menu to be displayed.
        return true;
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Prevent an arrow to be displayed when toolbar is created.
        this.mActionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.checklist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clmenu_edit_list) {
            showEditListDialog();
        } else if (item.getItemId() == R.id.clmenu_delete_items) {
            mDeleteItemsMode.toggle();
        }
        // Open navigation drawer if toolbar icon is clicked.
        return this.mActionBarDrawerToggle.onOptionsItemSelected(item);
    }

    public void onBackButtonPressed() {
        // Close the NavDrawer, deactivate DeleteItemsMode, or quit
        // the application if the back button is pressed
        if (mBinding.drawerLayout.isOpen()) {
            mBinding.drawerLayout.close();
        } else if (mDeleteItemsMode.getValue() == MainViewModel.DeleteItemsMode.ACTIVATED) {
            mDeleteItemsMode.toggle();
        } else {
            // Quit the application
            finish();
        }
    }

    private boolean onNavDrawerItemSelected(MenuItem item){
        // Confirm the clicked item is not already selected.
        if (mBinding.navView.getCheckedItem() != item) {
            if (item.getGroupId() == R.id.group_checklists) {
                // A Checklist has been selected, activate it.
                CharSequence listTitle = item.getTitle();
                assert listTitle != null: "item.getTitle() returned null";
                mViewModel.setActiveChecklist(item.getTitle().toString());
                mBinding.drawerLayout.close();
            } else if (item.getItemId() == R.id.nav_new_list) {
                showNewListDialog();
            } else if (item.getItemId() == R.id.nav_about) {
                showAboutDialog();
            } else if (item.getItemId() == R.id.nav_settings) {
                showSettingsActivity();
            } else {
                assert false: "Unexpected menu item: id = " + item.getItemId();
            }
        }
        // Return false so that the selected menu item won't be highlighted
        // (this will be handled in onActiveChecklistChanged()).
        return false;
    }

    private void onDeleteItemsModeChanged(Integer mode) {
        // Will trigger the onPrepareOptionsMenu() callback.
        invalidateMenu();
    }

    private void onActiveChecklistChanged(@Nullable String newActiveChecklist) {
        if (newActiveChecklist == null) {
            // No item was selected yet, or no lists present at all.
            showChecklist(null);
        } else {
            Menu menu = mBinding.navView.getMenu();
            // Iterate over all menu items:
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                CharSequence title = item.getTitle();
                assert title != null: "item.getTitle() returned null";
                boolean isActive = title.equals(newActiveChecklist);
                View actionView = item.getActionView();
                // Highlight the menu item if it's the currently
                // selected Checklist.
                item.setChecked(isActive);
                // Check if this menu item has an ActionView (icon next to the item)
                // assigned (only the Checklist items will have ActionViews).
                if (actionView != null) {
                    // Show the ActionView only for the selected Checklist and hide every
                    // other ActionView.
                    actionView.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
                }
            }
            showChecklist(newActiveChecklist);
            mBinding.drawerLayout.close();
        }
    }

    private void onChecklistTitlesChanged(List<String> newChecklistTitles) {
        // Remove entire group and populate again with current Checklist titles.
        mBinding.navView.getMenu().removeGroup(R.id.group_checklists);
        newChecklistTitles.forEach(title -> {
            // Create new MenuItem.
            Menu menu = mBinding.navView.getMenu();
            MenuItem menuItem = menu.add(R.id.group_checklists, Menu.NONE, Menu.NONE, title);
            menuItem.setCheckable(true);
            // Add ActionView (icon next to the item)
            AppCompatImageButton actionView = new AppCompatImageButton(this);
            actionView.setImageResource(R.drawable.ic_edit);
            actionView.setColorFilter(ThemeHelper.getColor(
                    com.google.android.material.R.attr.colorOnSurfaceVariant, this));
            actionView.setBackground(null);
            actionView.setOnClickListener(v -> showEditListDialog());
            menuItem.setActionView(actionView);
            // Highlight the currently selected Checklist and hide ActionViews
            // from all other items.
            if (mActiveChecklist.getValue() != null) { // null if no Checklist selected.
                // Highlight the menu item if it's the currently
                // selected Checklist.
                if (mActiveChecklist.getValue().equals(title)) {
                    menuItem.setChecked(true);
                }else{
                    View av = menuItem.getActionView();
                    if (av != null) {
                        // Show the ActionView only for the selected Checklist and hide every
                        // other ActionView.
                        menuItem.getActionView().setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    private void showChecklist(@Nullable String listTitle) {
        // Show the Checklist named "listTitle". If "listTitle" is null,
        // show the NoListsFragment.
        // TODO: add animation for fragment transaction?
        if (listTitle == null) {
            getSupportFragmentManager()
                    .setFragmentResultListener(
                    NoListsFragment.REQ_KEY_NEW_LIST_BUTTON_CLICKED,
                    this,
                    (requestKey, result) -> showNewListDialog());
            getSupportFragmentManager()
                    .beginTransaction()
                    //.setCustomAnimations(R.anim.slide, R.anim.slide)
                    .replace(
                            mBinding.fragmentContainerView.getId(),
                            new NoListsFragment())
                    .commit();
            mBinding.toolbar.setTitle("");
            invalidateMenu();
        } else {
            getSupportFragmentManager().beginTransaction()
                    //.setCustomAnimations(R.anim.slide, R.anim.slide)
                    .replace(
                            mBinding.fragmentContainerView.getId(),
                            ChecklistPagerFragment.class,
                            ChecklistPagerFragment.makeArgs(listTitle))
                    .commit();
            mBinding.toolbar.setTitle(listTitle);
        }
    }

    private void showSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void setupNavDrawer() {
        mBinding.navView.setNavigationItemSelectedListener(this::onNavDrawerItemSelected);
        // Make the menu icon open the NavDrawer (back button will close the NavDrawer)
        this.mActionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                mBinding.drawerLayout,
                R.string.navdrawer_open,
                R.string.navdrawer_close){
            @Override
            public void onDrawerStateChanged(int newState) {
                // Hide IME if NavDrawer is opened.
                // ActionBarDrawerToggle.onDrawerOpened() will be called with a
                // small delay, so using ActionBarDrawerToggle.onDrawerStateChanged() instead
                if (newState == DrawerLayout.STATE_SETTLING && !mBinding.drawerLayout.isOpen()){
                    mIMEHelper.showIME(mBinding.getRoot(), false);
                }
            }
        };
        mBinding.drawerLayout.addDrawerListener(this.mActionBarDrawerToggle);
        // Make the Navigation drawer icon always appear on the action bar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void addNavViewPadding() {
        // Add padding to the NavView matching the height of the bottom navigation bar
        // (currently used to place the version number label).
        View view = mBinding.fragmentContainerView;
        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets insetsNormal = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
                mBinding.navView.setPadding(0, 0, 0, insetsNormal.bottom);
                return insets;
                // return WindowInsetsCompat.CONSUMED;
            }
        });
    }

    private void showNewListDialog() {
        new NewListDialog()
                .show(getSupportFragmentManager(), "NewListDialog");
    }

    private void showAboutDialog() {
        AboutDialog.newInstance(mViewModel.getVersionName())
                .show(getSupportFragmentManager(), "AboutDialog");
    }

    private void showWelcomeDialog() {
        WelcomeDialog dialog = WelcomeDialog.newInstance();
        dialog.setOnClickListener(
                (dialog1, which) -> mViewModel.getSimpleOnboarding()
                        .notify(MainViewModel.Onboarding.Event.START_ONBOARDING));
        dialog.show(getSupportFragmentManager(), "WelcomeDialog");
    }

    private void showEditListDialog() {
        EditListDialog.newInstance(mActiveChecklist.getValue())
                .show(getSupportFragmentManager(), "EditListDialog");
    }

    @Override
    public void newListDialog_onCreateClicked(String title) {
        try {
            mViewModel.insertChecklist(title);
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void newListDialog_onValidateTitle(String title) throws IllegalArgumentException{
        mViewModel.validateNewChecklistName(title);
    }

    @Override
    public void editListDialog_onSafeClicked(String oldTitle, String newTitle) throws IllegalArgumentException{
        try {
            mViewModel.renameChecklist(oldTitle, newTitle);
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void editListDialog_onValidateTitle(String title) throws IllegalArgumentException{
        mViewModel.validateNewChecklistName(title);
    }

    @Override
    public void editListDialog_onDeleteClicked(String listTitle) {
        mViewModel.moveChecklistToTrash(listTitle);
    }
}