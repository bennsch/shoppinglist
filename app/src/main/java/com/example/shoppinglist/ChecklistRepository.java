package com.example.shoppinglist;

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

    RepoItem getItem(String listTitle, String name) {
        return mItemDao.getItem(listTitle, name);
    }

    List<RepoItem> getItemsFromList(String listTitle) {
        return mItemDao.getAllItemsFromChecklist(listTitle);
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

    void update(List<RepoItem> items) {
        mItemDao.update(items);
    }

    void insertAndUpdate(RepoItem itemToInsert,
                         List<RepoItem> itemsToUpdate) {
        mItemDao.insertAndUpdate(itemToInsert, itemsToUpdate);
    }

    List<RepoItem> getSublistSorted(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return mItemDao.getSubsetSortedByPosition(listTitle, isChecked);
    }

    LiveData<List<RepoItem>> getSublistSortedLiveData(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return mItemDao.getSubsetSortedByPositionAsLiveData(listTitle, isChecked);
    }
}