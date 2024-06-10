package com.example.shoppinglist.viewmodel;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.example.shoppinglist.ChecklistItem;
import com.example.shoppinglist.data.ChecklistRepository;
import com.example.shoppinglist.data.DbChecklistItem;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.nio.InvalidMarkException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class AppViewModel extends AndroidViewModel {

    private static final String TAG = "AppViewModel";

    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private static final ListeningExecutorService mLexec = MoreExecutors.listeningDecorator(mExecutor);

    private final ChecklistRepository mChecklistRepo;
    private final LiveData<List<String>> mListTitles;
    private static Bundle mSettings;
    private static String SETTING_NEW_ITEM_END = "new_item";


    public AppViewModel(@NonNull Application application) {
        super(application);


        Log.d(TAG, "ChecklistViewModel: Ctor");
        mChecklistRepo = new ChecklistRepository(application);
        mListTitles = mChecklistRepo.getAllChecklistTitles();

        if (mSettings == null) {
            mSettings = new Bundle();
            mSettings.putBoolean(SETTING_NEW_ITEM_END, true);
        }
    }

    public LiveData<List<String>> getAllChecklistTitles() {
        return mListTitles;
    }

    public boolean isNewItemInsertBottom() {
        return mSettings.getBoolean(SETTING_NEW_ITEM_END);
    }

    public void insertChecklist(String listTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.insertChecklist(listTitle);
        });
    }

    public void deleteChecklist(String checklistTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.deleteChecklist(checklistTitle);
        });
    }

    public void updateChecklistName(String checklistTitle, String newTitle) {
        assert mListTitles.getValue() != null;
        if (mListTitles.getValue().contains(newTitle)) {
            throw new IllegalArgumentException("Checklist title '" + newTitle + "' already present");
        } else {
            mExecutor.execute(() -> {
                mChecklistRepo.updateChecklistName(checklistTitle, newTitle);
            });
        }
    }

    public LiveData<List<ChecklistItem>> getItemsSortedByPosition(String listTitle, boolean isChecked) {
        return Transformations.map(
                mChecklistRepo.getItemsSortedByPosition(listTitle, isChecked),
                AppViewModel::toChecklistItems);
    }

    public ListenableFuture<Void> insertItem(final @NonNull String listTitle, final @NonNull ChecklistItem item) {
        return mLexec.submit(() -> {
            if (item.getName().isEmpty()) {
                throw new Exception("Empty");
            }
            else if (mChecklistRepo.getItemsFromList(listTitle)
                    .stream()
                    .anyMatch(dbItem -> dbItem.getName().equals(item.getName()))) {
                throw new Exception("\"" + item.getName() + "\" already present");
            } else {
                List<DbChecklistItem> dbItems = mChecklistRepo.getSublistSorted(listTitle, item.isChecked());
                DbChecklistItem newDbItem = new DbChecklistItem(item.getName(), item.isChecked(), 0, listTitle);
                if (mSettings.getBoolean(SETTING_NEW_ITEM_END)) {
                    dbItems.add(newDbItem);
                } else {
                    dbItems.add(0, newDbItem);
                }
                assignPositionByOrder(dbItems);
                Log.d(TAG, "item: " + newDbItem.getName() + ": " + newDbItem.getPositionInSublist());
                mChecklistRepo.insertAndUpdate(newDbItem, dbItems);
                return null;
            }
        });
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
    //  Because the items may not be from the same sublist(checked/unchecked)
    public void itemsHaveBeenMoved(String listTitle, final List<ChecklistItem> itemsSortedByPos) {

        // TODO: 5/12/2024 update "importance" of item if it has been moved in "checked" list

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
    }
}
