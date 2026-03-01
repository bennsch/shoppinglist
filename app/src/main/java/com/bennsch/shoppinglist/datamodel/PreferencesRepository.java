package com.bennsch.shoppinglist.datamodel;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.bennsch.shoppinglist.BuildConfig;
import com.bennsch.shoppinglist.R;

import java.util.Objects;


public class PreferencesRepository {
    /*
     *  A Repository class abstracts access to multiple data sources. Currently only one data source
     *  is implemented (SharedPreferences).
     */

    public enum NightMode {
        ENABLED,
        DISABLED,
        FOLLOW_SYSTEM;
    }

    public static final int PREFS_RES_ID = R.xml.preference_screen;

    // Will be overwritten in debug build:
    public static boolean DBG_PRETEND_FIRST_STARTUP = false;
    public static boolean DBG_SHOW_INCIDENCE = false;
    public static boolean DBG_SHOW_TRASH = false;

    private static PreferencesRepository INSTANCE;
    private final SharedPreferences mSharedPreferences;

    // We cannot use anonymous inner class but instead use static variable, because the
    // PreferenceManager does not store a strong reference to the listener, and it would be
    // destroyed by the garbage collector.
    private static SharedPreferences.OnSharedPreferenceChangeListener
            mPreferenceChangeListener = null;
    // TODO: Why does using an instance variable won't work in release build? Instance variables are
    //  strong references. Maybe try to keep SharedPreferences object as instance variable?

    // Create corresponding LiveData objects for each preference.
    private final MutableLiveData<String> mPrefCompletedMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mPrefUseDynamicColors = new MutableLiveData<>();
    private final MutableLiveData<NightMode> mPrefNightMode = new MutableLiveData<>();
    private final MutableLiveData<Float> mPrefItemFontSize = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mPrefFirstStartup = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mPrefOnboardingCompleted = new MutableLiveData<>();

    // Store some of the keys (from resources) used in the setter methods
    private final String mKeyFirstStartup;
    private final String mKeyOnboardingComplete;

