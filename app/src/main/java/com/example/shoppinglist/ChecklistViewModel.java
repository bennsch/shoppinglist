package com.example.shoppinglist;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class ChecklistViewModel extends AndroidViewModel {

    private static final String TAG = "ChecklistViewModel";

    private static Bundle mSettings;
    private static String SETTING_NEW_ITEM_END = "new_item";

    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final ChecklistRepository mChecklistRepo;

    public ChecklistViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "ChecklistViewModel: Ctor");
        mChecklistRepo = new ChecklistRepository(application);

        if (mSettings == null) {
            mSettings = new Bundle();
            mSettings.putBoolean(SETTING_NEW_ITEM_END, false);
        }
    }

    LiveData<List<String>> getAllChecklistTitles() {
        return mChecklistRepo.getAllChecklistTitles();
    }

    public void insertChecklist(String listTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.insertChecklist(listTitle);
        });
    }

    LiveData<List<ChecklistItem>> getSubsetSortedByPosition(String listTitle, boolean isChecked) {
        return Transformations.map(
                mChecklistRepo.getSublistSortedLiveData(listTitle, isChecked),
                items -> items.stream()
                        .map(repoItem -> new ChecklistItem(repoItem.getName(), repoItem.isChecked()))
                        .collect(Collectors.toList()));
    }

    public void insertItem(String listTitle, ChecklistItem item) {
        mExecutor.execute(() -> {
            List<RepoItem> dbMirror = mChecklistRepo.getSublistSorted(listTitle, item.isChecked());
            RepoItem newRepoItem = new RepoItem(item.getName(), item.isChecked(), 0, listTitle);
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

    public void flipItem(String listTitle, ChecklistItem clItem) {
        mExecutor.execute(() -> {
            // TODO: 3/26/2024 Redo the whole thing
            List<RepoItem> dbMirrorRemovedFrom = mChecklistRepo.getSublistSorted(listTitle, clItem.isChecked());
            RepoItem repoItem = dbMirrorRemovedFrom.stream()
                    .filter(item -> item.getName().equals(clItem.getName()))
                    .findFirst()
                    .orElse(null);
            assert repoItem != null;

            boolean removed = dbMirrorRemovedFrom.remove(repoItem);
            assert removed;

            List<RepoItem> dbMirrorAddTo = mChecklistRepo.getSublistSorted(listTitle, !clItem.isChecked());
            repoItem.setChecked(!repoItem.isChecked());
            if (repoItem.isChecked()) {
                dbMirrorAddTo.add(0, repoItem);
            } else {
                dbMirrorAddTo.add(repoItem);
            }

            // Make sure that only a single repo function is called (only single database update)
            assignPositionByOrder(dbMirrorRemovedFrom);
            assignPositionByOrder(dbMirrorAddTo);

            List<RepoItem> dbMirrorCombined = new ArrayList<>();
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
            List<RepoItem> dbMirror = mChecklistRepo.getSublistSorted(listTitle, isChecked);
            AtomicLong pos = new AtomicLong(0);
            itemsSortedByPos.forEach(item -> {
                RepoItem found = dbMirror.stream()
                        .filter(item1 -> item1.getName().equals(item.getName()))
                        .findFirst().orElse(null);
                assert found != null;
                found.setPositionInSublist(pos.getAndIncrement());
            });
            mChecklistRepo.update(dbMirror);
        });
    }

    static void assignPositionByOrder(List<RepoItem> repoItems) {
        for (int i = 0; i < repoItems.size(); i++) {
            repoItems.get(i).setPositionInSublist(i);
        }
    }

    @Override
    protected void onCleared() {
        Log.d(TAG, "onCleared: ");
        super.onCleared();
    }
}
