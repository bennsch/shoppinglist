package com.example.shoppinglist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.shoppinglist.databinding.FragmentChecklistBinding;
import com.google.android.material.textview.MaterialTextView;


public class ChecklistFragment extends Fragment {

    private static final String ARG_DISPLAY_CHECKED = "display_checked";


    private FragmentChecklistBinding mBinding;
    private boolean mDisplayChecked;

    public ChecklistFragment() {
        // Required empty public constructor
    }

    public static ChecklistFragment newInstance(boolean displayChecked) {
        ChecklistFragment fragment = new ChecklistFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_DISPLAY_CHECKED, displayChecked);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDisplayChecked = getArguments().getBoolean(ARG_DISPLAY_CHECKED);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentChecklistBinding.inflate(inflater, container, false);

        for (int i = 0; i < (mDisplayChecked ? 5 : 25); i++) {
            MaterialTextView view = new MaterialTextView(getContext());
            view.setText("Item " + i);
            view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35);
            mBinding.checklistItems.addView(view);
        }

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}