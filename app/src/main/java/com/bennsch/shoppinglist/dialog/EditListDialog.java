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
import com.bennsch.shoppinglist.ThemeHelper;
import com.bennsch.shoppinglist.databinding.DialogEditListBinding;

import java.util.Objects;


public class EditListDialog extends DialogFragment {

    private static final String ARG_LIST_TITLE = "list_title";

    // Using interface, because we cannot override DialogFragment constructor
    // (Fragment gets recreated on e.g. screen rotation and arguments would be lost)
    // Using "onAttach()" is recommended by API doc.
    public interface DialogListener{
        void editListDialog_onSafeClicked(String oldTitle, String newTitle);
        void editListDialog_onDeleteClicked(String listTitle);
        void editListDialog_onValidateTitle(String title) throws Exception;
    }

    private DialogListener mListener;
    private AlertDialog mConfirmationDialog;

    public static EditListDialog newInstance(String listTitle) {
        Bundle args = new Bundle();
        args.putString(ARG_LIST_TITLE, listTitle);
        EditListDialog fragment = new EditListDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (DialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface. Throw exception.
            throw new ClassCastException(requireActivity()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mConfirmationDialog != null) {
            // Avoid leaking the dialog window if e.g. the screen is rotated.
            mConfirmationDialog.dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // TODO: don't scroll ChecklistItems if IME is displayed

        Bundle args = getArguments();
        assert args != null;
        String listTitle = getArguments().getString(ARG_LIST_TITLE);
        assert listTitle != null;

        DialogEditListBinding binding = DialogEditListBinding.inflate(requireActivity().getLayoutInflater());

        // AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot())
                .setTitle("Edit")
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete List", null) // OnClickListener is set in onShow() callback
                .setPositiveButton("Save", (dialog, which) ->
                        mListener.editListDialog_onSafeClicked(
                                listTitle,
                                Objects.requireNonNull(binding.listTitle.getText()).toString()));
        AlertDialog dialog = builder.create();
        binding.listTitle.setText(listTitle);
        binding.listTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mListener.editListDialog_onValidateTitle(s.toString());
                    binding.listTitle.setError(null);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }catch (Exception e){
                    binding.listTitle.setError(e.getMessage());
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        dialog.setOnShowListener(dlg -> {
            // Disable positive button when dialog is first opened, so TextChangedListener
            // decides to enable/disable the button when user enters the first character.
            ((AlertDialog) dlg).getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(false);
            // Add listener here to prevent the dialog from closing when the button is pressed.
            ((AlertDialog) dlg)
                    .getButton(DialogInterface.BUTTON_NEUTRAL)
                    .setOnClickListener(v ->
                            showConfirmationDialog(listTitle));
            // Change color of the "Delete" button.
            Context context = getContext();
            assert context != null: "getContext() returned null";
            dialog
                .getButton(DialogInterface.BUTTON_NEUTRAL)
                .setTextColor(ThemeHelper.getColor(android.R.attr.colorError, context));
        });
        // Focus on EditText and show IME.
        binding.listTitle.requestFocus();
        IMEHelper.showIME(dialog);
        return dialog;
    }

    private void showConfirmationDialog(String listTitle) {
        Context context = getContext();
        assert context != null: "getContext() returned null";
        // MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete List")
                .setMessage("Are you sure to delete \"" + listTitle + "\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    mListener.editListDialog_onDeleteClicked(listTitle);
                    dismiss(); // Close the dialog.
                });
        mConfirmationDialog = builder.create();
        // Change color of the "Delete" button.
        mConfirmationDialog.setOnShowListener(
                dialog -> mConfirmationDialog
                        .getButton(DialogInterface.BUTTON_POSITIVE)
                        .setTextColor(ThemeHelper.getColor(android.R.attr.colorError, context)));
        mConfirmationDialog.show();
    }
}
