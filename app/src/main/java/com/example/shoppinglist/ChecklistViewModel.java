package com.example.shoppinglist;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChecklistViewModel extends AndroidViewModel {

    private static final String TAG = "ChecklistViewModel";

    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final ChecklistRepository mChecklistRepo;

    public ChecklistViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "ChecklistViewModel: Ctor");
        mChecklistRepo = new ChecklistRepository(application);
    }

    LiveData<List<ChecklistItem>> getFilteredList(String listTitle, boolean isChecked) {
        // TODO: 3/5/2024 no need to return sorted here, since RecyclerView will use ChecklistItem.getPosition()
        return mChecklistRepo.getSubsetSortedByPosition(listTitle, isChecked);
    }

    public void flipChecked(ChecklistItem toBeFlipped) {
        mExecutor.execute(() -> {
            List<ChecklistItem> dbMirror = mChecklistRepo.getList(toBeFlipped.getListTitle());
            dbMirror.stream().filter(item -> item.getUid().equals(toBeFlipped.getUid()))
                    .findFirst()
                    .orElse(null)
            .flipChecked();
            mChecklistRepo.update(dbMirror);
        });
    }

    public void insertItem(ChecklistItem item) {
        mExecutor.execute(() -> {
            mChecklistRepo.insert(item);
        });
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        super.onCleared();
    }
}
