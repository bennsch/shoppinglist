package com.bennsch.shoppinglist;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bennsch.shoppinglist.databinding.ActivityMainBinding;
import com.bennsch.shoppinglist.dialog.AboutDialog;
import com.bennsch.shoppinglist.dialog.EditListDialog;
import com.bennsch.shoppinglist.dialog.NewListDialog;

import java.util.List;

// TODO: Test dark mode
// TODO: Test on oldest supported Android version (no dynamic color pre v12)
// TODO: test device rotation
// TODO: Test rotating the screen in a possible views

// TODO: fix build warning: uses or overrides deprecated API
// TODO: Update gradle packages
// TODO: Update target API
// TODO: Use old icon (shopping cart)
// TODO: highlight action icon while delete mode is active?
// TODO: Limit number of characters for every text input
// TODO: Make all TextFields use textColor as highlight color
// TODO: Put all hardcoded strings to strings.xml
// TODO: General settings:
//          -Dynamic color seed
// TODO: Per Checklist settings:
//          -Sort by incidence
//          -List name
//          -Show suggestions
//          -Delete list

public class MainActivity
        extends AppCompatActivity
        implements  NewListDialog.DialogListener,
                    EditListDialog.DialogListener{

    private static final String TAG = "MainActivity";

    private ActivityMainBinding mBinding;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private MainViewModel viewModel;
    // getValue() is null if no checklist selected yet
    private LiveData<String> mActiveChecklist;
    private IMEHelper mIMEHelper;
    private Menu mOptionsMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        mIMEHelper = new IMEHelper(this);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackButtonPressed();
            }
        });

        this.viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getAllChecklistTitles().observe(this, this::onChecklistTitlesChanged);

        this.viewModel.getDeleteIconsVisible().observe(this, isVisible -> {
            if (mOptionsMenu != null) {
                MenuItem menuItem = mOptionsMenu.findItem(R.id.clmenu_delete_items);
                if (menuItem != null) {
                    if (isVisible) {
                        menuItem.setIcon(R.drawable.ic_not_delete);
                    } else {
                        menuItem.setIcon(R.drawable.ic_delete);
                    }
                }
            }
        });

        mBinding.versionLabel.setText("v" + viewModel.getVersionName());

        mActiveChecklist = viewModel.getActiveChecklist();
        mActiveChecklist.observe(this, this::onActiveChecklistChanged);

        setSupportActionBar(mBinding.toolbar);
        setupNavDrawer();
        setupEdgeToEdgeInsets();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        // avoid starting with arrow in toolbar
        this.actionBarDrawerToggle.syncState();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.checklist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clmenu_edit_list) {
            showEditListDialog();
        } else if (item.getItemId() == R.id.clmenu_delete_items) {
            this.viewModel.toggleDeleteIconsVisibility();
        }
        // Open navigation drawer if toolbar icon is clicked.
        return this.actionBarDrawerToggle.onOptionsItemSelected(item);
    }

    public void onBackButtonPressed() {
        Log.d(TAG, "handleOnBackPressed: ");
        if (mBinding.drawerLayout.isOpen()) {
            mBinding.drawerLayout.close();
        } else if (viewModel.isDeleteIconVisible()) {
            viewModel.toggleDeleteIconsVisibility();
        } else {
            finish();
        }
    }

    private boolean onNavDrawerItemSelected(MenuItem item){
        if (mBinding.navView.getCheckedItem() == item) {
            // Item already selected.
        } else if (item.getGroupId() == R.id.group_checklists) {
            // A list has been selected
            viewModel.setActiveChecklist(item.getTitle().toString());
            mBinding.drawerLayout.close();
        } else if (item.getItemId() == R.id.nav_new_list) {
            showNewListDialog();
            mBinding.drawerLayout.close();
        } else if (item.getItemId() == R.id.nav_about) {
            showAboutDialog();
        } else if (item.getItemId() == R.id.nav_settings) {
            showSettingsActivity();
        } else {
            assert false;
        }
        // Return false to not display the item as checked
        // (will be handled in onActiveChecklistChanged())
        return false;
    }

    private void onActiveChecklistChanged(@Nullable String newActiveChecklist) {
        if (newActiveChecklist == null) {
            // No item selected yet or no lists present.
            showChecklist(null);
        } else {
            Menu menu = mBinding.navView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                boolean active = item.getTitle().equals(newActiveChecklist);
                item.setChecked(active);
                View actionView = item.getActionView();
                if (actionView != null) {
                    // Show ActionView only for selected checklist and hide
                    // every other ActionView.
                    actionView.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
                }
            }
            showChecklist(newActiveChecklist);
        }
    }

    private void onChecklistTitlesChanged(List<String> newChecklistTitles) {
        // Remove entire group and populate again with current checklist titles.
        mBinding.navView.getMenu().removeGroup(R.id.group_checklists);
        newChecklistTitles.forEach(title -> {
            // Create new MenuItem.
            Menu menu = mBinding.navView.getMenu();
            MenuItem menuItem = menu.add(R.id.group_checklists, Menu.NONE, Menu.NONE, title);
            menuItem.setCheckable(true);
            if (GlobalConfig.DBG_SHOW_NAVDRAWER_ACTIONVIEW) {
                // Add ActionView.
                AppCompatImageButton actionView = new AppCompatImageButton(this);
                actionView.setImageResource(R.drawable.ic_edit);
                actionView.setBackground(null);
                actionView.setOnClickListener(v -> showEditListDialog());
                menuItem.setActionView(actionView);
            }
            // Highlight the currently selected checklist and hide ActionViews
            // from all other items.
            if (mActiveChecklist.getValue() != null) { // null if no checklist selected yet.
                if (mActiveChecklist.getValue().equals(title)) {
                    Log.d(TAG, "onChecklistTitlesChanged: setChecked " + title);
                    menuItem.setChecked(true);
                }else{
                    View actionView = menuItem.getActionView();
                    if (actionView != null) {
                        menuItem.getActionView().setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    private void showChecklist(@Nullable String listTitle) {
        // TODO: add animation for fragment transaction?
        if (listTitle == null) {
            // No lists present
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
            // TODO: what if there's more than one group?
            mBinding.toolbar.getMenu().setGroupVisible(0, false);
        } else {
            getSupportFragmentManager().beginTransaction()
                    //.setCustomAnimations(R.anim.slide, R.anim.slide)
                    .replace(
                            mBinding.fragmentContainerView.getId(),
                            ChecklistPagerFragment.class,
                            ChecklistPagerFragment.makeArgs(listTitle))
                    .commit();
            mBinding.toolbar.setTitle(listTitle);
            mBinding.toolbar.getMenu().setGroupVisible(0, true);
        }

    }

    private void showSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void setupNavDrawer() {
        mBinding.navView.setNavigationItemSelectedListener(this::onNavDrawerItemSelected);
        // drawer layout instance to toggle the menu icon to
        // open drawer and back button to close drawer
        this.actionBarDrawerToggle = new ActionBarDrawerToggle(
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
        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        mBinding.drawerLayout.addDrawerListener(this.actionBarDrawerToggle);
        // to make the Navigation drawer icon always appear on the action bar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupEdgeToEdgeInsets() {
        View view = mBinding.fragmentContainerView;
        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                // TODO: Handle landscape orientation
                Insets insetsNormal = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
                mBinding.navView.setPadding(0, 0, 0, insetsNormal.bottom); // to show version number above bottom navigation bar
                return insets;
//                 return WindowInsetsCompat.CONSUMED;
            }
        });
    }

    private void showNewListDialog() {
        new NewListDialog()
                .show(getSupportFragmentManager(), "NewListDialog");
    }

    private void showAboutDialog() {
        AboutDialog.newInstance(viewModel.getVersionName())
                .show(getSupportFragmentManager(), "AboutDialog");
    }

    private void showEditListDialog() {
        EditListDialog.newInstance(mActiveChecklist.getValue())
                .show(getSupportFragmentManager(), "EditListDialog");
    }

    @Override
    public void newListDialog_onCreateClicked(String title) {
        try {
            viewModel.insertChecklist(title);
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void newListDialog_onValidateTitle(String title) throws Exception{
        viewModel.validateNewChecklistName(title);
    }

    @Override
    public void editListDialog_onSafeClicked(String oldTitle, String newTitle) throws IllegalArgumentException{
        try {
            viewModel.renameChecklist(oldTitle, newTitle);
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public String editListDialog_onValidateTitle(String title) throws Exception{
        return viewModel.validateNewChecklistName(title);
    }

    @Override
    public void editListDialog_onDeleteClicked(String listTitle) {
        viewModel.deleteChecklist(listTitle);
    }
}