package com.example.shoppinglist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import android.os.Bundle;
import android.view.MenuItem;

import com.example.shoppinglist.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());

        setSupportActionBar(this.binding.toolbar);
        setupNavDrawer();

        this.binding.textView.setText("Hello Drawer");
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        // avoid starting with arrow in toolbar
        this.actionBarDrawerToggle.syncState();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Open navigation drawer if toolbar icon is clicked.
        return this.actionBarDrawerToggle.onOptionsItemSelected(item);
    }

    private boolean onNavDrawerItemSelected(MenuItem item){
        this.binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
}