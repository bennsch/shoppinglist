package com.bennsch.shoppinglist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bennsch.shoppinglist.databinding.FragmentNoListsBinding;


public class NoListsFragment extends Fragment {
    /*
     *  Fragment to be displayed when no Checklist is present.
     */

    public static final String REQ_KEY_NEW_LIST_BUTTON_CLICKED = "NoListsFragment_new_list_button_clicked";

    public NoListsFragment() {
        // Required empty public constructor.
    }

    public static NoListsFragment newInstance() {
        return new NoListsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentNoListsBinding binding = FragmentNoListsBinding.inflate(inflater, container, false);
        binding.buttonNewList.setOnClickListener(
                v -> getParentFragmentManager()
                        .setFragmentResult(REQ_KEY_NEW_LIST_BUTTON_CLICKED, new Bundle()));
        return binding.getRoot();
    }
}