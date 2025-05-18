package com.bennsch.shoppinglist;

import android.app.Activity;
import android.app.Application;
import android.app.UiModeManager;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
        GlobalConfig.DBG_SHOW_NAVDRAWER_ACTIONVIEW = preferences.getBoolean("dbg_show_navdrawer_actionview", true);
        GlobalConfig.DBG_SHOW_TRASH = preferences.getBoolean("dbg_show_trash", false);
        GlobalConfig.DBG_FIRST_STARTUP = preferences.getBoolean("dbg_first_startup", false);

        applyDynamicColors();
        observePrefNightMode();
        observePrefOrientation();
    }

    private void applyDynamicColors() {
        Boolean useDynamicColors = PreferencesRepository.getInstance(this)
                .getPrefUseDynamicColors()
                .getValue();
        if (Boolean.TRUE.equals(useDynamicColors)) {
            DynamicColors.applyToActivitiesIfAvailable(
                    this,
                    new DynamicColorsOptions.Builder()
                            .setOnAppliedCallback(activity -> Log.d(TAG, "DynamicColors applied"))
                            .setContentBasedSource(GlobalConfig.DBG_DYNAMIC_COLOR_SEED)
                            .build());
        }
    }

    private void observePrefNightMode() {
        // No need to remove observer, because all resources will be freed when
        // Application finishes.
        PreferencesRepository.getInstance(this)
                .getPrefNightMode()
                .observeForever(nightMode -> {
                    Log.d(TAG, "Setting Night-Mode to " + nightMode);
                    if (Build.VERSION.SDK_INT >= 31) {
                        UiModeManager uim = (UiModeManager) getSystemService(UI_MODE_SERVICE);
                        switch (nightMode) {
                            case ENABLED:
                                uim.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES);
                                break;
                            case DISABLED:
                                uim.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO);
                                break;
                            case FOLLOW_SYSTEM:
                                uim.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO);
                                break;
                            default:
                                assert false;
                        }
                    }else {
                        switch (nightMode) {
                            case ENABLED:
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                                break;
                            case DISABLED:
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                                break;
                            case FOLLOW_SYSTEM:
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                                break;
                            default:
                                assert false;
                        }
                    }
                });
    }

    private void observePrefOrientation() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                PreferencesRepository.getInstance(activity.getApplication())
                        .getPrefOrientation()
                        .observe((AppCompatActivity)activity, orientation -> {
                            // TODO: When returning to MainActivity, the old orientation is still
                            //  visible briefly before it rotates.
                            Log.d(TAG, activity.getClass().getSimpleName() + ": Setting orientation to " + orientation);
                            switch (orientation) {
                                case PORTRAIT:
                                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); break;
                                case LANDSCAPE:
                                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); break;
                                case AUTO:
                                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); break;
                            }
                        });
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            @Override
            public void onActivityResumed(@NonNull Activity activity) {}
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}
