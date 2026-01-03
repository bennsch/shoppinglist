package com.bennsch.shoppinglist.datamodel;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.bennsch.shoppinglist.BuildConfig;
import com.bennsch.shoppinglist.R;


public class PreferencesRepository {
    /*
     *  A Repository class abstracts access to multiple data sources. Currently only one data source
     *  is implemented (SharedPreferences).
     */

    public static final int PREFS_RES_ID = R.xml.preference_screen;

    // Will be overwritten in debug build:
    public static boolean DBG_PRETEND_FIRST_STARTUP = false;
    public static boolean DBG_SHOW_INCIDENCE = false;
    public static boolean DBG_SHOW_TRASH = false;


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

    private static PreferencesRepository INSTANCE;
    private final SharedPreferences mSharedPreferences;
    private final String mKeyMessageListDeleted;
    private final String mKeyUseDynamicColors;
    private final String mKeyNightMode;
    private final String mKeyFirstStartup;
    private final String mKeyOnboardingCompleted;

    // TODO: Why does using an instance variable won't work in release build? Instance variables are
    //  strong references. Maybe try to keep SharedPreferences object as instance variable?
    private static SharedPreferences.OnSharedPreferenceChangeListener
            mPreferenceChangeListener = null;

    // Create LiveData objects for preferences that are displayed on the PreferenceScreen:
    private final MutableLiveData<String> mPrefMessageListCompleted = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mPrefUseDynamicColors = new MutableLiveData<>();
    private final MutableLiveData<NightMode> mPrefNightMode = new MutableLiveData<>();

    public static synchronized PreferencesRepository getInstance(
            @NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new PreferencesRepository(context);
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
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mKeyMessageListDeleted = context.getResources().getString(R.string.key_complete_msg);
        mKeyUseDynamicColors = context.getResources().getString(R.string.key_use_dynamic_colors);
        mKeyNightMode = context.getResources().getString(R.string.key_night_mode);
        mKeyFirstStartup = context.getResources().getString(R.string.key_first_startup);
        mKeyOnboardingCompleted = context.getResources().getString(R.string.key_onboarding_completed);

        // Apply the default values from the xml, because the SharedPreferences won't be initialized
        // until the SettingsActivity is started. Parameter "readAgain" is set to "true", so that
        // default values will be applied even if this method has been called in the past (so that
        // newly added preferences will get their default value applied). This won't override the
        // preferences after the user changed them.
        PreferenceManager.setDefaultValues(context, PREFS_RES_ID, true);

        // We don't need to worry about the default values, because
        // PreferenceManager.setDefaultValues() has been called already.
        // TODO: use postValue()?
        mPrefMessageListCompleted.setValue(
                mSharedPreferences.getString(mKeyMessageListDeleted, null));
        mPrefUseDynamicColors.setValue(
                mSharedPreferences.getBoolean(mKeyUseDynamicColors, false));
        mPrefNightMode.setValue(
                NightMode.fromPrefEntryValue(mSharedPreferences.getString(mKeyNightMode, "")));

        // We cannot use anonymous inner class, because the PreferenceManager does not store a
        // strong reference to the listener and it would be destroyed by the garbage collector.
        if (mPreferenceChangeListener != null) {
            throw new RuntimeException("mPreferenceChangeListener != null");
        }

        mPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key != null)
            {
                // TODO: use postValue()?
                if (key.contentEquals(mKeyMessageListDeleted)) {
                    mPrefMessageListCompleted.setValue(
                            sharedPreferences.getString(key, null));
                } else if (key.contentEquals(mKeyUseDynamicColors)) {
                    mPrefUseDynamicColors.setValue(
                            sharedPreferences.getBoolean(key, false));
                } else if (key.contentEquals(mKeyNightMode)) {
                    mPrefNightMode.setValue(
                            NightMode.fromPrefEntryValue(sharedPreferences.getString(key, "")));
                }
            }
        };
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

        if (BuildConfig.DEBUG) {
            DBG_PRETEND_FIRST_STARTUP = mSharedPreferences.getBoolean("dbg_first_startup", false);
            DBG_SHOW_INCIDENCE = mSharedPreferences.getBoolean("dbg_show_incidence", false);
            DBG_SHOW_TRASH = mSharedPreferences.getBoolean("dbg_show_trash", false);
        }
    }
}