    public static synchronized PreferencesRepository getInstance(
            @NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new PreferencesRepository(context);
        }
        return INSTANCE;
    }

    public LiveData<String> getPrefMessageListDeleted() {
        return mPrefCompletedMessage;
    }

    public LiveData<Boolean> getPrefUseDynamicColors() {
        return mPrefUseDynamicColors;
    }

    public LiveData<NightMode> getPrefNightMode() {
        return mPrefNightMode;
    }

    public LiveData<Float> getPrefItemFontSize() {
        // Font size in pixels
        return mPrefItemFontSize;
    }

    public LiveData<Boolean> getPrefFirstStartup() {
        return mPrefFirstStartup;
    }

    public LiveData<Boolean> getPrefOnboardingCompleted() {
        return mPrefOnboardingCompleted;
    }

    // The setter methods will trigger the OnSharedPreferenceChangeListener which updates the
    // corresponding LiveData object.

    public void setPrefFirstStartup(boolean firstStartup) {
        mSharedPreferences.edit().putBoolean(mKeyFirstStartup, firstStartup).apply();
    }

    public void setPrefOnboardingCompleted(boolean completed) {
        mSharedPreferences.edit().putBoolean(mKeyOnboardingComplete, completed).apply();
    }

    private PreferencesRepository(@NonNull Context context) {
        mKeyFirstStartup = context.getResources().getString(R.string.pref_key_first_startup);
        mKeyOnboardingComplete = context.getResources().getString(R.string.pref_key_onboarding_completed);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Apply the default values from the XML, because the SharedPreferences won't be initialized
        // until the SettingsActivity is started. Parameter "readAgain" is set to "true", so that
        // default values will be applied even if this method has been called in the past (so that
        // newly added preferences will get their default value applied). This won't override the
        // preferences after the user changed them.
        PreferenceManager.setDefaultValues(context, PREFS_RES_ID, true);

        // Register a listener to update the LiveData whenever a preference is changed.
        if (mPreferenceChangeListener == null) {
            mPreferenceChangeListener = (sharedPreferences, key) -> {
                // We don't need to worry about the default values, because
                // PreferenceManager.setDefaultValues() has been called already at this point.
                if (Objects.equals(key, context.getResources().getString(
                        R.string.pref_key_placeholder_unchecked))) {
                    mPrefCompletedMessage.setValue(sharedPreferences.getString(key, null));
                } else if (Objects.equals(key, context.getResources().getString(
                        R.string.pref_key_use_dynamic_colors))) {
                    mPrefUseDynamicColors.setValue(sharedPreferences.getBoolean(key, false));
                } else if (Objects.equals(key, context.getResources().getString(
                        R.string.pref_key_onboarding_completed))) {
                    mPrefOnboardingCompleted.setValue(sharedPreferences.getBoolean(key, false));
                } else if (Objects.equals(key, context.getResources().getString(
                        R.string.pref_key_first_startup))) {
                    mPrefFirstStartup.setValue(sharedPreferences.getBoolean(key, false));
                } else if (Objects.equals(key, context.getResources().getString(
                        R.string.pref_key_nightmode))) {
                    String entryValue = sharedPreferences.getString(key, null);
                    if (Objects.equals(entryValue, context.getResources().getString(
                            R.string.pref_entry_value_nightmode_disabled))) {
                        mPrefNightMode.setValue(NightMode.DISABLED);
                    } else if (Objects.equals(entryValue, context.getResources().getString(
                            R.string.pref_entry_value_nightmode_enabled))) {
                        mPrefNightMode.setValue(NightMode.ENABLED);
                    } else if (Objects.equals(entryValue, context.getResources().getString(
                            R.string.pref_entry_value_nightmode_follow_system))) {
                        mPrefNightMode.setValue(NightMode.FOLLOW_SYSTEM);
                    } else {
                        // TODO: Add error log to all "assert" statements in this project
                        assert false : "Unknown entry value: " + entryValue;
                    }
                } else if (Objects.equals(key, context.getResources().getString(
                        R.string.pref_key_item_font_size))) {
                    String entryValue = sharedPreferences.getString(key, null);
                    if (Objects.equals(entryValue, context.getResources().getString(
                            R.string.pref_entry_value_item_font_size_small))) {
                        mPrefItemFontSize.setValue(
                                context.getResources().getDimension(
                                        R.dimen.item_viewholder_font_size_small));
                    } else if (Objects.equals(entryValue, context.getResources().getString(
                            R.string.pref_entry_value_item_font_size_medium))) {
                        mPrefItemFontSize.setValue(
                                context.getResources().getDimension(
                                        R.dimen.item_viewholder_font_size_medium));
                    } else if (Objects.equals(entryValue, context.getResources().getString(
                            R.string.pref_entry_value_item_font_size_large))) {
                        mPrefItemFontSize.setValue(
                                context.getResources().getDimension(
                                        R.dimen.item_viewholder_font_size_large));
                    } else {
                        // TODO: Add error log to all "assert" statements in this project
                        assert false : "Unknown entry value: " + entryValue;
                    }
                // FOR DEBUGGING ONLY:
                } else if (BuildConfig.DEBUG) {
                    if (Objects.equals(key, context.getResources().getString(
                            R.string.pref_key_dbg_pretend_first_startup))) {
                        DBG_PRETEND_FIRST_STARTUP = sharedPreferences.getBoolean(key, false);
                    } else if (Objects.equals(key, context.getResources().getString(
                            R.string.pref_key_dbg_show_incidence))) {
                        DBG_SHOW_INCIDENCE = sharedPreferences.getBoolean(key, false);
                    } else if (Objects.equals(key, context.getResources().getString(
                            R.string.pref_key_dbg_show_trash))) {
                        DBG_SHOW_TRASH = sharedPreferences.getBoolean(key, false);
                    } else {
                        assert false: "Unknown debug key: " + key;
                    }
                } else {
                    assert false: "Unknown key: " + key;
                }
            };
        } else {
            assert false: "mPreferenceChangeListener != null";
        }
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

        // Trigger the listener for each preference to initialize their respective LiveData objects.
        mSharedPreferences.getAll().keySet().forEach(
                key -> mPreferenceChangeListener.onSharedPreferenceChanged(mSharedPreferences, key));
    }
}
