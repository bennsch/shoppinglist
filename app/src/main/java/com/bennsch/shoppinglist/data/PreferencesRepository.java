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

    private final SharedPreferences mSharedPreferences;

    private final String mKeyMessageListDeleted;
    private final String mKeyUseDynamicColors;
    private final String mKeyNightMode;
    private final String mKeyOrientation;
    private final String mKeyFirstStartup;
    private final String mKeyOnboardingCompleted;

    // TODO: Why does using an instance variable not work in release build? Instance variables are strong references.
    //  Maybe try to keep SharedPreferences object as instance variable?
    private static SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = null;

    // Create LiveData objects for preferences displayed on the PreferenceScreen:
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

    public boolean getPrefFirstStartup() {
        return mSharedPreferences.getBoolean(mKeyFirstStartup, true);
    }

    public void setPrefFirstStartup(boolean firstStartup) {
        mSharedPreferences.edit().putBoolean(mKeyFirstStartup, firstStartup).apply();
    }

    public boolean getPrefOnboardingCompleted() {
        return mSharedPreferences.getBoolean(mKeyOnboardingCompleted, false);
    }

    public void setPrefOnboardingCompleted(boolean completed) {
        mSharedPreferences.edit().putBoolean(mKeyOnboardingCompleted, completed).apply();
    }

    private PreferencesRepository(@NonNull Context context) {
        Log.d(TAG, "PreferencesRepository: Ctor " + context);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mKeyMessageListDeleted = context.getResources().getString(R.string.key_complete_msg);
        mKeyUseDynamicColors = context.getResources().getString(R.string.key_use_dynamic_colors);
        mKeyNightMode = context.getResources().getString(R.string.key_night_mode);
        mKeyOrientation = context.getResources().getString(R.string.key_orientation);
        mKeyFirstStartup = context.getResources().getString(R.string.key_first_startup);
        mKeyOnboardingCompleted = context.getResources().getString(R.string.key_onboarding_completed);

        // Apply the default values from the xml, because the SharedPreferences
        // won't be initialized until the SettingsActivity is started.
        // Parameter "readAgain" is set to "true", so that default values will be applied
        // even if this method has been called in the past (so that newly added preferences
        // will get their default value applied).
        // This won't override the preferences after the user changed them.
        PreferenceManager.setDefaultValues(context, PREF_RES, true);

        // Initialize the LiveData.
        // TODO: use postValue()?
        // We don't need to worry about the default values, because PreferenceManager.setDefaultValues()
        // has been called already.
        mPrefMessageListCompleted.setValue(mSharedPreferences.getString(mKeyMessageListDeleted, null));
        mPrefUseDynamicColors.setValue(mSharedPreferences.getBoolean(mKeyUseDynamicColors, false));
        mPrefNightMode.setValue(NightMode.fromPrefEntryValue(mSharedPreferences.getString(mKeyNightMode, "")));
        mPrefOrientation.setValue(Orientation.fromPrefEntryValue(mSharedPreferences.getString(mKeyOrientation, "")));

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
            } else if (key.contentEquals(mKeyMessageListDeleted)) {
                mPrefMessageListCompleted.setValue(sharedPreferences.getString(key, null));
            } else if (key.contentEquals(mKeyUseDynamicColors)) {
                mPrefUseDynamicColors.setValue(sharedPreferences.getBoolean(key, false));
            } else if (key.contentEquals(mKeyNightMode)) {
                mPrefNightMode.setValue(NightMode.fromPrefEntryValue(sharedPreferences.getString(key, "")));
            } else if (key.contentEquals(mKeyOrientation)) {
                mPrefOrientation.setValue(Orientation.fromPrefEntryValue(sharedPreferences.getString(key, "")));
            }
        };
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }
}
