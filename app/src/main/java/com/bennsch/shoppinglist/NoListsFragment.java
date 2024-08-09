package com.bennsch.shoppinglist;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bennsch.shoppinglist.databinding.FragmentChecklistPagerBinding;
import com.bennsch.shoppinglist.databinding.FragmentNoListsBinding;

public class NoListsFragment extends Fragment {

    public static final String REQ_KEY_NEW_LIST_BUTTON_CLICKED = "NoListsFragment_new_list_button_clicked";

    public NoListsFragment() {
        // Required empty public constructor
    }

    public static NoListsFragment newInstance() {
        return new NoListsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentNoListsBinding binding = FragmentNoListsBinding.inflate(inflater, container, false);
        binding.buttonNewList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().setFragmentResult(REQ_KEY_NEW_LIST_BUTTON_CLICKED, new Bundle());
            }
        });
        return binding.getRoot();
    }
}