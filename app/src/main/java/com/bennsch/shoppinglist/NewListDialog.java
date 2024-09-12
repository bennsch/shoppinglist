package com.bennsch.shoppinglist;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.bennsch.shoppinglist.databinding.NewListDialogBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;


public class NewListDialog extends DialogFragment {

    private static final String TAG = "NewListDialog";

    public interface DialogListener{
        void onCreateListClicked(String title);
        void onCancelClicked();
    }

    private DialogListener listener;
    private List<String> currentLists;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // TODO: don't scroll ChecklistItems if IME is displayed

        NewListDialogBinding binding = NewListDialogBinding.inflate(requireActivity().getLayoutInflater());
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
        builder.setView(binding.getRoot())
                .setTitle("Create a new list")
                .setMessage("Please enter the name of your list")
                .setPositiveButton("Create", (dialog, which) -> {
                    String title = binding.listTitle.getText().toString();
                    if (currentLists.contains(title)) {
                        // TODO: Color Dialog's EditText red if list already exists and
                        //  don't let user press "Create" button.
                        Toast.makeText(getContext(), title + " already exists.", Toast.LENGTH_SHORT).show();
                    } else {
                        listener.onCreateListClicked(title);
                    }})
                .setNegativeButton("Cancel", (dialog, id) -> listener.onCancelClicked());
        AlertDialog dialog = builder.create();

        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.listTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String titleValidated = viewModel.validateNewChecklistName(s.toString());
                    // TODO: This would recursively call afterTextChanged()
                    // binding.listTitle.setText(titleValidated);
                    binding.listTitle.setError(null);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } catch (IllegalArgumentException e) {
                    binding.listTitle.setError(e.getMessage());
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        binding.listTitle.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }


    // TODO: try to change "DarkTheme" while dialog is open, it will recreate the Dialog and crash.
    //  so use Factory for NewListDialog fragment
    public void setCurrentLists(List<String> titles) {
        currentLists = titles;
    }

    public void setDialogListener(DialogListener listener) {
        this.listener = listener;
    }
}
