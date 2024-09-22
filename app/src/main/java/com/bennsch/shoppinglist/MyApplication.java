package com.bennsch.shoppinglist;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

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
    }
}
