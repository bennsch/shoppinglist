package com.example.shoppinglist.data;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.List;
import java.util.stream.Collectors;


public class ChecklistRepository {

    // databaseWriteExecutor not required for call that return LiveData
    // (async code will be performed automatically by the DAO if return type is LiveData)

    private static final String TAG = "ChecklistRepository";

    private ChecklistDatabase.ItemDao mItemDao;


    public ChecklistRepository(@NonNull Application application) {
        ChecklistDatabase db = ChecklistDatabase.getDatabase(application);
        mItemDao = db.itemDao();
    }

    public DbChecklistItem getItem(String listTitle, String name) {
        return mItemDao.getItem(listTitle, name);
    }

    public List<DbChecklistItem> getItemsFromList(String listTitle) {
        return mItemDao.getAllItemsFromChecklist(listTitle);
    }

    public LiveData<List<String>> getAllChecklistTitles() {
        return Transformations.map(
                mItemDao.getAllChecklists(),
                checklists -> checklists.stream()
                        .map(DbChecklist::getChecklistTitle)
                        .collect(Collectors.toList()));
    }

    public void insertChecklist(String listTitle) {
        mItemDao.insert(new DbChecklist(listTitle));
    }

    public void update(List<DbChecklistItem> items) {
        mItemDao.update(items);
    }

    public void insertAndUpdate(DbChecklistItem itemToInsert,
                                List<DbChecklistItem> itemsToUpdate) {
        mItemDao.insertAndUpdate(itemToInsert, itemsToUpdate);
    }

    public List<DbChecklistItem> getSublistSorted(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return mItemDao.getSubsetSortedByPosition(listTitle, isChecked);
    }

    public LiveData<List<DbChecklistItem>> getSublistSortedLiveData(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return mItemDao.getSubsetSortedByPositionAsLiveData(listTitle, isChecked);
    }
}