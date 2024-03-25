package com.example.shoppinglist;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


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

    LiveData<List<String>> getAllChecklistTitles() {
        return mChecklistRepo.getAllChecklistTitles();
    }

    LiveData<List<ChecklistItem>> getFilteredList(String listTitle, boolean isChecked) {
        return mChecklistRepo.getSubsetSortedByPosition(listTitle, isChecked);
    }

    public void flipItemChecked(String listTitle, String name) {
        mExecutor.execute(() -> {
            ChecklistItem item = mChecklistRepo.getItem(listTitle, name);
            List<ChecklistRepository.ItemWithPosition> itemsWithPosition = new ArrayList<>();

            if (item == null) {
                throw new IndexOutOfBoundsException("Item not found " + name);
            }

            List<ChecklistItem> dbMirrorRemovedFrom = mChecklistRepo.getSublistSorted(listTitle, item.isChecked());
            if (!(dbMirrorRemovedFrom.removeIf(item1 -> item1.getName().equals(item.getName())))) {
                throw new IndexOutOfBoundsException("No item with such name " + name);
            }
            itemsWithPosition.addAll(mapPositionByOrder(dbMirrorRemovedFrom));

            List<ChecklistItem> dbMirrorAddTo = mChecklistRepo.getSublistSorted(listTitle, !item.isChecked());
            item.flipChecked();
            if (item.isChecked()) {
                dbMirrorAddTo.add(0, item);
            } else {
                dbMirrorAddTo.add(item);
            }
            itemsWithPosition.addAll(mapPositionByOrder(dbMirrorAddTo));
            // Make sure that only a single repo function is called (only single database update)
            mChecklistRepo.updateAndOrInsert(listTitle, itemsWithPosition);
        });
    }

    public void insertItem(String listTitle, ChecklistItem item) {
        mExecutor.execute(() -> {
            List<ChecklistItem> dbMirror = mChecklistRepo.getSublistSorted(listTitle, item.isChecked());
            if (mSettings.getBoolean(SETTING_NEW_ITEM_END)) {
                dbMirror.add(item);
            } else {
                dbMirror.add(0, item);
            }
            mChecklistRepo.updateAndOrInsert(listTitle, mapPositionByOrder(dbMirror));
        });
    }

    public void insertChecklist(String listTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.insertChecklist(listTitle);
        });
    }


    // TODO: 3/17/2024 use a Map (or such) to map a position to each item
    //  Because the items may not be from the same sublist
    public void updateItemPositions(String listTitle, final List<ChecklistItem> itemsSortedByPos) {
        mExecutor.execute(() -> {
            mChecklistRepo.updateAndOrInsert(listTitle, mapPositionByOrder(itemsSortedByPos));
        });
    }

    private static List<ChecklistRepository.ItemWithPosition> mapPositionByOrder(List<ChecklistItem> items) {
        AtomicLong position = new AtomicLong(0);
        return items.stream()
                .map(item -> new ChecklistRepository.ItemWithPosition(position.getAndIncrement(), item))
                .collect(Collectors.toList());
    }


    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        super.onCleared();
    }
}
