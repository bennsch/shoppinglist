package com.bennsch.shoppinglist.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bennsch.shoppinglist.R;
import com.bennsch.shoppinglist.databinding.DialogAboutBinding;


public class AboutDialog extends DialogFragment {
    /*
     *  Dialog to display general information about the app.
     */

    private static final String ARG_VERSION_NAME = "version_name";


    public static AboutDialog newInstance(String versionName) {
        Bundle args = new Bundle();
        args.putString(ARG_VERSION_NAME, versionName);
        AboutDialog fragment = new AboutDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        assert args != null;
        String versionName = getArguments().getString(ARG_VERSION_NAME);
        assert versionName != null;

        @NonNull DialogAboutBinding binding = DialogAboutBinding.inflate(
                requireActivity().getLayoutInflater());
        binding.aboutVersion.setText(getString(R.string.dialog_about_version, versionName));

        // AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        return builder
                .setView(binding.getRoot())
                .setNegativeButton(getString(R.string.dialog_close), null)
                .create();
    }
}
