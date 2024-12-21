package com.bennsch.shoppinglist;

import android.app.Application;
import android.content.pm.ActivityInfo;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.bennsch.shoppinglist.data.PreferencesRepository;


public class OrientationHelper {

    private static final String TAG = "OrientationHelper";

    // Call this method during onCreate() of every activity.
    public static void observeOrientationPreference(Application application, AppCompatActivity activity) {
        PreferencesRepository
                .getInstance(application)
                .getPrefOrientation()
                .observe(activity, orientation -> {
                    // TODO: Will briefly switch to landscape if phone held sideways
                    // TODO: called multiple times
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
}
