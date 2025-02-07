package com.bennsch.shoppinglist.data;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.bennsch.shoppinglist.R;


public class PreferencesRepository {

    public static final int PREF_RES = R.xml.preference_screen;

    // TODO: Find better solution. E.g. simply use integers instead of strings
    public enum NightMode {
        ENABLED,
        DISABLED,
        FOLLOW_SYSTEM;

        public static NightMode fromPrefEntryValue(@NonNull String entryValue) {
            // Needs to match resource array "pref_night_mode_entry_values".
            switch (entryValue) {
                case "enabled":
                    return NightMode.ENABLED;
                case "disabled":
                    return NightMode.DISABLED;
                case "follow_system":
                    return NightMode.FOLLOW_SYSTEM;
                default:
                    assert false : "Invalid entryValue: " + entryValue;
                    return null;
            }
        }
    }

    public enum Orientation {
        PORTRAIT,
        LANDSCAPE,
        AUTO;

        public static Orientation fromPrefEntryValue(@NonNull String entryValue) {
            // Needs to match resource array "pref_orientation_entry_values".
            switch (entryValue) {
                case "portrait":
                    return Orientation.PORTRAIT;
                case "landscape":
                    return Orientation.LANDSCAPE;
                case "auto":
                    return Orientation.AUTO;
                default:
                    assert false : "Invalid entryValue: " + entryValue;
                    return null;
            }
        }
    }

    private static PreferencesRepository INSTANCE;
    private static final String TAG = "PreferencesRepository";

    // TODO: Why does using an instance variable not work in release build? Instance variables are strong references.
    private static SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = null;
    private final MutableLiveData<String> mPrefMessageListCompleted = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mPrefUseDynamicColors = new MutableLiveData<>();
    private final MutableLiveData<NightMode> mPrefNightMode = new MutableLiveData<>();
    private final MutableLiveData<Orientation> mPrefOrientation = new MutableLiveData<>();


    public static synchronized PreferencesRepository getInstance(@NonNull Application application) {
        if (INSTANCE == null) {
            INSTANCE = new PreferencesRepository(application);
        }
        return INSTANCE;
    }

    public LiveData<String> getPrefMessageListDeleted() {
        return mPrefMessageListCompleted;
    }

    public LiveData<Boolean> getPrefUseDynamicColors() {
        return mPrefUseDynamicColors;
    }

    public LiveData<NightMode> getPrefNightMode() {
        return mPrefNightMode;
    }

    public LiveData<Orientation> getPrefOrientation() {
        return mPrefOrientation;
    }

    private PreferencesRepository(@NonNull Context context) {
        Log.d(TAG, "PreferencesRepository: Ctor " + context);

        String keyMessageListDeleted = context.getResources().getString(R.string.key_complete_msg);
        String keyUseDynamicColors = context.getResources().getString(R.string.key_use_dynamic_colors);
        String keyNightMode = context.getResources().getString(R.string.key_night_mode);
        String keyOrientation = context.getResources().getString(R.string.key_orientation);

        // Apply the default values from the xml, because the SharedPreferences
        // won't be initialized until the SettingsActivity is started.
        // Parameter "readAgain" is set to "true", so that default values will be applied
        // even if this method has been called in the past (so that newly added preferences
        // will get their default value applied).
        // This won't override the preferences after the user changed them.
        PreferenceManager.setDefaultValues(context, PREF_RES, true);

        // Initialize the LiveData.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // TODO: use postValue()?
        // We don't need to worry about the default values, because PreferenceManager.setDefaultValues()
        // has been called already.
        mPrefMessageListCompleted.setValue(preferences.getString(keyMessageListDeleted, null));
        mPrefUseDynamicColors.setValue(preferences.getBoolean(keyUseDynamicColors, false));
        mPrefNightMode.setValue(NightMode.fromPrefEntryValue(preferences.getString(keyNightMode, "")));
        mPrefOrientation.setValue(Orientation.fromPrefEntryValue(preferences.getString(keyOrientation, "")));
        // Register a change listener.
        // Cannot use anonymous inner class, because the PreferenceManager
        // does not store a strong reference to the listener and it
        // would be garbage collected.
        if (mPreferenceChangeListener != null) {
            throw new RuntimeException("mPreferenceChangeListener != null");
        }
        mPreferenceChangeListener = (sharedPreferences, key) -> {
            // TODO: use postValue()?
            Log.d(TAG, "PreferencesRepository: onChangeListener " + key);
            if (key == null) {
                Log.w(TAG, "key == null");
            } else if (key.contentEquals(keyMessageListDeleted)) {
                mPrefMessageListCompleted.setValue(sharedPreferences.getString(key, null));
            } else if (key.contentEquals(keyUseDynamicColors)) {
                mPrefUseDynamicColors.setValue(sharedPreferences.getBoolean(key, false));
            } else if (key.contentEquals(keyNightMode)) {
                mPrefNightMode.setValue(NightMode.fromPrefEntryValue(sharedPreferences.getString(key, "")));
            }else if (key.contentEquals(keyOrientation)) {
                mPrefOrientation.setValue(Orientation.fromPrefEntryValue(sharedPreferences.getString(key, "")));
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }
}
