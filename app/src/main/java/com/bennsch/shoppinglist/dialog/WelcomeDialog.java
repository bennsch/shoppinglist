package com.bennsch.shoppinglist.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bennsch.shoppinglist.databinding.DialogWelcomeBinding;


public class WelcomeDialog extends DialogFragment {

    public static WelcomeDialog newInstance() {
        return new WelcomeDialog();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @NonNull DialogWelcomeBinding binding = DialogWelcomeBinding.inflate(
                requireActivity().getLayoutInflater());

//        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        return builder
                .setView(binding.getRoot())
                // TODO: use string resource for "Close"
                .setNegativeButton("Got It", null)
                .create();
    }

}
