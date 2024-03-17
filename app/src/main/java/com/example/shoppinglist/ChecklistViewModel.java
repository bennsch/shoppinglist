package com.example.shoppinglist;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ReportFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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

    LiveData<List<ChecklistItem>> getFilteredList(String listTitle, boolean isChecked) {
        return mChecklistRepo.getSubsetSortedByPosition(listTitle, isChecked);
    }

    public void flipChecked(@NonNull Integer toBeFlippedUid) {
        mExecutor.execute(() -> {
            ChecklistItem item = mChecklistRepo.getItem(toBeFlippedUid);
            List<ChecklistRepository.ItemWithPosition> itemsWithPosition = new ArrayList<>();

            if (item == null) {
                throw new IndexOutOfBoundsException("UID not found in database " + toBeFlippedUid);
            }
            List<ChecklistItem> dbMirrorRemovedFrom = mChecklistRepo.getSublistSorted(item.getListTitle(), item.isChecked());
            if (!(dbMirrorRemovedFrom.removeIf(item1 -> item1.getUid().equals(item.getUid())))) {
                throw new IndexOutOfBoundsException("No item with such UID " + toBeFlippedUid);
            }
            itemsWithPosition.addAll(mapPositionByOrder(dbMirrorRemovedFrom));

            List<ChecklistItem> dbMirrorAddTo = mChecklistRepo.getSublistSorted(item.getListTitle(), !item.isChecked());
            item.flipChecked();
            if (item.isChecked()) {
                dbMirrorAddTo.add(0, item);
            } else {
                dbMirrorAddTo.add(item);
            }
            itemsWithPosition.addAll(mapPositionByOrder(dbMirrorAddTo));
            // Make sure that only a single repo function is called (only single database update)
            mChecklistRepo.updateAndOrInsert(itemsWithPosition);
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
            mChecklistRepo.updateAndOrInsert(mapPositionByOrder(dbMirror));

        });
    }

    // TODO: 3/17/2024 use a Map (or such) to map a position to each item
    //  Because the items may not be from the same sublist
    public void updateItemPositions(final List<ChecklistItem> itemsSortedByPos) {
        mExecutor.execute(() -> {
            mChecklistRepo.updateAndOrInsert(mapPositionByOrder(itemsSortedByPos));
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
