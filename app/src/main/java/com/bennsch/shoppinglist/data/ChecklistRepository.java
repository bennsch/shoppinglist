package com.bennsch.shoppinglist.data;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.List;
import java.util.stream.Collectors;


public class ChecklistRepository {

    // A Repository class abstracts access to multiple data sources.
    // Currently only one data source is implemented (Room database).

    private static final String TAG = "ChecklistRepository";

    private final ChecklistDatabase.ItemDao mItemDao;


    public ChecklistRepository(@NonNull Application application) {
        ChecklistDatabase db = ChecklistDatabase.getInstance(application);
        mItemDao = db.itemDao();
    }

    public LiveData<List<String>> getAllChecklistTitles() {
        // The Room database will notify the  observers of LiveData
        // if any of the data in the Checklist table changes, not just
        // the title of a checklist, so distinctUntilChanged() is required.
        return Transformations.distinctUntilChanged(
                Transformations.map(
                        mItemDao.getAllChecklists(),
                        checklists -> checklists.stream()
                                .map(DbChecklist::getChecklistTitle)
                                .collect(Collectors.toList())));
    }

    public LiveData<String> getActiveChecklistTitle() {
        // The Room database would notify the  observers of LiveData
        // if any of the data in the Checklist table changes, not just
        // the "active" attribute, so distinctUntilChanged() is required.
        return Transformations.distinctUntilChanged(
                Transformations.map(
                        mItemDao.getActiveChecklist(), dbChecklist -> {
                            if (dbChecklist != null) {
                                return dbChecklist.getChecklistTitle();
                            } else {
                                return null;
                            }
                        }));
    }

    public void setActiveChecklist(String checklistTitle) {
        // Make Checklist with title "checklistTitle" active, and all other
        // Checklists inactive
        mItemDao.setActiveChecklist(checklistTitle);
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

    public void deleteItem(@NonNull DbChecklistItem item) {
        mItemDao.delete(item);
    }

    public List<DbChecklistItem> getAllItems(@NonNull final String listTitle) {
        return mItemDao.getItemsFromChecklist(listTitle);
    }

    public List<DbChecklistItem> getItems(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return mItemDao.getItems(listTitle, isChecked);
    }

    public List<DbChecklistItem> getItemsSortedByPosition(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return mItemDao.getItemsSortedByPosition(listTitle, isChecked);
    }

    public LiveData<List<DbChecklistItem>> getItemsSortedByPositionLiveData(@NonNull String listTitle, @NonNull Boolean isChecked) {
        // TODO: Why are still both observers (checked and unchecked fragment) being called?
//        return Transformations.distinctUntilChanged(
//                mItemDao.getItemsSortedByPositionLiveData(listTitle, isChecked));
        return mItemDao.getItemsSortedByPositionLiveData(listTitle, isChecked);
    }
}