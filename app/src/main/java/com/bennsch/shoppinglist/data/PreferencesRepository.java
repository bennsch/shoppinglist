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

    public static final int PREF_RES = R.xml.preferences;


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

    private static PreferencesRepository INSTANCE;
    private static final String TAG = "PreferencesRepository";

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private final MutableLiveData<String> mPrefMessageListCompleted = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mPrefUseDynamicColors = new MutableLiveData<>();
    private final MutableLiveData<NightMode> mPrefNightMode = new MutableLiveData<>();


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

    private PreferencesRepository(@NonNull Context context) {
        Log.d(TAG, "PreferencesRepository: Ctor " + context);

        String keyMessageListDeleted = context.getResources().getString(R.string.key_complete_msg);
        String keyUseDynamicColors = context.getResources().getString(R.string.key_use_dynamic_colors);
        String keyNightMode = context.getResources().getString(R.string.key_night_mode);

        // Apply the default values from the xml, because the SharedPreferences
        // won't be initialized until the SettingsActivity is started.
        // This won't override the preferences after the user changed them.
        PreferenceManager.setDefaultValues(context, PREF_RES, false);

        // Initialize the LiveData.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // TODO: use postValue()?
        // We don't need to worry about the default values, because PreferenceManager.setDefaultValues()
        // has been called already.
        mPrefMessageListCompleted.setValue(preferences.getString(keyMessageListDeleted, null));
        mPrefUseDynamicColors.setValue(preferences.getBoolean(keyUseDynamicColors, false));
        mPrefNightMode.setValue(NightMode.fromPrefEntryValue(preferences.getString(keyNightMode, "")));
        // Register a change listener.
        // Cannot use anonymous inner class, because the PreferenceManager
        // does not store a strong reference to the listener and it
        // would be garbage collected.
        preferenceChangeListener = (sharedPreferences, key) -> {
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
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}
