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
    private ChecklistDatabase.Item dbItem;


    public ChecklistRepository(@NonNull Application application) {
        ChecklistDatabase db = ChecklistDatabase.getDatabase(application);
        mItemDao = db.itemDao();
    }

    // databaseWriteExecutor not required for call that return LiveData
    // (async code will be performed automatically by the DAO if return type is LiveData)

//    LiveData<List<ChecklistDatabase.Item>> getSubset(String listTitle, Boolean isChecked) {
//        return mItemDao.getSubsetAsLiveData(listTitle, isChecked);
//    }

    LiveData<List<Item>> getSubsetSortedByPosition(String listTitle, Boolean isChecked) {
        return Transformations.map(
                mItemDao.getSubsetSortedByPositionAsLiveData(listTitle, isChecked),
                ChecklistRepository::toRepoItems);
    }


    void flipChecked(ChecklistRepository.Item item) {
        // TODO: 3/2/2024  why is observer called twice per list? (4 calls in total)
        //  Perform all updates on local list then commit to itemDao
        ChecklistDatabase.databaseWriteExecutor.execute(() -> {

            ChecklistDatabase.Item dbItem = mItemDao.getItem(item.getUid());
            dbItem.flipChecked();
            mItemDao.update(dbItem);

//            ChecklistDatabase.Item dbItem = mItemDao.getItem(item.getUid());
//            List<ChecklistDatabase.Item> dbItemsToRemoveFrom = mItemDao.getSubsetSortedByPosition(item.getListTitle(), item.isChecked());
//            dbItemsToRemoveFrom.remove(dbItem);
//            List<ChecklistDatabase.Item> dbItemsToAddTo = mItemDao.getSubsetSortedByPosition(item.getListTitle(), !item.isChecked());
//            dbItem.flipChecked();
//            dbItemsToAddTo.add(dbItem);

        });
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

    private static List<Item> toRepoItems(List<ChecklistDatabase.Item> databaseItems) {
        return databaseItems.stream().map(Item::new).collect(Collectors.toList());
    }


    static class Item {
        private final Integer mUid;
        private String mName;
        private String mListTitle;
        private Boolean mIsChecked;

        public Item(String listTitle, String name, boolean isChecked) {
            // Only the database is allowed to generate the UID
            mUid = null; // non-set
            mListTitle = listTitle;
            mName = name;
            mIsChecked = isChecked;
        }

        public Item(ChecklistDatabase.Item databaseItem) {
            mUid = databaseItem.getUID();
            mListTitle = databaseItem.getListTitle();
            mName = databaseItem.getName();
            mIsChecked = databaseItem.isChecked();
        }

        @Nullable
        public Integer getUid() {
            return mUid;
        }

        public Boolean isChecked() {
            return mIsChecked;
        }

        public String getName() {
            return mName;
        }

        public String getListTitle() {
            return mListTitle;
        }
    }
}