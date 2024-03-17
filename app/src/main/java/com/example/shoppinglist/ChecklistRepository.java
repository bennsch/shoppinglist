package com.example.shoppinglist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    ChecklistItem getItem(@NonNull Integer uid) {
        return toChecklistItem(mItemDao.getItem(uid));
    }

    void updateAndOrInsert(List<ItemWithPosition> items) {
        mItemDao.updateAndOrInsert(toDatabaseItems(items));
    }

    int getSubSetMaxPosition(String listTitle, Boolean isChecked) {
        return mItemDao.getSubSetMaxPosition(listTitle, isChecked);
    }

    List<ChecklistItem> getList(String listTitle) {
        return toChecklistItems(mItemDao.getList(listTitle));
    }

    List<ChecklistItem> getSublistSorted(@NonNull String listTitle, @NonNull Boolean isChecked) {
        return toChecklistItems(mItemDao.getSubsetSortedByPosition(listTitle, isChecked));
    }

    LiveData<List<ChecklistItem>> getSubsetSortedByPosition(String listTitle, Boolean isChecked) {
        return Transformations.map(
                mItemDao.getSubsetSortedByPositionAsLiveData(listTitle, isChecked),
                ChecklistRepository::toChecklistItems);
    }

    void insert(String listTitle, String name, Boolean isChecked) {
        mItemDao.insert(new ChecklistDatabase.Item(null, listTitle, name, isChecked, null));
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

    private static List<ChecklistDatabase.Item> toDatabaseItems(List<ItemWithPosition> clItems) {
        return clItems.stream()
                .map(itemWithPosition -> new ChecklistDatabase.Item(
                        itemWithPosition.mItem.getUid(),
                        itemWithPosition.mItem.getListTitle(),
                        itemWithPosition.mItem.getName(),
                        itemWithPosition.mItem.isChecked(),
                        itemWithPosition.mPosition))
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