package com.example.shoppinglist.data;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.List;
import java.util.Map;
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

    public LiveData<List<String>> getAllChecklistTitles() {
        return Transformations.map(
                mItemDao.getAllChecklists(),
                checklists -> checklists.stream()
                        .map(DbChecklist::getChecklistTitle)
                        .collect(Collectors.toList()));
    }

    public void insertChecklist(String listTitle) {
        mItemDao.insert(new DbChecklist(listTitle, false));
    }

    public void update(List<DbChecklistItem> items) {
        mItemDao.update(items);
    }

    public void updateChecklistName(String checklistTitle, String newTitle) {
        mItemDao.update(checklistTitle, newTitle);
    }


    public void deleteChecklist(String listTitle) {
        mItemDao.delete(listTitle);
    }

    public void insertAndUpdate(DbChecklistItem itemToInsert,
                                List<DbChecklistItem> itemsToUpdate) {
        mItemDao.insertAndUpdate(itemToInsert, itemsToUpdate);
    }

    public List<DbChecklistItem> getItemsFromList(@NonNull final String listTitle) {
        return mItemDao.getItemsFromList(listTitle);
    }

    public List<DbChecklistItem> getSublistSorted(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return mItemDao.getSubsetSortedByPosition(listTitle, isChecked);
    }

    public LiveData<List<DbChecklistItem>> getItemsSortedByPosition(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return mItemDao.getSubsetSortedByPositionAsLiveData(listTitle, isChecked);
    }
}