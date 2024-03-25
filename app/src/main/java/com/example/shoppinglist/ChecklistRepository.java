package com.example.shoppinglist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ChecklistRepository {

    private static final String TAG = "ChecklistRepository";

    private ChecklistDatabase.ItemDao mItemDao;


    public static class ItemWithPosition {
        private final long mPosition;
        private final ChecklistItem mItem;
        public ItemWithPosition(long position, ChecklistItem item) {
            assert position >= 0;
            mPosition = position;
            mItem = item;
        }
    }

    public ChecklistRepository(@NonNull Application application) {
        ChecklistDatabase db = ChecklistDatabase.getDatabase(application);
        mItemDao = db.itemDao();
    }

    // databaseWriteExecutor not required for call that return LiveData
    // (async code will be performed automatically by the DAO if return type is LiveData)

    ChecklistItem getItem(String listTitle, String name) {
        return toChecklistItem(mItemDao.getItem(listTitle, name));
    }

    LiveData<List<String>> getAllChecklistTitles() {
        return Transformations.map(
                mItemDao.getAllChecklists(),
                checklists -> checklists.stream()
                        .map(ChecklistDatabase.Checklist::getChecklistTitle)
                        .collect(Collectors.toList()));
    }

    void insertChecklist(String listTitle) {
        mItemDao.insert(new ChecklistDatabase.Checklist(listTitle));
    }

    void updateAndOrInsert(String listTitle, List<ItemWithPosition> items) {
        List<ChecklistDatabase.Item> dbItemsToInsert = new ArrayList<>();
        List<ChecklistDatabase.Item> dbItemsToUpdate = new ArrayList<>();
        items.forEach(itemWithPosition -> {
            ChecklistDatabase.Item dbItem = mItemDao.getItem(listTitle, itemWithPosition.mItem.getName());
            if (dbItem == null) {
                dbItemsToInsert.add(new ChecklistDatabase.Item(
                        itemWithPosition.mItem.getName(),
                        itemWithPosition.mItem.isChecked(),
                        itemWithPosition.mPosition,
                        listTitle));
            } else {
                // TODO: 3/24/2024 make sure to update all fields!
                dbItem.setChecked(itemWithPosition.mItem.isChecked());
                dbItem.setPositionInSublist(itemWithPosition.mPosition);
                dbItemsToUpdate.add(dbItem);
            }
        });
        mItemDao.insertAndUpdate(listTitle, dbItemsToInsert, dbItemsToUpdate);
    }

    List<ChecklistItem> getSublistSorted(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return toChecklistItems(mItemDao.getSubsetSortedByPosition(listTitle, isChecked));
    }

    LiveData<List<ChecklistItem>> getSubsetSortedByPosition(String listTitle, Boolean isChecked) {
        return Transformations.map(
                mItemDao.getSubsetSortedByPositionAsLiveData(listTitle, isChecked),
                ChecklistRepository::toChecklistItems);
    }


    private static List<ChecklistItem> toChecklistItems(List<ChecklistDatabase.Item> dbItems) {
        return dbItems.stream()
                .map(ChecklistRepository::toChecklistItem)
                .collect(Collectors.toList());
    }

    private static ChecklistItem toChecklistItem(ChecklistDatabase.Item dbItem) {
        return new ChecklistItem(
                dbItem.getName(),
                dbItem.isChecked());
    }
}