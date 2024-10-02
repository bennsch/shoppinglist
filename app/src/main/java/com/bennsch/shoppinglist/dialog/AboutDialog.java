package com.bennsch.shoppinglist.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.bennsch.shoppinglist.MainViewModel;
import com.bennsch.shoppinglist.databinding.DialogAboutBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class AboutDialog extends DialogFragment {
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @NonNull DialogAboutBinding binding = DialogAboutBinding.inflate(
                requireActivity().getLayoutInflater());

        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        binding.aboutVersion.setText("Version " + viewModel.getVersionName());

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
        return builder
                .setView(binding.getRoot())
                // TODO: use string resource for "Close"
                .setNegativeButton("Close", (dlg, id) -> AboutDialog.this.getDialog().cancel())
                .create();
    }
}
