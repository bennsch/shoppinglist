package com.bennsch.shoppinglist;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceFragmentCompat;

import com.bennsch.shoppinglist.data.PreferencesRepository;
import com.bennsch.shoppinglist.databinding.SettingsActivityBinding;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = "SettingsActivity";

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(PreferencesRepository.PREF_RES, rootKey);
        }
    }

    private SettingsActivityBinding mBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesRepository preferencesRepository = PreferencesRepository.getInstance(getApplication());
        preferencesRepository.getPrefUseDynamicColors().observe(this, aBoolean -> {
            Log.d(TAG, "dynaColor (Settings) changed " + aBoolean);
        });

        ThemeHelper.applyDynamicColors(this);

        mBinding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_fragment_container, new SettingsFragment())
                    .commit();
        }

        setupActionBar();
        setupEdgeToEdge();
    }

    private void setupActionBar() {
        setSupportActionBar(mBinding.settingsToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupEdgeToEdge() {
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.settingsFragmentContainer, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets insetsNormal = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
                mBinding.getRoot().setPadding(0, insetsNormal.top, 0, insetsNormal.bottom);
                return insets;
//                 return WindowInsetsCompat.CONSUMED;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}