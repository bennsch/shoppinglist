package com.example.shoppinglist;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ReportFragment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class ChecklistViewModel extends AndroidViewModel {

    private static final String TAG = "ChecklistViewModel";

    private static Bundle mSettings;
    private static String SETTING_NEW_ITEM_END = "new_item";

    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final ChecklistRepository mChecklistRepo;

    public ChecklistViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "ChecklistViewModel: Ctor");
        mChecklistRepo = new ChecklistRepository(application);

        if (mSettings == null) {
            mSettings = new Bundle();
            mSettings.putBoolean(SETTING_NEW_ITEM_END, false);
        }
    }

    LiveData<List<ChecklistItem>> getFilteredList(String listTitle, boolean isChecked) {
        return mChecklistRepo.getSubsetSortedByPosition(listTitle, isChecked);
    }

    public void flipChecked(@NonNull Integer toBeFlippedUid) {
        mExecutor.execute(() -> {
            ChecklistItem item = mChecklistRepo.getItem(toBeFlippedUid);

            List<ChecklistItem> dbMirrorRemovedFrom = mChecklistRepo.getSublistSorted(item.getListTitle(), item.isChecked());
            dbMirrorRemovedFrom.remove(item);
            updatePositions(dbMirrorRemovedFrom);

            List<ChecklistItem> dbMirrorAddTo = mChecklistRepo.getSublistSorted(item.getListTitle(), !item.isChecked());
            item.flipChecked();
            if (item.isChecked()) {
                dbMirrorAddTo.add(0, item);
            } else {
                dbMirrorAddTo.add(item);
            }
            updatePositions(dbMirrorAddTo);

            dbMirrorRemovedFrom.addAll(dbMirrorAddTo);
            mChecklistRepo.update(dbMirrorRemovedFrom);
        });
    }

    public void insertItem(String listTitle, String name, Boolean isChecked) {
        mExecutor.execute(() -> {
            List<ChecklistItem> dbMirror = mChecklistRepo.getSublistSorted(listTitle, isChecked);
            ChecklistItem newItem = new ChecklistItem(listTitle, name, isChecked);
            if (mSettings.getBoolean(SETTING_NEW_ITEM_END)) {
                dbMirror.add(newItem);
            } else {
                dbMirror.add(0, newItem);
            }
            updatePositions(dbMirror);
            // TODO: 3/11/2024 Let repo detect new items and insert them
            // TODO: 3/11/2024 use Database @Transaction!
            mChecklistRepo.insert(listTitle, name, isChecked);
            mChecklistRepo.update(dbMirror);
        });
    }

    private static void updatePositions(List<ChecklistItem> items) {
        AtomicInteger position = new AtomicInteger(0);
        items.forEach(item -> item.setPosition(position.getAndIncrement()));
    }


    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        super.onCleared();
    }
}
