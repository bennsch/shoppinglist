package com.bennsch.shoppinglist;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.bennsch.shoppinglist.data.PreferencesRepository;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;


public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();

        // TODO: Remove this debug code
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        GlobalConfig.DBG_SHOW_INCIDENCE = preferences.getBoolean("dbg_show_incidence", false);
        GlobalConfig.DBG_SHOW_NAVDRAWER_ACTIONVIEW = preferences.getBoolean("dbg_show_navdrawer_actionview", false);
        GlobalConfig.DBG_SHOW_TRASH = preferences.getBoolean("dbg_show_trash", false);

        PreferencesRepository preferencesRepository = PreferencesRepository.getInstance(this);

        Boolean useDynamicColors = preferencesRepository.getPrefUseDynamicColors().getValue();
        if (Boolean.TRUE.equals(useDynamicColors)) {
            DynamicColors.applyToActivitiesIfAvailable(
                    this,
                    new DynamicColorsOptions.Builder()
                            .setOnAppliedCallback(activity -> Log.d(TAG, "DynamicColors applied"))
                            .setContentBasedSource(GlobalConfig.DBG_DYNAMIC_COLOR_SEED)
                            .build());
        }

        // No need to remove observer, because all resources will be freed when
        // Application finishes.
        preferencesRepository
                .getPrefNightMode()
                .observeForever(nightMode -> {
                    Log.d(TAG, "onCreate: Setting Night-Mode to " + nightMode);
                    switch (nightMode) {
                        case ENABLED: AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES); break;
                        case DISABLED: AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO); break;
                        case FOLLOW_SYSTEM: AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
                        default: assert false;
                    }
                });
    }
}
