package com.bennsch.shoppinglist.datamodel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.List;
import java.util.stream.Collectors;


public class ChecklistRepository {
    /*
     *  A Repository class abstracts access to multiple data sources. Currently only one data source
     *  is implemented (ChecklistDatabase, which is a Room-database to store the data locally on the
     *  device).
     */
    
    private static ChecklistRepository INSTANCE;
    private final ChecklistDatabase.ItemDao mItemDao;


    private ChecklistRepository(@NonNull Context context) {
        ChecklistDatabase db = ChecklistDatabase.getInstance(context);
        mItemDao = db.itemDao();
    }

    public static synchronized ChecklistRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ChecklistRepository(context);
        }
        return INSTANCE;
    }

    public LiveData<List<String>> getAllChecklistTitles() {
        // distinctUntilChanged() is required, because the room database would notify the LiveData 
        // observers if ANY column in the DbChecklist table changes, not just the title of a
        // checklist.
        return Transformations.distinctUntilChanged(
                Transformations.map(
                        mItemDao.getAllChecklists(),
                        checklists -> checklists.stream()
                                .map(DbChecklist::getListTitle)
                                .collect(Collectors.toList())));
    }

    public LiveData<String> getActiveChecklistTitle() {
        // distinctUntilChanged() is required, because the room database would notify the LiveData 
        // observers if ANY column in the DbChecklist table changes, not just the "active" attribute.
        return Transformations.distinctUntilChanged(
                Transformations.map(
                        mItemDao.getActiveChecklist(), dbChecklist -> {
                            if (dbChecklist != null) {
                                return dbChecklist.getListTitle();
                            } else {
                                return null;
                            }
                        }));
    }

    public void setActiveChecklist(@Nullable String listTitle) {
        // Set the checklist "listTitle" active, set all other checklists "inactive". If title is 
        // null, set all checklists inactive.
        mItemDao.setActiveChecklist(listTitle);
    }

    public void updateChecklistTitle(@NonNull String listTitle, @NonNull String newListTitle) {
        mItemDao.update(listTitle, newListTitle);
    }

    public void insertChecklist(@NonNull String listTitle) {
        mItemDao.insert(new DbChecklist(listTitle, false));
    }

    public void deleteChecklist(@NonNull String listTitle) {
        mItemDao.delete(listTitle);
    }

    public List<DbChecklistItem> getAllItems() {
        // Return all items in the database.
        return mItemDao.getAllItems();
    }

    public List<DbChecklistItem> getItems(@NonNull String listTitle) {
        // Return all items from a checklist.
        return mItemDao.getItems(listTitle);
    }

    public LiveData<List<DbChecklistItem>> getItemsLiveData(@NonNull String listTitle) {
        // Same as getItems(), but items are wrapped in a LiveData holder.
        return mItemDao.getItemsLiveData(listTitle);
    }

    public List<DbChecklistItem> getItemSubsetSorted(@NonNull String listTitle,
                                                     @NonNull Boolean isChecked) {
        // Return all items from the checklist that are "isChecked".
        // The items are sorted by their "position".
        return mItemDao.getItemSubsetSorted(listTitle, isChecked);
    }

    public LiveData<List<DbChecklistItem>> getItemSubsetSortedLiveData(@NonNull String listTitle, @NonNull Boolean isChecked) {
        // Same as getItemSubsetSorted(), but items are wrapped in a LiveData holder.
        return mItemDao.getItemSubsetSortedLiveData(listTitle, isChecked);
    }

    public void updateItems(@NonNull List<DbChecklistItem> items) {
        // Update the database items so they match the items in "items". Their "itemId" is used to
        // find the right items in the database.
        mItemDao.update(items);
    }

    public void insertAndUpdateItems(@NonNull DbChecklistItem itemToInsert, @NonNull List<DbChecklistItem> itemsToUpdate) {
        // Insert a new item, then update all items in "itemsToUpdate".
        mItemDao.insertAndUpdate(itemToInsert, itemsToUpdate);
    }

    public void deleteItem(@NonNull DbChecklistItem item) {
        mItemDao.delete(item);
    }

    public long getMinIncidence(@NonNull String listTitle) {
        // Return the smallest "incidence" value in the checklist.
        // Returns 0 if the list is empty.
        return mItemDao.getMinIncidence(listTitle);
    }
}