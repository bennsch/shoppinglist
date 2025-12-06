package com.bennsch.shoppinglist.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bennsch.shoppinglist.databinding.DialogWelcomeBinding;


public class WelcomeDialog extends DialogFragment {
    /*
     *  Dialog to be shown when user launches the app for the first time.
     */

    // Using an interface, because we cannot override DialogFragment constructor (Fragment is
    // recreated on e.g. screen rotation and arguments would be lost) Using "onAttach()" is
    // recommended by API doc.
    private DialogInterface.OnClickListener mOnClickListener = null;

    public static WelcomeDialog newInstance() {
        return new WelcomeDialog();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @NonNull DialogWelcomeBinding binding = DialogWelcomeBinding.inflate(
                requireActivity().getLayoutInflater());

        // MaterialAlertDialogBuilder is not scaled properly on smaller screens.
        // AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        return builder
                .setView(binding.getRoot())
                .setNegativeButton("Got It", mOnClickListener)
                .create();
    }

    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mOnClickListener = listener;
    }
}
