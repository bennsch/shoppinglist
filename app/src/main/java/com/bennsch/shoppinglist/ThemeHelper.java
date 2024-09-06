package com.bennsch.shoppinglist;

import android.app.Activity;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public class ThemeHelper {

    public static void applyDynamicColors(Activity activity) {
        // Apply colors derived from a seed.
        // (If dynamic colors are not supported on the device, the colors
        // defined in "AppTheme" will be applied).
        if (GlobalConfig.DBG_DYNAMIC_COLOR_ENABLED) {
            DynamicColors.applyToActivityIfAvailable(
                    activity,
                    new DynamicColorsOptions.Builder()
                            .setContentBasedSource(GlobalConfig.DBG_DYNAMIC_COLOR_SEED)
                            //.setThemeOverlay(R.style.ThemeOverlay_AppTheme_HighContrast) // TODO: why is it not working?
                            .build());
        }
    }
}
