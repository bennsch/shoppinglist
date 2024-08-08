package com.bennsch.shoppinglist;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;

import com.bennsch.shoppinglist.data.ChecklistRepository;
import com.bennsch.shoppinglist.data.DbChecklist;
import com.bennsch.shoppinglist.data.DbChecklistItem;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class AppViewModel extends AndroidViewModel {

    // All the app's business logic should be handled in the ViewModel.
    // It's the interface between data and UI.

    private static final String TAG = "AppViewModel";
    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private static final ListeningExecutorService mListeningExecutor = MoreExecutors.listeningDecorator(mExecutor);

    private final ChecklistRepository mChecklistRepo;
    private final LiveData<List<String>> mChecklistTitles;
    private final MutableLiveData<Boolean> mDeleteIconsVisible;


    public AppViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "AppViewModel: CTOR");
        mChecklistRepo = new ChecklistRepository(application);
        mChecklistTitles = mChecklistRepo.getAllChecklistTitles();
        mDeleteIconsVisible = new MutableLiveData<>(false);
    }

    public void toggleDeleteIconsVisibility() {
        Boolean visible = mDeleteIconsVisible.getValue();
        assert visible != null;
        mDeleteIconsVisible.postValue(!visible);
    }

    public LiveData<Boolean> getDeleteIconsVisible() {
        return mDeleteIconsVisible;
    }

    public boolean isDeleteIconVisible() {
        Boolean visible = mDeleteIconsVisible.getValue();
        assert visible != null;
        return visible;
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
        assert mChecklistTitles.getValue() != null;
        if (mChecklistTitles.getValue().contains(listTitle)) {
            // TODO: replace with appropriate exception
            throw new IllegalArgumentException("List with title \"" + listTitle +  "\" already exists");
        }
        mExecutor.execute(() -> {
            mChecklistRepo.insertChecklist(listTitle);
            mChecklistRepo.setActiveChecklist(listTitle);
        });
    }

    public void deleteChecklist(String checklistTitle) {
        assert mChecklistTitles.getValue() != null;
        if (!mChecklistTitles.getValue().contains(checklistTitle)) {
            // TODO: replace with appropriate exception
            throw new IllegalArgumentException("List with title \"" + checklistTitle +  "\" does not exists");
        }
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
            // TODO: replace with appropriate exception
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
                // TODO: maybe don't use integral type for 'position', so that undefined position is allowed
                DbChecklistItem newDbItem = new DbChecklistItem(item.getName(), isChecked, 0, listTitle, 0);
                dbItems.add(newDbItem);

                if (isChecked) {
                    sortByIncidenceDescending(dbItems);
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

            repoItem.setIncidence(repoItem.getIncidence() + 1);
            dbMirrorAddTo.add(repoItem);
            if (repoItem.isChecked()) {
                sortByIncidenceDescending(dbMirrorAddTo);
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

    public void deleteItem(@NonNull String listTitle, @NonNull ChecklistItem clItem) {
        mExecutor.execute(() -> {
            DbChecklistItem dbItem = findDbItem(listTitle, clItem);
            assert dbItem != null;
            mChecklistRepo.deleteItem(dbItem);
        });
    }

    public void itemsHaveBeenMoved(String listTitle,
                                   boolean isChecked,
                                   final List<ChecklistItem> itemsSortedByPos) {
        mExecutor.execute(() -> {
            // TODO: Redo properly
            // Update the database items (position) to match the current visual order
            // in RecyclerView and modify the incidence accordingly.
            // This is performed only when "checked" items have been moved.
            List<DbChecklistItem> dbMirror = mChecklistRepo.getItems(listTitle, isChecked);
            long prev_incidence = 0;
            for (int i = 0; i < itemsSortedByPos.size(); i++) {
                // Find the corresponding database item.
                String name = itemsSortedByPos.get(i).getName();
                // TODO: does it matter if we search dbMirror or use findDbItem()?
                DbChecklistItem found = dbMirror.stream()
                        .filter(item1 -> item1.getName().equals(name))
                        .findFirst().orElse(null);
                assert found != null;
                // Update incidence so that it less than the previous incidence.
                long current_incidence = itemsSortedByPos.get(i).getIncidence();
                if (isChecked && (i > 0)){ // Update incidence only for checked items.
                    if (current_incidence >= prev_incidence) {
                        found.setIncidence(prev_incidence - 1);
                    }
                }
                prev_incidence = found.getIncidence();
                // Update position of database item.
                found.setPosition(i);
            }
            mChecklistRepo.update(dbMirror);
        });
    }

    @Nullable
    private DbChecklistItem findDbItem(String listTitle, ChecklistItem clItem) {
        // An item can be unambiguously identified by its list title and name
        // (no duplicate items allowed in a list).
        return mChecklistRepo.getAllItems(listTitle)
                .stream()
                .filter(item -> item.getName().equals(clItem.getName()))
                .findFirst().orElse(null);
    }

    private static List<ChecklistItem> toChecklistItems(List<DbChecklistItem> dbItems) {
        return dbItems.stream()
                .map(dbChecklistItem -> new ChecklistItem(
                        dbChecklistItem.getName(),
                        dbChecklistItem.getIncidence()))
                .collect(Collectors.toList());
    }

    private static void updatePositionByOrder(List<DbChecklistItem> repoItems) {
        for (int i = 0; i < repoItems.size(); i++) {
            repoItems.get(i).setPosition(i);
        }
    }

    private static void sortByIncidenceDescending(List<DbChecklistItem> repoItems) {
        repoItems.sort(Comparator.comparingLong(DbChecklistItem::getIncidence).reversed());
    }
}
