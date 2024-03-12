package com.example.shoppinglist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.shoppinglist.databinding.ChecklistItemViewholderBinding;
import com.example.shoppinglist.databinding.FragmentChecklistBinding;

import java.util.ArrayList;
import java.util.List;


public class ChecklistFragment extends Fragment {

    private static final String TAG = "ChecklistFragment";
    private static final String ARG_LIST_TITLE = "list_title";
    private static final String ARG_DISPLAY_CHECKED = "display_checked";


    private FragmentChecklistBinding mBinding;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private boolean mDisplayChecked;
    private String mListTitle;
    private ChecklistViewModel mViewModel;

    public ChecklistFragment() {
        Log.d(TAG, "ChecklistFragment: Ctor");
        // Required empty public constructor
    }

    public static ChecklistFragment newInstance(String listTitle, boolean displayChecked) {
        Log.d(TAG, "newInstance: ");
        ChecklistFragment fragment = new ChecklistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LIST_TITLE, listTitle);
        args.putBoolean(ARG_DISPLAY_CHECKED, displayChecked);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        mListTitle = getArguments().getString(ARG_LIST_TITLE);
        mDisplayChecked = getArguments().getBoolean(ARG_DISPLAY_CHECKED);
        mRecyclerViewAdapter = new RecyclerViewAdapter();
        mViewModel = new ViewModelProvider(this).get(ChecklistViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentChecklistBinding.inflate(inflater, container, false);
        mBinding.recyclerView.setAdapter(mRecyclerViewAdapter);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        Log.d(TAG, "onCreateView: " + getViewLifecycleOwner());
        mViewModel.getFilteredList(mListTitle, mDisplayChecked)
                .observe(getViewLifecycleOwner(), this::onItemsChanged);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void onItemClicked(int adapterPosition) {
        Log.d(TAG, "onItemClicked: " + adapterPosition);
        ChecklistItem item = mRecyclerViewAdapter.getItem(adapterPosition);
        mViewModel.flipChecked(item.getUid());
    }

    protected void onItemsChanged(List<ChecklistItem> newItems) {
        // The newItems are already sorted by position
        Log.d(TAG, "onItemSubsetChanged: " + mListTitle + "(" + (mDisplayChecked ? "Checked Items" : "Unchecked Items" + ")"));
        mRecyclerViewAdapter.update(newItems);
    }


    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private List<ChecklistItem> mCachedItems = new ArrayList<>();

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

        public void update(List<ChecklistItem> newItems) {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new DiffCallback(getItems(), newItems));
            mCachedItems = newItems;
            // will trigger the appropriate animations
            diffResult.dispatchUpdatesTo(this);
        }

        public ChecklistItem getItem(int pos) {
            return mCachedItems.get(pos);
        }

        public List<ChecklistItem> getItems() {
            return mCachedItems;
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

        class DiffCallback extends DiffUtil.Callback {

            private final List<ChecklistItem> mOldList;
            private final List<ChecklistItem> mNewList;

            public DiffCallback(List<ChecklistItem> oldList, List<ChecklistItem> newList) {
                this.mOldList = oldList;
                this.mNewList = newList;
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                return super.getChangePayload(oldItemPosition, newItemPosition);
            }

            @Override
            public int getOldListSize() {
                return mOldList.size();
            }

            @Override
            public int getNewListSize() {
                return mNewList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                //Called to check whether two objects represent the same item.
                final ChecklistItem oldItem = mOldList.get(oldItemPosition);
                final ChecklistItem newItem = mNewList.get(newItemPosition);
                return oldItem.getUid().equals(newItem.getUid());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                // If visual representation is same
                // This method is called only if areItemsTheSame returns true for these items.
                final ChecklistItem oldItem = mOldList.get(oldItemPosition);
                final ChecklistItem newItem = mNewList.get(newItemPosition);
                // TODO: 1/15/2024 more than name required here? IsChecked is part of visual representation
                return oldItem.getName().equals(newItem.getName());
            }
        }
    }
}