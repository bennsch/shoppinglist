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


public class SingleChecklistViewModel extends ChecklistViewModel {

    private static final String TAG = "SingleChecklistViewModel";

    private static Bundle mSettings;
    private static String SETTING_NEW_ITEM_END = "new_item";

    private String mListTitle;
    private LiveData<List<ChecklistItem>> mAllItemsSorted;
    private final Observer<Object> observer = o -> {};


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
            return (T) new SingleChecklistViewModel(mApplication, mArgListTitle);
        }
    }


    public SingleChecklistViewModel(@NonNull Application application, @NonNull String listTitle) {
        super(application);
        Log.d(TAG, "ChecklistViewModel: Ctor");
        mListTitle = listTitle;
        if (mSettings == null) {
            mSettings = new Bundle();
            mSettings.putBoolean(SETTING_NEW_ITEM_END, false);
        }
        mAllItemsSorted = Transformations.map(
                mChecklistRepo.getItemsSorted(mListTitle),
                dbChecklistItems -> dbChecklistItems.stream()
                        .map(dbChecklistItem -> new ChecklistItem(dbChecklistItem.getName(), dbChecklistItem.isChecked()))
                        .collect(Collectors.toList())
        );
        // Add dummy observer so that mAllItemsSorted is always observed
        // (to make ChecklistViewModel work regardless of the user adding an observer)
        mAllItemsSorted.observeForever(observer);
    }

    public LiveData<List<ChecklistItem>> getAllItemsSortedLiveData() {
        return mAllItemsSorted;
    }

    private List<ChecklistItem> getAllItemsSorted() {
        List<ChecklistItem> items = mAllItemsSorted.getValue();
        assert items != null;
        return items;
    }

    private List<ChecklistItem> getFilteredItemsSorted(boolean isChecked) {
        return getAllItemsSorted().stream()
                .filter(item -> item.isChecked() == isChecked)
                .collect(Collectors.toList());
    }

    public void insertItem(ChecklistItem item) {
        if (getAllItemsSorted().stream().anyMatch(item1 -> item1.getName().equals(item.getName()))) {
            throw new IllegalArgumentException("Item with name " + item.getName() + " already present");
        } else {
            mExecutor.execute(() -> {
                List<DbChecklistItem> dbMirror = mChecklistRepo.getSublistSorted(mListTitle, item.isChecked());
                DbChecklistItem newRepoItem = new DbChecklistItem(item.getName(), item.isChecked(), 0, mListTitle);
                if (mSettings.getBoolean(SETTING_NEW_ITEM_END)) {
                    dbMirror.add(newRepoItem);
                } else {
                    dbMirror.add(0, newRepoItem);
                }
                assignPositionByOrder(dbMirror);

                Log.d(TAG, "item: " + newRepoItem.getName() + ": " + newRepoItem.getPositionInSublist());
                dbMirror.forEach(repoItem -> {
                    Log.d(TAG, "mirror:" + repoItem.getName() + ": " + repoItem.getPositionInSublist());
                });

                mChecklistRepo.insertAndUpdate(newRepoItem, dbMirror);
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

    private static void assignPositionByOrder(List<DbChecklistItem> repoItems) {
        for (int i = 0; i < repoItems.size(); i++) {
            repoItems.get(i).setPositionInSublist(i);
        }
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        super.onCleared();
        mAllItemsSorted.removeObserver(observer);
    }
}
