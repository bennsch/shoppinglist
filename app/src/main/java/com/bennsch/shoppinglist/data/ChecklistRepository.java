package com.bennsch.shoppinglist.data;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
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

    // TODO: Check if distinctUntilChanged() is necessary for other
    //  LiveData functions as well

    public LiveData<List<String>> getAllChecklistTitles() {
        // The database will update the LiveData if ANY of the DbChecklist
        // members are updated. Hence, this LiveData would be updated
        // even if only "selected" is updated, but not actually the checklistTitles.
        // Therefore we need to use distinctUntilChanged.
        return Transformations.distinctUntilChanged(
                Transformations.map(
                        mItemDao.getAllChecklists(),
                        checklists -> checklists.stream()
                                .map(DbChecklist::getChecklistTitle)
                                .collect(Collectors.toList())));
    }

    public LiveData<String> getSelectedChecklist() {
        // The database will update the LiveData if ANY of the DbChecklist
        // members are updated. Hence, this LiveData would be updated
        // even if a new checklist is added, but the selected list did not change.
        // Therefore we need to use distinctUntilChanged.
        return Transformations.distinctUntilChanged(
                mItemDao.getSelectedChecklist());
    }

    public void selectChecklist(String checklistTitle) {
        mItemDao.selectChecklist(checklistTitle);
    }

    public void insertChecklist(String listTitle) {
        mItemDao.insert(new DbChecklist(listTitle, false, false));
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