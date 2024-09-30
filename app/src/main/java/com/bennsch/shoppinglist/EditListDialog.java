package com.bennsch.shoppinglist;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.bennsch.shoppinglist.databinding.DialogEditListBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;


public class EditListDialog extends DialogFragment {

    private static final String TAG = "EditListDialog";

    // Using interface, because we cannot override DialogFragment constructor
    // (Fragment gets recreated on e.g. screen rotation and arguments would be lost)
    // Using "onAttach()" recommended by API doc.
    public interface DialogListener{
        void editListDialog_onSafeClicked(String oldTitle, String newTitle);
        void editListDialog_onDeleteClicked(DialogFragment dialogFragment);
        String editListDialog_onValidateTitle(String title) throws Exception;
        String editListDialog_getTitle();
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
        DialogEditListBinding binding = DialogEditListBinding.inflate(requireActivity().getLayoutInflater());

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
        builder.setView(binding.getRoot())
                .setTitle("Edit")
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete List", null) // OnClickListener set in onShow()
                .setPositiveButton("Save", (dialog, which) -> {
                    listener.editListDialog_onSafeClicked(
                            listener.editListDialog_getTitle(),
                            Objects.requireNonNull(binding.listTitle.getText()).toString());
                });
        AlertDialog dialog = builder.create();

        binding.listTitle.setText(listener.editListDialog_getTitle());
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
                    String titleValidated = listener.editListDialog_onValidateTitle(s.toString());
                    // TODO: This would recursively call afterTextChanged()
                    //  binding.listTitle.setText(titleValidated);
                    binding.listTitle.setError(null);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }catch (Exception e){
                    binding.listTitle.setError(e.getMessage());
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        dialog.setOnShowListener(dialog1 -> {
            // Disable positive button when dialog is first opened, so TextChangedListener
            // decides to enable/disable the button when user enters the first character.
            ((AlertDialog) dialog1).getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(false);
            // Add listener here to prevent the dialog from closing when the button is pressed.
            ((AlertDialog) dialog1).getButton(DialogInterface.BUTTON_NEUTRAL)
                    .setOnClickListener(v -> listener.editListDialog_onDeleteClicked(this));
            // Change color of "Delete" button.
            TypedValue typedValue = new TypedValue();
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, typedValue, true);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setTextColor(
                            ContextCompat.getColor(
                                    getContext(), typedValue.resourceId));


        });
        // Focus on EditText
        binding.listTitle.requestFocus();
        // TODO: Use IMEHelper here?
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }
}
