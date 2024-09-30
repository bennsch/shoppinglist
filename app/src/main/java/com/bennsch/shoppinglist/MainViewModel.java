package com.bennsch.shoppinglist;

import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.bennsch.shoppinglist.data.ChecklistRepository;
import com.bennsch.shoppinglist.data.DbChecklistItem;
import com.bennsch.shoppinglist.data.PreferencesRepository;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class MainViewModel extends AndroidViewModel {

    // TODO: Remove all logic from GUI. The ViewModel should contain all logic.
    //  (e.g. onAddButtonClicked()...also, config values should be defined here(e.g. autocompl threshold).

    // All the app's business logic should be handled in the ViewModel.
    // It's the interface between data and UI.

    public static final int AUTOCOMPLETE_THRESHOLD = 0;
    public static final int LIST_TITLE_MAX_LENGTH = 50;

    private static final String TAG = "AppViewModel";
    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private static final ListeningExecutorService mListeningExecutor = MoreExecutors.listeningDecorator(mExecutor);

    private final ChecklistRepository mChecklistRepo;
    private final PreferencesRepository mPreferencesRepo;
    private final LiveData<List<String>> mChecklistTitles;
    private final MutableLiveData<Boolean> mDeleteIconsVisible;


    public MainViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "AppViewModel: CTOR");
        mChecklistRepo = new ChecklistRepository(application);
        mPreferencesRepo = PreferencesRepository.getInstance(application);
        mChecklistTitles = mChecklistRepo.getAllChecklistTitles();
        mDeleteIconsVisible = new MutableLiveData<>(false);
    }

    @NonNull
    public String getVersionName() {
        try {
            return getApplication()
                    .getPackageManager()
                    .getPackageInfo(getApplication().getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getVersionName: ", e);
            return "?.?";
        }
    }

    public LiveData<String> getPrefMessageListDeleted() {
        return mPreferencesRepo.getPrefMessageListDeleted();
    }

//    @NonNull
//    public String getAppName() {
//        return getApplication()
//                .getApplicationInfo()
//                .loadLabel(getApplication().getPackageManager())
//                .toString();
//    }

    public void toggleDeleteIconsVisibility() {
        Boolean visible = mDeleteIconsVisible.getValue();
        assert visible != null;
        visible = !visible;
        mDeleteIconsVisible.postValue(visible);
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

    public LiveData<Boolean> isChecklistEmpty(String listTitle) {
        MediatorLiveData<Boolean> mediator = new MediatorLiveData<>();
        mediator.addSource(
                    mChecklistRepo.getAllItemsLiveData(listTitle),
                    dbChecklistItems
                        -> mediator.setValue(dbChecklistItems.isEmpty()));
        return Transformations.distinctUntilChanged(mediator);
    }

    public LiveData<String> getActiveChecklist() {
        return mChecklistRepo.getActiveChecklistTitle();
    }

    public LiveData<List<String>> getAllChecklistTitles() {
        return mChecklistTitles;
    }

    public LiveData<List<String>> getAutoCompleteItems(@NonNull String listTitle, @Nullable Boolean isChecked) {
        if (isChecked == null || isChecked) {
            // If the user is currently looking at "checked" items, then
            // we don't show any auto complete suggestions.
            return new MutableLiveData<>(new ArrayList<>(0));
        } else {
            // TODO: map runs on UI-thread!
            // Show all items (both checked and unchecked) as auto complete suggestions
            return Transformations.map(
                    mChecklistRepo.getAllItemsLiveData(listTitle),
                    dbChecklistItems
                            -> dbChecklistItems
                            .stream()
                            .map(DbChecklistItem::getName)
                            .collect(Collectors.toList()));

        }
    }

    public String validateNewChecklistName(String newTitle) throws IllegalArgumentException{
        String newTitleStripped = stripWhitespace(newTitle);
        List<String> currentTitles = getAllChecklistTitles().getValue();
        assert currentTitles != null;
        // TODO: replace with appropriate exception
        if (currentTitles.contains(newTitleStripped)) {
            throw new IllegalArgumentException("Name already in use");
        } else if (newTitleStripped.length() > LIST_TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("Name is too long");
        } else if (newTitleStripped.isEmpty()) {
            throw new IllegalArgumentException("Name is empty");
        } else {
            return newTitleStripped;
        }
    }

    public void insertChecklist(String listTitle) throws IllegalArgumentException{
        String listTitleValidated = validateNewChecklistName(listTitle);
        mExecutor.execute(() -> {
            mChecklistRepo.insertChecklist(listTitleValidated);
            mChecklistRepo.setActiveChecklist(listTitleValidated);
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

    public void renameChecklist(@NonNull final String title, @NonNull final String newTitle) throws IllegalArgumentException{
        assert mChecklistTitles.getValue() != null;
        if (mChecklistTitles.getValue().contains(title)) {
            String newTitleValidated = validateNewChecklistName(newTitle);
            mExecutor.execute(() -> {
                mChecklistRepo.updateChecklistName(title, newTitleValidated);
            });
        } else {
            throw new IllegalArgumentException("List \"" + title + "\" doesn't exist" );
        }
    }

    public LiveData<List<ChecklistItem>> getItemsSortedByPosition(String listTitle, boolean isChecked) {
        return Transformations.map(
                mChecklistRepo.getItemsSortedByPositionLiveData(listTitle, isChecked),
                MainViewModel::toChecklistItems);
    }

    public ListenableFuture<Void> insertItem(final @NonNull String listTitle,
                                             final boolean isChecked,
                                             final @NonNull String name) {
        return mListeningExecutor.submit(() -> {
            // Remove leading and trailing spaces, and replace all multi-spaces with single
            // spaces.
            String strippedName = stripWhitespace(name);

            if (strippedName.isEmpty()) {
                throw new Exception("Empty");
            } else {
                DbChecklistItem dbItem = findDbItem(mChecklistRepo.getAllItems(listTitle), strippedName);
                if (dbItem == null){
                    // Item does not exist in database. Insert a new item.
                    List<DbChecklistItem> dbItems = mChecklistRepo.getItemsSortedByPosition(listTitle, isChecked);
                    // TODO: maybe don't use integral type for 'position', so that undefined position is allowed
                    DbChecklistItem newDbItem = new DbChecklistItem(strippedName, isChecked, 0, listTitle, 0);
                    dbItems.add(newDbItem);
                    if (isChecked) {
                        sortByIncidenceDescending(dbItems);
                    }
                    updatePositionByOrder(dbItems);
                    Log.d(TAG, "insertItem: \"" + newDbItem.getName() + "\"");
                    mChecklistRepo.insertAndUpdate(newDbItem, dbItems);
                } else {
                    // Item already exists in database.
                    if (dbItem.isChecked() == isChecked) {
                        // The existing item is of the same category as the user tries to add to,
                        // so just move it to the bottom.
                        List<DbChecklistItem> items = mChecklistRepo.getItemsSortedByPosition(listTitle, dbItem.isChecked());
                        items.remove((int)dbItem.getPosition());
                        items.add(dbItem);
                        updatePositionByOrder(items);
                        mChecklistRepo.update(items);
                    } else {
                        // The item is of the other category, so flip it.
                        flipItem(listTitle, dbItem.isChecked(), new ChecklistItem(dbItem.getName(), dbItem.getIncidence()));
                    }
                }
                return null;
            }
        });
    }

    public void flipItem(String listTitle, boolean isChecked, ChecklistItem clItem) {
        mExecutor.execute(() -> {
            // TODO: 3/26/2024 Redo the whole thing
            //  (we don't need to worry about white spaces, since this function
            //   is only for "moving" items, not changing their names)
            // TODO: don't use "isChecked" as argument. Get that info from dbItem (findDbItem)
            // TODO: use String name instead of CHeckListItem argument
            List<DbChecklistItem> dbMirrorRemovedFrom = mChecklistRepo.getItemsSortedByPosition(listTitle, isChecked);
            DbChecklistItem repoItem = findDbItem(dbMirrorRemovedFrom, clItem.getName());
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
            DbChecklistItem dbItem = findDbItem(mChecklistRepo.getAllItems(listTitle), clItem.getName());
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
                //  (we don't need to worry about white spaces, since this function
                //   is only for "moving" items, not changing their names)
                DbChecklistItem found = findDbItem(dbMirror, name);
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

    private static String stripWhitespace(@NonNull final String s) {
        return s.strip().replaceAll(" +", " ");
    }

    @Nullable
    private DbChecklistItem findDbItem(List<DbChecklistItem> dbItems, @NonNull String name) {
        // This function should always be used if an item needs to be found
        // in the database from its name.
        // An item can be unambiguously identified by its list title and name
        // (no duplicate items allowed in a list).
        return dbItems
                .stream()
                .filter(item -> item.getName().equalsIgnoreCase(name))
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
