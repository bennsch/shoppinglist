package com.bennsch.shoppinglist.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bennsch.shoppinglist.IMEHelper;
import com.bennsch.shoppinglist.databinding.DialogNewListBinding;

import java.util.Objects;


public class NewListDialog extends DialogFragment {
    /*
     *  Dialog to create a new checklist.
     */

    // Using an interface, because we cannot override DialogFragment constructor (Fragment is
    // recreated on e.g. screen rotation and arguments would be lost) Using "onAttach()" is
    // recommended by API doc.
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
            // The activity doesn't implement the interface. Throw an exception.
            throw new ClassCastException(requireActivity() + " must implement NoticeDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // TODO: don't scroll ChecklistItems if IME is displayed
        // TODO: "Create" button will be disabled after screen is rotated
        DialogNewListBinding binding = DialogNewListBinding.inflate(
                requireActivity().getLayoutInflater());

        // MaterialAlertDialogBuilder is not scaled properly on smaller screens.
        // AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot())
                .setTitle("Create a new list")
                .setMessage("Please enter the name of your list")
                .setPositiveButton("Create", (dialog, which) -> {
                    listener.newListDialog_onCreateClicked(
                            Objects.requireNonNull(binding.listTitle.getText()).toString());
                })
                .setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();

        binding.listTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

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

        dialog.setOnShowListener(dlg -> {
            // Disable positive button when dialog is first opened, so that TextChangedListener
            // can enable/disable the button when user enters the first character.
            ((AlertDialog) dlg).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        });
        // Focus on EditText and show IME.
        binding.listTitle.requestFocus();
        IMEHelper.showIME(dialog);
        return dialog;
    }
}
