package com.example.shoppinglist.viewmodel;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoppinglist.ChecklistItem;
import com.example.shoppinglist.data.DbChecklistItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class ChecklistViewModel extends AppViewModel {

    private static final String TAG = "SingleChecklistViewModel";

    private static Bundle mSettings;
    private static String SETTING_NEW_ITEM_END = "new_item";

    private final String mListTitle;
    private final LiveData<List<ChecklistItem>> mItemsSorted;
    private final Observer<Object> observer = o -> {/* Nothing to do */};


    public static class Factory implements ViewModelProvider.Factory {
        private Application mApplication;
        private String mArgListTitle;

        public Factory(Application application, @NonNull String argListTitle) {
            mApplication = application;
            mArgListTitle = argListTitle;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ChecklistViewModel(mApplication, mArgListTitle);
        }
    }


    public ChecklistViewModel(@NonNull Application application, @NonNull String listTitle) {
        super(application);
        Log.d(TAG, "ChecklistViewModel: Ctor: " + listTitle);
        mListTitle = listTitle;
        if (mSettings == null) {
            mSettings = new Bundle();
            mSettings.putBoolean(SETTING_NEW_ITEM_END, false);
        }

        mItemsSorted = Transformations.map(
                mChecklistRepo.getItemsSorted(mListTitle),
                ChecklistViewModel::toChecklistItems
        );
        // Add dummy observer so that mAllItemsSorted is always observed
        // (to make ChecklistViewModel work regardless of the user adding an observer)
        mItemsSorted.observeForever(observer);
    }

    public LiveData<List<ChecklistItem>> getItemsSortedLiveData() {
        return mItemsSorted;
    }

    public void insertItem(ChecklistItem item) {
        if (getItemsSorted().stream().anyMatch(currentItem -> currentItem.getName().equals(item.getName()))) {
            throw new IllegalArgumentException("Item with name " + item.getName() + " already present");
        } else {
            mExecutor.execute(() -> {
                List<DbChecklistItem> dbItems = mChecklistRepo.getSublistSorted(mListTitle, item.isChecked());
                DbChecklistItem newDbItem = new DbChecklistItem(item.getName(), item.isChecked(), 0, mListTitle);
                if (mSettings.getBoolean(SETTING_NEW_ITEM_END)) {
                    dbItems.add(newDbItem);
                } else {
                    dbItems.add(0, newDbItem);
                }
                assignPositionByOrder(dbItems);
                Log.d(TAG, "item: " + newDbItem.getName() + ": " + newDbItem.getPositionInSublist());
                mChecklistRepo.insertAndUpdate(newDbItem, dbItems);
            });
        }
    }

    public void flipItem(String listTitle, ChecklistItem clItem) {
        mExecutor.execute(() -> {
            // TODO: 3/26/2024 Redo the whole thing
            List<DbChecklistItem> dbMirrorRemovedFrom = mChecklistRepo.getSublistSorted(listTitle, clItem.isChecked());
            DbChecklistItem repoItem = dbMirrorRemovedFrom.stream()
                    .filter(item -> item.getName().equals(clItem.getName()))
                    .findFirst()
                    .orElse(null);
            assert repoItem != null;

            boolean removed = dbMirrorRemovedFrom.remove(repoItem);
            assert removed;

            List<DbChecklistItem> dbMirrorAddTo = mChecklistRepo.getSublistSorted(listTitle, !clItem.isChecked());
            repoItem.setChecked(!repoItem.isChecked());
            if (repoItem.isChecked()) {
                dbMirrorAddTo.add(0, repoItem);
            } else {
                dbMirrorAddTo.add(repoItem);
            }

            // Make sure that only a single repo function is called (only single database update)
            assignPositionByOrder(dbMirrorRemovedFrom);
            assignPositionByOrder(dbMirrorAddTo);

            List<DbChecklistItem> dbMirrorCombined = new ArrayList<>();
            dbMirrorCombined.addAll(dbMirrorRemovedFrom);
            dbMirrorCombined.addAll(dbMirrorAddTo);
            mChecklistRepo.update(dbMirrorCombined);
        });
    }

    // TODO: 3/17/2024 use a Map (or such) to map a position to each item
    //  Because the items may not be from the same sublist
    public void updateItemPositions(String listTitle, final List<ChecklistItem> itemsSortedByPos) {
        mExecutor.execute(() -> {
            // TODO: Redo properly
            boolean isChecked = itemsSortedByPos.get(0).isChecked();
            assert itemsSortedByPos.stream().allMatch(item -> item.isChecked() == isChecked);
            List<DbChecklistItem> dbMirror = mChecklistRepo.getSublistSorted(listTitle, isChecked);
            AtomicLong pos = new AtomicLong(0);
            itemsSortedByPos.forEach(item -> {
                DbChecklistItem found = dbMirror.stream()
                        .filter(item1 -> item1.getName().equals(item.getName()))
                        .findFirst().orElse(null);
                assert found != null;
                found.setPositionInSublist(pos.getAndIncrement());
            });
            mChecklistRepo.update(dbMirror);
        });
    }

    private List<ChecklistItem> getItemsSorted() {
        List<ChecklistItem> items = mItemsSorted.getValue();
        assert items != null;
        return items;
    }

    private static List<ChecklistItem> toChecklistItems(List<DbChecklistItem> dbItems) {
        return dbItems.stream()
                .map(dbChecklistItem -> new ChecklistItem(
                        dbChecklistItem.getName(),
                        dbChecklistItem.isChecked()))
                .collect(Collectors.toList());
    }

    private static void assignPositionByOrder(List<DbChecklistItem> repoItems) {
        for (int i = 0; i < repoItems.size(); i++) {
            repoItems.get(i).setPositionInSublist(i);
        }
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        super.onCleared();
        mItemsSorted.removeObserver(observer);
    }
}
