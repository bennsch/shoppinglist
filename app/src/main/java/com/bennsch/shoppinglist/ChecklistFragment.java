package com.bennsch.shoppinglist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bennsch.shoppinglist.databinding.ChecklistItemViewholderBinding;
import com.bennsch.shoppinglist.databinding.FragmentChecklistBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ChecklistFragment extends Fragment {

    private static final String TAG = "ChecklistFragment";
    private static final String ARG_LIST_TITLE = "list_title";
    private static final String ARG_DISPLAY_CHECKED = "display_checked";


    private FragmentChecklistBinding mBinding;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private boolean mDisplayChecked;
    private String mListTitle;
    private AppViewModel mViewModel;

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
        mViewModel = new ViewModelProvider(this).get(AppViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentChecklistBinding.inflate(inflater, container, false);
        mBinding.recyclerView.setAdapter(mRecyclerViewAdapter);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        Log.d(TAG, "onCreateView: " + getViewLifecycleOwner());
        mViewModel.getItemsSortedByPosition(mListTitle, mDisplayChecked)
                .observe(getViewLifecycleOwner(), this::onLiveDataChanged);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void scrollTo(boolean bottom) {
        // TODO: Limit scrolling time (calculate scroll distance and then adjust scrolling speed
        //  so that it will always take the same amount of time.
        int pos;
        if (bottom) {
            pos = (mRecyclerViewAdapter.getItemCount() > 0) ? (mRecyclerViewAdapter.getItemCount() - 1) : 0;
        } else {
            pos = 0;
        }
        mBinding.recyclerView.smoothScrollToPosition(pos);
    }

    protected void onLiveDataChanged(List<ChecklistItem> newItemsSorted) {
        Log.d(TAG, "onLiveDataChanged: " + mListTitle + "(" + (mDisplayChecked ? "Checked Items" : "Unchecked Items" + ")"));
        mRecyclerViewAdapter.updateItems(newItemsSorted);
    }


    // TODO: 3/12/2024 Use ChecklistItem as parameter
    private void onItemClicked(int adapterPosition) {
        Log.d(TAG, "onItemClicked: " + adapterPosition);
        ChecklistItem item = mRecyclerViewAdapter.getCachedItem(adapterPosition);
        mViewModel.flipItem(mListTitle, item);
    }

    protected void onItemsMoved(List<ChecklistItem> itemsSortedByPosition) {
        mViewModel.itemsHaveBeenMoved(mListTitle, itemsSortedByPosition);
    }

    // TODO: move to separate file?
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private List<ChecklistItem> mCachedItems = new ArrayList<>();
        private ItemTouchHelper mItemTouchHelper;

        @NonNull
        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ChecklistItemViewholderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChecklistItem item = mCachedItems.get(position);
            holder.getBinding().textView.setText(item.getName());
            if (mCachedItems.get(position).isChecked()) {
                holder.getBinding().textView.setTextAppearance(R.style.ChecklistItem_Checked);
                holder.getBinding().textView.setBackgroundResource(R.drawable.strike_through);
            } else {
                holder.getBinding().textView.setTextAppearance(R.style.ChecklistItem_Unchecked);
            }
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mItemTouchHelper = new ItemTouchHelper(
                    new ItemTouchHelper.SimpleCallback(
                            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return onItemMove(viewHolder, target);
                }

                @Override
                public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        onMoveCompleted();
                    }
                }

                @Override
                public boolean isLongPressDragEnabled() {
                    return false;
                }

                        @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
            });
            mItemTouchHelper.attachToRecyclerView(recyclerView);
        }

        @Override
        public int getItemCount() {
            return mCachedItems.size();
        }

        public void updateItems(List<ChecklistItem> newItems) {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new DiffCallback(getCachedItems(), newItems));
            mCachedItems = newItems;
            // will trigger the appropriate animations
            diffResult.dispatchUpdatesTo(new ListUpdateCallback() {
                @Override
                public void onInserted(int position, int count) {
                    notifyItemRangeInserted(position, count);
                    if (count == 1) {
                        // If a single item has been added, scroll to it.
                        mBinding.recyclerView.smoothScrollToPosition(position);
                    }
                    // TODO: highlight the new item (e.g. flash animation)?
                }

                @Override
                public void onRemoved(int position, int count) {
                    notifyItemRangeRemoved(position, count);
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {
                    notifyItemMoved(fromPosition, toPosition);
                }

                @Override
                public void onChanged(int position, int count, @Nullable Object payload) {
                    notifyItemRangeChanged(position, count, payload);
                }
            });
        }

        public boolean onItemMove(@NonNull RecyclerView.ViewHolder itemFrom,
                                  @NonNull RecyclerView.ViewHolder itemTo) {
            int from = itemFrom.getAdapterPosition();
            int to = itemTo.getAdapterPosition();

            if ((from == RecyclerView.NO_POSITION) ||
                    (to == RecyclerView.NO_POSITION)) {
                Log.w(TAG, "Item move ignored");
                return false;
            } else {

                // We need to update cachedItems, otherwise DiffUtil.calculateDiff in update()
                // would not work properly.
                // Basically, the newItems will already match the cachedItems when the
                // ViewModel has updated the database and calls the callback, so it is
                // redundant. But updating the cachedItems ensures that the RecyclerView
                // visually matches it's underlying data (cachedItems). So any subsequent
                // moves will result in correct calls to  update the ViewModel.
                if (from < to) {
                    for (int i = from; i < to; i++) {
                        Collections.swap(mCachedItems, i, i + 1);
                    }
                } else {
                    for (int i = from; i > to; i--) {
                        Collections.swap(mCachedItems, i, i - 1);
                    }
                }
                // Animate the move (visually move the items)
                notifyItemMoved(from, to);
                return true;
            }
        }

        public void onMoveCompleted() {
            onItemsMoved(mCachedItems);
        }

        public ChecklistItem getCachedItem(int pos) {
            return mCachedItems.get(pos);
        }

        public List<ChecklistItem> getCachedItems() {
            return mCachedItems;
        }

        public boolean onDragHandleTouch(ViewHolder dragHandle) {
            mItemTouchHelper.startDrag(dragHandle);
            return true; // true if the listener has consumed the event
        }


        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            private final ChecklistItemViewholderBinding mBinding;

            public ViewHolder(@NonNull ChecklistItemViewholderBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
                mBinding.viewholderClickable.setOnClickListener(this);
                mBinding.dragHandle.setOnTouchListener((view, motionEvent) -> onDragHandleTouch(this));
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
                return oldItem.getName().equals(newItem.getName());
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