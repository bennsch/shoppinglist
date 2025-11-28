package com.bennsch.shoppinglist.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bennsch.shoppinglist.databinding.DialogNewListBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;


public class NewListDialog extends DialogFragment {

    private static final String TAG = "NewListDialog";

    // Using interface, because we cannot override DialogFragment constructor
    // (Fragment gets recreated on e.g. screen rotation and arguments would be lost)
    // Using "onAttach()" recommended by API doc.
    public interface DialogListener{
        void newListDialog_onCreateClicked(String title);
        String newListDialog_onValidateTitle(String title);
    }

    DialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (DialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface. Throw exception.
            throw new ClassCastException(requireActivity()
                    + " must implement NoticeDialogListener");
        }

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // TODO: don't scroll ChecklistItems if IME is displayed
        // TODO: "Create" button will be disabled after screen is rotated
        DialogNewListBinding binding = DialogNewListBinding.inflate(requireActivity().getLayoutInflater());

        // MaterialAlertDialogBuilder is not scaled properly on smaller screens.
        // AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot())
                .setTitle("Create a new list")
                .setMessage("Please enter the name of your list")
                .setPositiveButton("Create", (dialog, which) -> {
                    listener.newListDialog_onCreateClicked(
                            Objects.requireNonNull(
                                    binding.listTitle.getText()).toString());
                })
                .setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();

        binding.listTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String validationFailedReason = listener.newListDialog_onValidateTitle(s.toString());
                if (validationFailedReason == null) {
                    binding.listTitle.setError(null);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    binding.listTitle.setError(validationFailedReason);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        dialog.setOnShowListener(dialog1 -> {
            // Disable positive button when dialog is first opened, so TextChangedListener
            // decides to enable/disable the button when user enters the first character.
            ((AlertDialog) dialog1).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        });
        // Focus on EditText
        binding.listTitle.requestFocus();
        // TODO: Use IMEHelper here?
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }
}
