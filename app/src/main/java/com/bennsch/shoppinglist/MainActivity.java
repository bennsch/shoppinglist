package com.bennsch.shoppinglist;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bennsch.shoppinglist.databinding.ActivityMainBinding;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import java.util.Calendar;
import java.util.List;

// TODO: Test dark mode
// TODO: Test on oldest supported Android version (no dynamic color pre v12)
// TODO: test device rotation

// TODO: highlight action icon while delete mode is active?
// TODO: strip white space from item when user add a new one
// TODO: Show suggestions when typing new item
// TODO: Rounded corners for ItemNameBox
// TODO: List is moving down slightly if IME is opened (if there are only few items)
// TODO: Make ItemNameBox smaller so that it doesn't align with last divider
//       (maybe use LinearLayout instead of ConstraintLayout)
// TODO: 'About' info (git repo, name etc)

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding mBinding;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private AppViewModel viewModel;
    // getValue() is null if no checklist selected yet
    private LiveData<String> mActiveChecklist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Apply colors derived from a seed.
        // (If dynamic colors are not supported on the device, the colors
        // defined in "AppTheme" will be applied).
        DynamicColors.applyToActivityIfAvailable(
                this,
                new DynamicColorsOptions.Builder()
                        .setContentBasedSource(0xf5e4ba)
                        .build());

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.versionLabel.setText("v" + getVersionName());

        this.viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        viewModel.getAllChecklistTitles().observe(this, this::onChecklistTitlesChanged);

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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.checklist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clmenu_delete_list) {
            String currentList = mBinding.navView.getCheckedItem().getTitle().toString();
            this.viewModel.deleteChecklist(currentList);
        } else if (item.getItemId() == R.id.clmenu_rename_list) {
            String currentList = mBinding.navView.getCheckedItem().getTitle().toString();
            this.viewModel.renameChecklist(currentList, currentList + "-renamed*");
        } else if (item.getItemId() == R.id.clmenu_delete_items) {
            this.viewModel.toggleDeleteIconsVisibility();
        }
        // Open navigation drawer if toolbar icon is clicked.
        return this.actionBarDrawerToggle.onOptionsItemSelected(item);
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "onCreate: ", e);
            return "?.?";
        }
    }

    private boolean onNavDrawerItemSelected(MenuItem item){
        if (mBinding.navView.getCheckedItem() == item) {
            // Item already selected.
        }
        else {
            if (item.getGroupId() == R.id.group_checklists) {
                viewModel.setActiveChecklist(item.getTitle().toString());
            } else if (item.getItemId() == R.id.nav_new_list) {
                NewListDialog dialog =  new NewListDialog();
                dialog.setCurrentLists(viewModel.getAllChecklistTitles().getValue());
                dialog.setDialogListener(new NewListDialog.DialogListener() {
                    @Override
                    public void onCreateListClicked(String title) {
                        try {
                            viewModel.insertChecklist(title);
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelClicked() {
                        // Do nothing.
                    }
                });
                dialog.show(getSupportFragmentManager(), "NewListDialog");
            }
        }
        mBinding.drawerLayout.close();
        // Return false to not display the item as checked
        // (will be handled in onActiveChecklistChanged())
        return false;
    }

    private void onActiveChecklistChanged(@Nullable String newActiveChecklist) {
        if (newActiveChecklist == null) {
            // No item selected yet.
            Log.d(TAG, "onActiveChecklistChanged: is null");
        } else {
            Menu menu = mBinding.navView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getTitle().equals(newActiveChecklist)) {
                    menu.getItem(i).setChecked(true);
                    Log.d(TAG, "onActiveChecklistChanged: setChecked " + newActiveChecklist);
                    break;
                }
            }
            showChecklistPagerFragment(newActiveChecklist);
        }
    }

    private void onChecklistTitlesChanged(List<String> newChecklistTitles) {
        mBinding.navView.getMenu().removeGroup(R.id.group_checklists);
        newChecklistTitles.forEach(title -> {
            Menu menu = mBinding.navView.getMenu();
            MenuItem newItem = menu.add(R.id.group_checklists, Menu.NONE, Menu.NONE, title);
            newItem.setCheckable(true);
            if (mActiveChecklist.isInitialized()) {
                if (mActiveChecklist.getValue() == null) {
                    // No checklist selected yet
                }
                else if (mActiveChecklist.getValue().equals(title)) {
                    Log.d(TAG, "onChecklistTitlesChanged: setChecked " + title);
                    newItem.setChecked(true);
                }else{
                    // Do nothing
                }
            } else {
                // TODO: Remove, for debugging only
//                throw new RuntimeException("mActiveChecklist not initialized yet");
                Log.e(TAG, "onChecklistTitlesChanged: mActiveChecklist not initialized yet!, " + newChecklistTitles);
            }
        });
    }

    private void showChecklistPagerFragment(@Nullable String listTitle) {
        getSupportFragmentManager().beginTransaction()
                //.setCustomAnimations(R.anim.slide, R.anim.slide)
                .replace(mBinding.fragmentContainerView.getId(),
                        ChecklistPagerFragment.class,
                        ChecklistPagerFragment.makeArgs(listTitle))
                .commit();
        mBinding.toolbar.setTitle(listTitle);
    }

    private void setupNavDrawer() {
        mBinding.navView.setNavigationItemSelectedListener(this::onNavDrawerItemSelected);
        // drawer layout instance to toggle the menu icon to
        // open drawer and back button to close drawer
        this.actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                mBinding.drawerLayout,
                R.string.navdrawer_open,
                R.string.navdrawer_close);
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
                Insets insetsNormal = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
                mBinding.navView.setPadding(0, 0, 0, insetsNormal.bottom); // to show version number above bottom navigation bar
                return insets;
//                 return WindowInsetsCompat.CONSUMED;
            }
        });
    }
}