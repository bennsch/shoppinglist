package com.bennsch.shoppinglist;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.bennsch.shoppinglist.data.ChecklistRepository;
import com.bennsch.shoppinglist.data.DbChecklistItem;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class AppViewModel extends AndroidViewModel {

    // All the app's business logic should be handled in the ViewModel.
    // It's the interface between data and UI.

    private static final String TAG = "AppViewModel";
    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private static final ListeningExecutorService mListeningExecutor = MoreExecutors.listeningDecorator(mExecutor);

    private final ChecklistRepository mChecklistRepo;
    private final LiveData<List<String>> mChecklistTitles;


    public AppViewModel(@NonNull Application application) {
        super(application);
        mChecklistRepo = new ChecklistRepository(application);
        mChecklistTitles = mChecklistRepo.getAllChecklistTitles();
    }

    public void setActiveChecklist(String checklistTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.setActiveChecklist(checklistTitle);
        });
    }

    public LiveData<String> getActiveChecklist() {
        return mChecklistRepo.getActiveChecklistTitle();
    }

    public LiveData<List<String>> getAllChecklistTitles() {
        return mChecklistTitles;
    }

    public void insertChecklist(String listTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.insertChecklist(listTitle);
            mChecklistRepo.setActiveChecklist(listTitle);
        });
    }

    public void deleteChecklist(String checklistTitle) {
        mExecutor.execute(() -> {
            mChecklistRepo.deleteChecklist(checklistTitle);
            if (mChecklistTitles.isInitialized()) {
                List<String> listTitles = mChecklistTitles.getValue();
                // Select the first list that is not this one
                // (Database may have not been updated yet, so
                // the just deleted item might still be in there).
                if (listTitles != null) {
                    for (String listTitle : listTitles) {
                        if (!listTitle.equals(checklistTitle)) {
                            mChecklistRepo.setActiveChecklist(listTitle);
                            break;
                        }
                    }
                }
            }
        });
    }

    public void renameChecklist(String checklistTitle, String newTitle) {
        assert mChecklistTitles.getValue() != null;
        if (mChecklistTitles.getValue().contains(newTitle)) {
            throw new IllegalArgumentException("Checklist title '" + newTitle + "' already present");
        } else {
            mExecutor.execute(() -> {
                mChecklistRepo.updateChecklistName(checklistTitle, newTitle);
            });
        }
    }

    public LiveData<List<ChecklistItem>> getItemsSortedByPosition(String listTitle, boolean isChecked) {
        return Transformations.map(
                mChecklistRepo.getItemsSortedByPositionLiveData(listTitle, isChecked),
                AppViewModel::toChecklistItems);
    }

    public ListenableFuture<Void> insertItem(final @NonNull String listTitle,
                                             final boolean isChecked,
                                             final @NonNull ChecklistItem item) {
        // TODO: strip white space from item name
        return mListeningExecutor.submit(() -> {
            if (item.getName().isEmpty()) {
                throw new Exception("Empty");
            }
            else if (mChecklistRepo.getAllItems(listTitle)
                    .stream()
                    .anyMatch(dbItem -> dbItem.getName().equals(item.getName()))) {
                throw new Exception("\"" + item.getName() + "\" already present");
            } else {
                List<DbChecklistItem> dbItems = mChecklistRepo.getItemsSortedByPosition(listTitle, isChecked);
                DbChecklistItem newDbItem = new DbChecklistItem(item.getName(), isChecked, 0, listTitle);
                if (isNewItemInsertBottom()) {
                    dbItems.add(newDbItem);
                } else {
                    dbItems.add(0, newDbItem);
                }
                updatePositionByOrder(dbItems);
                Log.d(TAG, "item: " + newDbItem.getName() + ": " + newDbItem.getPosition());
                mChecklistRepo.insertAndUpdate(newDbItem, dbItems);
                return null;
            }
        });
    }

    public void flipItem(String listTitle, boolean isChecked, ChecklistItem clItem) {
        mExecutor.execute(() -> {
            // TODO: 3/26/2024 Redo the whole thing
            List<DbChecklistItem> dbMirrorRemovedFrom = mChecklistRepo.getItemsSortedByPosition(listTitle, isChecked);
            DbChecklistItem repoItem = dbMirrorRemovedFrom.stream()
                    .filter(item -> item.getName().equals(clItem.getName()))
                    .findFirst()
                    .orElse(null);
            assert repoItem != null;

            boolean removed = dbMirrorRemovedFrom.remove(repoItem);
            assert removed;

            List<DbChecklistItem> dbMirrorAddTo = mChecklistRepo.getItemsSortedByPosition(listTitle, !isChecked);
            repoItem.setChecked(!repoItem.isChecked());
            if (repoItem.isChecked()) {
                dbMirrorAddTo.add(0, repoItem);
            } else {
                dbMirrorAddTo.add(repoItem);
            }

            updatePositionByOrder(dbMirrorRemovedFrom);
            updatePositionByOrder(dbMirrorAddTo);

            List<DbChecklistItem> dbMirrorCombined = new ArrayList<>();
            dbMirrorCombined.addAll(dbMirrorRemovedFrom);
            dbMirrorCombined.addAll(dbMirrorAddTo);
            // Make sure that only a single repo function is called (only single database update)
            mChecklistRepo.update(dbMirrorCombined);
        });
    }

    public void itemsHaveBeenMoved(String listTitle,
                                   boolean isChecked,
                                   final List<ChecklistItem> itemsSortedByPos) {
        // TODO: update "importance" of item if it has been moved in "checked" list
        mExecutor.execute(() -> {
            // TODO: Redo properly
            List<DbChecklistItem> dbMirror = mChecklistRepo.getItems(listTitle, isChecked);
            AtomicLong pos = new AtomicLong(0);
            itemsSortedByPos.forEach(item -> {
                DbChecklistItem found = dbMirror.stream()
                        .filter(item1 -> item1.getName().equals(item.getName()))
                        .findFirst().orElse(null);
                assert found != null;
                found.setPosition(pos.getAndIncrement());
            });
            mChecklistRepo.update(dbMirror);
        });
    }

    public boolean isNewItemInsertBottom() {
        // Dummy method (retrieve from settings)
        return true;
    }

    private static List<ChecklistItem> toChecklistItems(List<DbChecklistItem> dbItems) {
        return dbItems.stream()
                .map(dbChecklistItem -> new ChecklistItem(
                        dbChecklistItem.getName()))
                .collect(Collectors.toList());
    }

    private static void updatePositionByOrder(List<DbChecklistItem> repoItems) {
        for (int i = 0; i < repoItems.size(); i++) {
            repoItems.get(i).setPosition(i);
        }
    }
}
