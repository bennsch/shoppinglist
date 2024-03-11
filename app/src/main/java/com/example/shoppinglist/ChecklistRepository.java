package com.example.shoppinglist;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ChecklistRepository {

    private static final String TAG = "ChecklistRepository";

    private ChecklistDatabase.ItemDao mItemDao;


    public ChecklistRepository(@NonNull Application application) {
        ChecklistDatabase db = ChecklistDatabase.getDatabase(application);
        mItemDao = db.itemDao();
    }

    // databaseWriteExecutor not required for call that return LiveData
    // (async code will be performed automatically by the DAO if return type is LiveData)

    ChecklistItem getItem(@NonNull Integer uid) {
        return toChecklistItem(mItemDao.getItem(uid));
    }

    void update(List<ChecklistItem> items) {
        mItemDao.update(toDatabaseItems(items));
    }

    List<ChecklistItem> getList(String listTitle) {
        return toChecklistItems(mItemDao.getList(listTitle));
    }

    LiveData<List<ChecklistItem>> getSubsetSortedByPosition(String listTitle, Boolean isChecked) {
        return Transformations.map(
                mItemDao.getSubsetSortedByPositionAsLiveData(listTitle, isChecked),
                ChecklistRepository::toChecklistItems);
    }

    void insert(ChecklistItem item) {
        mItemDao.insert(toDatabaseItem(item));
    }

    private static List<ChecklistItem> toChecklistItems(List<ChecklistDatabase.Item> dbItems) {
        return dbItems.stream()
                .map(ChecklistRepository::toChecklistItem)
                .collect(Collectors.toList());
    }

    private static ChecklistItem toChecklistItem(ChecklistDatabase.Item dbItem) {
        return new ChecklistItem(
                dbItem.getUID(),
                dbItem.getListTitle(),
                dbItem.getName(),
                dbItem.isChecked());
    }

    private static ChecklistDatabase.Item toDatabaseItem(ChecklistItem clItem) {
        return new ChecklistDatabase.Item(
                clItem.getUid(),
                clItem.getListTitle(),
                clItem.getName(),
                clItem.isChecked());
    }

    private static List<ChecklistDatabase.Item> toDatabaseItems(List<ChecklistItem> clItems) {
        return clItems.stream()
                .map(ChecklistRepository::toDatabaseItem)
                .collect(Collectors.toList());
    }

//    void insertEnd(ChecklistDatabase.Item item) {
//        ChecklistDatabase.databaseWriteExecutor.execute(() -> {
//            insertEndSync(item);
//        });
//    }
//
//    void delete(ChecklistDatabase.Item item) {
//        ChecklistDatabase.databaseWriteExecutor.execute(() -> {
//            deleteSync(item);
//        });
//    }

//    private void insertAndSetPosition(ChecklistDatabase.Item item) {
//        int maxPos = mItemDao.getSubSetMaxPosition(item.getListTitle(), item.isChecked());
//        item.setPosition(maxPos + 1);
//        mItemDao.insert(item);
//        Log.d(TAG, "insertEnd: " + item.getPosition());
//    }

//    private void deleteAndUpdateAllPositions(ChecklistDatabase.Item databaseItem) {
//        mItemDao.delete(databaseItem);
//        List<ChecklistDatabase.Item> sorted = mItemDao.getSubsetSortedByPosition(databaseItem.getListTitle(), databaseItem.isChecked());
//        for (int i = 0; i < sorted.size(); ++i) {
//            sorted.get(i).setPosition(i);
//            Log.d(TAG, "delete: loop" + sorted.get(i).getPosition());
//        }
//        mItemDao.update(sorted);
//    }
}