package com.example.shoppinglist;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.shoppinglist.databinding.ChecklistItemViewholderBinding;
import com.example.shoppinglist.databinding.FragmentChecklistBinding;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;


public class ChecklistFragment extends Fragment {

    private static final String TAG = "ChecklistFragment";
    private static final String ARG_DISPLAY_CHECKED = "display_checked";


    private FragmentChecklistBinding mBinding;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private boolean mDisplayChecked;
    private ChecklistRepository mRepo;

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
        mRecyclerViewAdapter = new RecyclerViewAdapter();
        mRepo = new ChecklistRepository(getActivity().getApplication());

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentChecklistBinding.inflate(inflater, container, false);
        mBinding.recyclerView.setAdapter(mRecyclerViewAdapter);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: 1/21/2024 getViewLifecycleOwner() or getParentFragment().getViewLifecycleOwner() ??
        mRepo.getSubsetSortedByPosition("List A", mDisplayChecked).observe(getViewLifecycleOwner(), this::onItemSubsetChanged);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    private void onItemClicked(int adapterPosition) {
        Log.d(TAG, "onItemClicked: " + adapterPosition);
            ChecklistRepository.Item item = mRecyclerViewAdapter.getItem(adapterPosition);
            mRepo.flipChecked(item);
    }

    protected void onItemSubsetChanged(List<ChecklistRepository.Item> newItems) {
        // The newItems are already sorted by position

//        Log.d(TAG, "onViewModelItemsChanged(): "+ mListTitle + "/" +
//                mDesignation + "(newItems size=" + newItems.size() + ")");

//        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
//                new MyCallback(mAdapter.getCachedItems(), newItems));

//        mAdapter.setCachedItems(newItems);
        // will trigger the appropriate animations
//        diffResult.dispatchUpdatesTo(mAdapter);

        Log.d(TAG, "onItemSubsetChanged: " + (mDisplayChecked ? "Checked Items" : "Unchecked Items"));
        mRecyclerViewAdapter.setItems(newItems);
    }


    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private List<ChecklistRepository.Item> mCachedItems = new ArrayList<>();

        @NonNull
        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ChecklistItemViewholderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.getBinding().textView.setText(mCachedItems.get(position).getName());
            if (mCachedItems.get(position).isChecked()) {
                holder.getBinding().textView.setTextAppearance(R.style.ChecklistItem_Checked);
            } else {
                holder.getBinding().textView.setTextAppearance(R.style.ChecklistItem_Unchecked);
            }
        }

        @Override
        public int getItemCount() {
            return mCachedItems.size();
        }

        public void setItems(List<ChecklistRepository.Item> items) {
            mCachedItems = items;
            notifyDataSetChanged();
        }

        public ChecklistRepository.Item getItem(int pos) {
            return mCachedItems.get(pos);
        }


        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            private final ChecklistItemViewholderBinding mBinding;

            public ViewHolder(@NonNull ChecklistItemViewholderBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
                mBinding.textView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                int adapterPos = getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) {
                    Log.w(TAG, "Clicked item doesn't exist anymore in adapter. Click ignored");
                } else {
                    onItemClicked(adapterPos);
                }
            }

            public ChecklistItemViewholderBinding getBinding() {
                return mBinding;
            }
        }

    }
}