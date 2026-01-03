package com.bennsch.shoppinglist;

import android.app.Application;
import android.app.UiModeManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.bennsch.shoppinglist.datamodel.PreferencesRepository;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;


public class MyApplication extends Application {
    /*
     *  Base class for maintaining global application state (supersedes activities).
     */

    private static final Integer DYNAMIC_COLOR_SEED = 0xFAD058;

    @Override
    public void onCreate() {
        super.onCreate();
        applyDynamicColors();
        observePrefNightMode();

        // For debugging only:
        if (BuildConfig.DEBUG){
            if (PreferencesRepository.DBG_PRETEND_FIRST_STARTUP){
                PreferencesRepository prefRepo = PreferencesRepository.getInstance(this);
                prefRepo.setPrefFirstStartup(true);
                prefRepo.setPrefOnboardingCompleted(false);
            }
        }
    }

    private void applyDynamicColors() {
        Boolean useDynamicColors = PreferencesRepository.getInstance(getApplicationContext())
                .getPrefUseDynamicColors()
                .getValue();
        if (Boolean.TRUE.equals(useDynamicColors)) {
            DynamicColors.applyToActivitiesIfAvailable(
                    this,
                    new DynamicColorsOptions.Builder()
                         // .setOnAppliedCallback(activity -> Log.d("MyApplication", "DynamicColors applied"))
                            .setContentBasedSource(DYNAMIC_COLOR_SEED)
                            .build());
        }
    }

    private void observePrefNightMode() {
        // No need to remove observer, because all resources will be freed when the application
        // finishes.
        PreferencesRepository.getInstance(getApplicationContext())
                .getPrefNightMode()
                .observeForever(nightMode -> {
                    if (Build.VERSION.SDK_INT >= 31) {
                        UiModeManager uim = (UiModeManager) getSystemService(UI_MODE_SERVICE);
                        switch (nightMode) {
                            case ENABLED:
                                uim.setApplicationNightMode(
                                        UiModeManager.MODE_NIGHT_YES);
                                break;
                            case DISABLED:
                                uim.setApplicationNightMode(
                                        UiModeManager.MODE_NIGHT_NO);
                                break;
                            case FOLLOW_SYSTEM:
                                uim.setApplicationNightMode(
                                        UiModeManager.MODE_NIGHT_AUTO);
                                break;
                            default:
                                assert false;
                        }
                    }else {
                        switch (nightMode) {
                            case ENABLED:
                                AppCompatDelegate.setDefaultNightMode(
                                        AppCompatDelegate.MODE_NIGHT_YES);
                                break;
                            case DISABLED:
                                AppCompatDelegate.setDefaultNightMode(
                                        AppCompatDelegate.MODE_NIGHT_NO);
                                break;
                            case FOLLOW_SYSTEM:
                                AppCompatDelegate.setDefaultNightMode(
                                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                                break;
                            default:
                                assert false;
                        }
                    }
                });
    }
}
