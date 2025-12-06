package com.bennsch.shoppinglist;

import android.app.Activity;
import android.app.Application;
import android.app.UiModeManager;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bennsch.shoppinglist.datamodel.PreferencesRepository;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;


public class MyApplication extends Application {
    /*
    *   Base class for maintaining global application state (supersedes activities).
    */

    private static final Integer DYNAMIC_COLOR_SEED = 0xFAD058;

    @Override
    public void onCreate() {
        super.onCreate();
        applyDynamicColors();
        observePrefNightMode();
        observePrefOrientation();
    }

    private void applyDynamicColors() {
        Boolean useDynamicColors = PreferencesRepository.getInstance(getApplicationContext())
                .getPrefUseDynamicColors()
                .getValue();
        if (Boolean.TRUE.equals(useDynamicColors)) {
            DynamicColors.applyToActivitiesIfAvailable(
                    this,
                    new DynamicColorsOptions.Builder()
                            //.setOnAppliedCallback(activity -> Log.d("MyApplication", "DynamicColors applied"))
                            .setContentBasedSource(DYNAMIC_COLOR_SEED)
                            .build());
        }
    }

    private void observePrefNightMode() {
        // No need to remove observer, because all resources will be freed when the
        // application finishes.
        PreferencesRepository.getInstance(getApplicationContext())
                .getPrefNightMode()
                .observeForever(nightMode -> {
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
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                PreferencesRepository.getInstance(activity.getApplicationContext())
                        .getPrefOrientation()
                        .observe((AppCompatActivity)activity, orientation -> {
                            // TODO: When returning to MainActivity, the old orientation is still
                            //  visible briefly before it rotates.
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
