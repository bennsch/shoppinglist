package com.bennsch.shoppinglist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.bennsch.shoppinglist.datamodel.ChecklistRepository;
import com.bennsch.shoppinglist.datamodel.DbChecklistItem;
import com.bennsch.shoppinglist.datamodel.PreferencesRepository;
import com.bennsch.shoppinglist.databinding.SettingsActivityBinding;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.Executors;


public class SettingsActivity extends AppCompatActivity {
    /*
     *  Dedicated activity to display and modify app settings.
     */

    private SettingsActivityBinding mBinding;
    private ActivityResultLauncher<Intent> mCsvFileLauncher;

    private static final ListeningExecutorService mListeningExecutor =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());


    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Populate preferences from xml.
            setPreferencesFromResource(PreferencesRepository.PREFS_RES_ID, rootKey);
            // Register OnClickListener for "Export to CSV" preference.
            Preference prefExportCsv = findPreference(getString(R.string.key_export_csv));
            if (prefExportCsv != null) {
                prefExportCsv.setOnPreferenceClickListener(preference -> {
                    SettingsActivity parent = (SettingsActivity)getActivity();
                    assert parent != null;
                    parent.onExportCsvClicked();
                    return true; // Return true if the click was handled.
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        // LifecycleOwners must call register*() before they are STARTED (e.g. during onCreate()).
        mCsvFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onCsvFileResult
        );
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_fragment_container, new SettingsFragment())
                    .commit();
        }
        setupActionBar();
        setupEdgeToEdge();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onExportCsvClicked() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "shopping_list.csv");
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        mCsvFileLauncher.launch(intent);
    }

    private void onCsvFileResult(ActivityResult result) {
        int resultCode = result.getResultCode();
        if (resultCode == RESULT_OK) {
            Intent intent = result.getData();
            if (intent == null) {
                Toast.makeText(this, "ERROR: intent == null", Toast.LENGTH_LONG).show();
            } else{
                Uri uri = intent.getData();
                if (uri == null) {
                    Toast.makeText(this, "ERROR: uri == null", Toast.LENGTH_LONG).show();
                }else{
                    exportToCsv(uri);
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "ERROR: Unexpected result code " + resultCode, Toast.LENGTH_LONG).show();
        }
    }

    private void exportToCsv(@NonNull Uri csvFileUri) {
        // TODO: Add progress dialog
        ListenableFuture<Void> result =  mListeningExecutor.submit(() -> {
            // Exceptions will be caught by ListenableFuture.
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            getContentResolver()
                                    .openOutputStream(csvFileUri)))) {
                ChecklistRepository repo = ChecklistRepository.getInstance(getApplicationContext());
                List<DbChecklistItem> items = repo.getAllItems();
                writer.write("List;Name;Incidence;Checked"); writer.newLine();
                for (DbChecklistItem item : items) {
                    writer.write(
                            item.getBelongsToChecklist() + ";" +
                            item.getName() + ";" +
                            item.getIncidence() + ";" +
                            item.isChecked()
                    );
                    writer.newLine();
                }
                return null;
            }
        });
        Futures.addCallback(result, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(getApplicationContext(), "File written successfully", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Toast.makeText(getApplicationContext(), "ERROR: " + t, Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupActionBar() {
        setSupportActionBar(mBinding.settingsToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupEdgeToEdge() {
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(
                mBinding.settingsFragmentContainer,
                (v, insets) -> {
                    Insets insetsNormal = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
                    mBinding.getRoot().setPadding(0, insetsNormal.top, 0, insetsNormal.bottom);
                    return insets;
                    // return WindowInsetsCompat.CONSUMED;
                });
    }
}