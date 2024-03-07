package com.example.shoppinglist;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.List;
import java.util.stream.Collectors;


public class ChecklistViewModel extends AndroidViewModel {

    private static final String TAG = "ChecklistViewModel";

    private final ChecklistRepository mChecklistRepo;

    public ChecklistViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "ChecklistViewModel: Ctor");
        mChecklistRepo = new ChecklistRepository(application);
    }

    LiveData<List<ChecklistItem>> getItems(String listTitle, boolean isChecked) {
        // TODO: 3/5/2024 no need to return sorted here, since RecyclerView will use ChecklistItem.getPosition()
        return mChecklistRepo.getSubsetSortedByPosition(listTitle, isChecked);
    }

    public void flipChecked(ChecklistItem item) {
        mChecklistRepo.flipChecked(item.getUid());
    }

    public void insertItem(ChecklistItem item) {
        mChecklistRepo.insert(item);
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        super.onCleared();
    }
}
