package com.example.shoppinglist;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.shoppinglist.databinding.ActivityMainBinding;
import com.example.shoppinglist.viewmodel.ChecklistViewModel;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private ChecklistViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        this.binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());

        this.viewModel = new ViewModelProvider(this).get(ChecklistViewModel.class);
        viewModel.getAllChecklistTitles().observe(this, this::onChecklistTitlesChanged);

        setSupportActionBar(this.binding.toolbar);
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
            String currentList = this.binding.navView.getCheckedItem().getTitle().toString();
            this.viewModel.deleteChecklist(currentList);
        } else if (item.getItemId() == R.id.clmenu_rename_list) {
            String currentList = this.binding.navView.getCheckedItem().getTitle().toString();
            this.viewModel.updateChecklistName(currentList, currentList + "-renamed*");
        }
        // Open navigation drawer if toolbar icon is clicked.
        return this.actionBarDrawerToggle.onOptionsItemSelected(item);
    }

    private boolean onNavDrawerItemSelected(MenuItem item){
        item.setCheckable(true);// TODO: 4/1/2024 don't do like this
        if (this.binding.navView.getCheckedItem() != item) { // if not already selected
            if (item.getGroupId() == R.id.group_checklists) {
                showChecklistPagerFragment(item.getTitle().toString());
            } else if (item.getItemId() == R.id.nav_new_list) {
                viewModel.insertChecklist("List " + Calendar.getInstance().get(Calendar.MILLISECOND));
                return false;
            }
        }
        this.binding.drawerLayout.close();
        // Return true, to display the item as the selected item.
        return true;
    }

    private void onChecklistTitlesChanged(List<String> newChecklistTitles) {
        Log.d(TAG, "onChecklistTitlesChanged: " + newChecklistTitles);
        this.binding.navView.getMenu().removeGroup(R.id.group_checklists);
        newChecklistTitles.forEach(title -> {
            Menu menu = this.binding.navView.getMenu();
            menu.add(R.id.group_checklists, Menu.NONE, Menu.NONE, title);
        });
    }

    private void showChecklistPagerFragment(String listTitle) {
        getSupportFragmentManager().beginTransaction()
//                .setCustomAnimations(R.anim.slide, R.anim.slide)
                .replace(this.binding.fragmentContainerView.getId(),
                        ChecklistPagerFragment.class,
                        ChecklistPagerFragment.makeArgs(listTitle))
                .commit();
        this.binding.toolbar.setTitle(listTitle);
    }

    private void setupNavDrawer() {
        this.binding.navView.setNavigationItemSelectedListener(this::onNavDrawerItemSelected);
        // drawer layout instance to toggle the menu icon to
        // open drawer and back button to close drawer
        this.actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                this.binding.drawerLayout,
                R.string.navdrawer_open,
                R.string.navdrawer_close);
        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        this.binding.drawerLayout.addDrawerListener(this.actionBarDrawerToggle);
        // to make the Navigation drawer icon always appear on the action bar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupEdgeToEdgeInsets() {
        View view = this.binding.fragmentContainerView;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures());
            // Apply the insets as padding to the view. Here, set all the dimensions
            // as appropriate to your layout. You can also update the view's margin if
            // more appropriate.
            view.setPadding(0, 0, 0, insets.bottom);
            this.binding.navView.setPadding(0, 0, 0, insets.bottom);

            // Return CONSUMED if you don't want the window insets to keep passing down
            // to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });
    }
}