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

    private static PreferencesRepository INSTANCE;
    private static final String TAG = "PreferencesRepository";

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private final MutableLiveData<String> mPrefMessageListCompleted = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mPrefUseDynamicColors = new MutableLiveData<>();


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

    private PreferencesRepository(@NonNull Context context) {
        Log.d(TAG, "PreferencesRepository: Ctor " + context);

        String keyMessageListDeleted = context.getResources().getString(R.string.key_complete_msg);
        String keyUseDynamicColors = context.getResources().getString(R.string.key_use_dynamic_colors);

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

        // Register a change listener.
        // Cannot use anonymous inner class, because the PreferenceManager
        // does not store a strong reference to the listener and it
        // would be garbage collected.
        preferenceChangeListener = (sharedPreferences, key) -> {
            // TODO: use postValue()?
            Log.d(TAG, "PreferencesRepository: onChangeListener" + key);
            if (key == null) {
                Log.w(TAG, "key == null");
            }else if (key.contentEquals(keyMessageListDeleted)) {
                mPrefMessageListCompleted.setValue(sharedPreferences.getString(key, null));
            }else if (key.contentEquals(keyUseDynamicColors)) {
                mPrefUseDynamicColors.setValue(sharedPreferences.getBoolean(key, false));
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}
