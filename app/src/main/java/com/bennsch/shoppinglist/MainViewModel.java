package com.bennsch.shoppinglist;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.bennsch.shoppinglist.datamodel.ChecklistRepository;
import com.bennsch.shoppinglist.datamodel.DbChecklistItem;
import com.bennsch.shoppinglist.datamodel.PreferencesRepository;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import kotlin.jvm.functions.Function1;


public class MainViewModel extends AndroidViewModel {
    /*
     *  The ViewModel is the interface between the UI-layer (Activities, Fragments etc.) and the
     *  data-layer (databases, repositories etc.). All the application's business logic should be
     *  handled here. The ViewModel should be unaware of any UI implementation details.
     *
     *  Note: The UI-layer components (Activities, Fragments etc.) should not perform any logic or
     *  modify data. Their sole purpose is to display the data provided by the ViewModel, and to
     *  notify the ViewModel about external events (e.g user inputs).
     *
     *  Example: User presses a button to add a new item:
     *
     *     1) User presses button.
     *
     *     2) UI-layer (e.g. MainActivity) notifies the ViewModel that a new item needs to be added.
     *
     *     3) ViewModel performs some logic (e.g. check item contents, sort items etc.) and adds a
     *        new item to the data repository.
     *
     *     4) UI-layer, which is observing the items (LiveData holders retrieved from the ViewModel),
     *        gets notified that the items have changed and updates the UI accordingly.
     */


    public static class InvalidNameException extends Exception {
        public InvalidNameException(String reason){
            super(reason);
        }
    }

    public static class Onboarding {
        /*
         *  This class encapsulates all the logic related to the onboarding process.
         *  When the app is launched for the first time, a series of hints are displayed introducing
         *  the user how to use the app. It's implemented as a state machine, that advances depending
         *  on the user's actions.
         */

        // Events triggered by the user.
        public enum Event {
            // Start the onboarding process.
            START_ONBOARDING,
            // A list item has been tapped.
            ITEM_TAPPED,
            // The ViewPager has been swiped to the "checked" page.
            SWIPED_TO_CHECKED,
            // The ViewPager has been swiped to the "unchecked" page.
            SWIPED_TO_UNCHECKED,
            // The last item in the list got deleted, or an empty list has been selected.
            LIST_EMPTY,
            // The first item was added to the list, or a non-empty list has been selected
            // (this event is also triggered if the phone's dark theme is enabled/disabled or if
            // the app is restarted).
            LIST_NOT_EMPTY
        }

        // Hints to be displayed to the user.
        public enum Hint {
            // Onboarding not started yet or cancelled
            INIT,
            // Tell the user that "unchecked" items can be tapped to check them
            TAP_ITEM_TO_CHECK,
            // Tell the user how to swipe to the "checked" page
            SWIPE_TO_CHECKED,
            // Tell the user that "checked" items can be tapped to uncheck them
            TAP_ITEM_TO_UNCHECK,
            // Tell the user how to swipe to the "unchecked" page
            SWIPE_TO_UNCHECKED,
            // Onboarding complete
            COMPLETED
        }

        public interface CompletedListener {
            void onCompleted();
        }

        private final MutableLiveData<Hint> mHint;
        private final CompletedListener mCompletedListener;

        // Determine the next hint based on the event and the current hint.
        // Null means to remain at the current hint.
        private static final Hint[][] NEXT_HINT_LOOKUP = {
        /*                          START_ONBOARDING        ITEM_TAPPED             SWIPED_TO_CHECKED           SWIPED_TO_UNCHECKED         LIST_EMPTY      LIST_NOT_EMPTY          */
        /* INIT                 */ {Hint.TAP_ITEM_TO_CHECK, null,                   null,                       null,                       null,           Hint.TAP_ITEM_TO_CHECK},
        /* TAP_ITEM_TO_CHECK    */ {null,                   Hint.SWIPE_TO_CHECKED,  Hint.TAP_ITEM_TO_UNCHECK,   null,                       Hint.INIT,      Hint.TAP_ITEM_TO_CHECK},
        /* SWIPE_TO_CHECKED     */ {null,                   null,                   Hint.TAP_ITEM_TO_UNCHECK,   null,                       Hint.INIT,      Hint.TAP_ITEM_TO_CHECK},
        /* TAP_ITEM_TO_UNCHECK  */ {null,                   Hint.SWIPE_TO_UNCHECKED,null,                       Hint.COMPLETED,             Hint.INIT,      Hint.TAP_ITEM_TO_CHECK},
        /* SWIPE_TO_UNCHECKED   */ {null,                   null,                   null,                       Hint.COMPLETED,             Hint.INIT,      Hint.TAP_ITEM_TO_CHECK},
        /* COMPLETED            */ {null,                   null,                   null,                       null,                       null,           null},
        };

        private Onboarding(boolean isCompleted, @NonNull CompletedListener listener) {
            mCompletedListener = listener;
            if (isCompleted) {
                mHint = new MutableLiveData<>(Hint.COMPLETED);
            } else {
                mHint = new MutableLiveData<>(Hint.INIT);
            }
        }

        public LiveData<Hint> getHint() {
            return Transformations.distinctUntilChanged(mHint);
        }

        public void notify(Event event) {
            assert mHint.getValue() != null;
            int idxHint = mHint.getValue().ordinal();
            int idxEvent = event.ordinal();
            Hint nextHint = NEXT_HINT_LOOKUP[idxHint][idxEvent];
            Log.e("BEN", "notify event = " + event + ", nextHint = " + nextHint);
            if (nextHint != null) {
                mHint.setValue(nextHint);
                if (nextHint == Hint.COMPLETED) {
                    mCompletedListener.onCompleted();
                }
            }
        }
    }

    public static class DeleteItemsMode {
        /*
         *  This class encapsulates all the logic related to the "Delete Items" mode.
         *  When the mode is enabled, the user can click items to delete them.
         */

        // DeleteItemsMode cannot be activated (i.e. the active Checklist is empty).
        public static final int DISABLED = 0;
        // DeleteItemsMode is activated. Items can now be deleted.
        public static final int ACTIVATED = 1;
        // DeleteItemsMode is deactivated. Items cannot be deleted currently.
        public static final int DEACTIVATED = 2;

        private final MediatorLiveData<Integer> mValue;

        public DeleteItemsMode(int initValue,
                               @NonNull LiveData<String> activeChecklist,
                               @NonNull Function1<String, LiveData<Boolean>> isChecklistEmpty) {
            mValue = new MediatorLiveData<>(initValue);
            // Disable DeleteItemsMode if the active Checklist is or becomes empty. Set it to
            // DEACTIVATED once items have been added again (list not empty anymore).
            mValue.addSource(
                    Transformations.switchMap(activeChecklist, isChecklistEmpty),
                    isActiveChecklistEmpty -> mValue.setValue(
                            isActiveChecklistEmpty ? DISABLED : DEACTIVATED));
        }

        public int getValue() {
            assert mValue.getValue() != null;
            return mValue.getValue();
        }

        public void observe(@NonNull LifecycleOwner owner,
                            @NonNull Observer<Integer> observer) {
            mValue.observe(owner, observer);
        }

        public void toggle() {
            // Toggle DeleteItemsMode, unless it's DISABLED.
            switch (getValue()) {
                case ACTIVATED:
                    mValue.setValue(DEACTIVATED);
                    break;
                case DEACTIVATED:
                    mValue.setValue(ACTIVATED);
                    break;
                case DISABLED:
                    break;
                default:
                    assert false: "Invalid value: " + getValue();
            }
        }
    }


    public static final int AUTOCOMPLETE_THRESHOLD = 0;
    public static final int LIST_TITLE_MAX_LENGTH = 50;

    private static final String TRASH_LABEL = "__TRASH__";

    // Room-Database queries must be executed on a separate thread.
    // NOTE: Each time an executor is called, it should only perform a single Room-database
    // transaction, to avoid concurrent LiveData updates!
    private static final ExecutorService mExecutor =
            Executors.newSingleThreadExecutor();
    // Executor that can throw an exception.
    private static final ListeningExecutorService mListeningExecutor =
            MoreExecutors.listeningDecorator(mExecutor);

    private final ChecklistRepository mChecklistRepo;
    private final PreferencesRepository mPreferencesRepo;
    private final LiveData<List<String>> mChecklistTitles;

    // We can store the below data in the MainViewModel itself (instead of repositories), because
    // we don't need to keep it when the app finishes:
    private final DeleteItemsMode mDeleteItemsMode;
    private final Onboarding mOnboarding;
    private final MutableLiveData<Boolean> mAreItemsDragged;


    public MainViewModel(@NonNull Application application) {
        super(application);
        mChecklistRepo = ChecklistRepository.getInstance(application.getApplicationContext());
        mPreferencesRepo = PreferencesRepository.getInstance(application.getApplicationContext());
        mChecklistTitles = mChecklistRepo.getAllChecklistTitles();
        mDeleteItemsMode = new DeleteItemsMode(
                DeleteItemsMode.DISABLED,
                getActiveChecklist(),
                this::isChecklistEmpty);
        mAreItemsDragged = new MutableLiveData<>(false);
        mOnboarding = new Onboarding(
                mPreferencesRepo.getPrefOnboardingCompleted(),
                () -> mPreferencesRepo.setPrefOnboardingCompleted(true));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public void setItemsDragged(boolean dragged) {
        mAreItemsDragged.setValue(dragged);
    }

    @NonNull
    public LiveData<Boolean> areItemsDragged() { // Casting to immutable LiveData for public API
        return mAreItemsDragged;
    }

    @NonNull
    public DeleteItemsMode getDeleteItemsMode() {
        return mDeleteItemsMode;
    }

    @NonNull
    public Onboarding getSimpleOnboarding() {
        return mOnboarding;
    }

    @NonNull
    public String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public void setActiveChecklist(String checklistTitle) {
        mExecutor.execute(() -> mChecklistRepo.setActiveChecklist(checklistTitle));
    }

    public LiveData<Boolean> isChecklistEmpty(String listTitle) {
        MediatorLiveData<Boolean> mediator = new MediatorLiveData<>();
        mediator.addSource(
                    mChecklistRepo.getItemsLiveData(listTitle),
                    dbChecklistItems
                        -> mediator.setValue(dbChecklistItems.isEmpty()));
        return Transformations.distinctUntilChanged(mediator);
    }

    public LiveData<String> getActiveChecklist() {
        return mChecklistRepo.getActiveChecklistTitle();
    }

    public LiveData<List<String>> getAllChecklistTitles(boolean includeTrash) {
        if (includeTrash) {
            return mChecklistTitles;
        } else {
            MediatorLiveData<List<String>> filtered = new MediatorLiveData<>();
            filtered.addSource(mChecklistTitles, listTitles -> {
                filtered.setValue(
                        listTitles.stream()
                                .filter(listTitle -> !listTitle.contains(TRASH_LABEL))
                                .collect(Collectors.toList()));
            });
            return Transformations.distinctUntilChanged(filtered);
        }
    }

    public LiveData<List<String>> getAutoCompleteDataset(@NonNull String listTitle,
                                                         boolean isCheckedVisible) {
        // Return a list of strings that should be used as the dataset for the adapter of an
        // AutoCompleteTextView. It's the names of all the items (both checked and unchecked) in the
        // Checklist titled "listTitle". Which of those suggestions will actually be displayed to
        // the user depends on the user input and is handled in the AutoCompleteTextView.
        if (isCheckedVisible) {
            // If the user is currently looking at checked items, then we won't show any
            // suggestions, because the user probably wants to only add new items anyway.
            return new MutableLiveData<>(new ArrayList<>(0));
        } else {
            // TODO: map() runs on UI-thread!
            return Transformations.map(
                    mChecklistRepo.getItemsLiveData(listTitle),
                    dbChecklistItems
                            -> dbChecklistItems
                            .stream()
                            .map(DbChecklistItem::getName)
                            .collect(Collectors.toList()));

        }
    }

    public String validateChecklistTitle(String listTitle) throws InvalidNameException {
        // Strip white spaces, validate it can be used for a new Checklist and return the stripped
        // title.
        String listTitleStripped = stripWhitespace(listTitle);
        List<String> currentTitles = mChecklistTitles.getValue();
        assert currentTitles != null;
        if (currentTitles.contains(listTitleStripped)) {
            throw new InvalidNameException("Name already in use");
        } else if (listTitleStripped.length() > LIST_TITLE_MAX_LENGTH) {
            throw new InvalidNameException("Name exceeds max. length");
        } else if (listTitleStripped.isEmpty()) {
            throw new InvalidNameException("Name is empty");
        } else {
            return listTitleStripped;
        }
    }

    public void insertChecklist(String listTitle) throws InvalidNameException {
        String listTitleValidated = validateChecklistTitle(listTitle);
        mExecutor.execute(() -> {
            mChecklistRepo.insertChecklist(listTitleValidated);
            mChecklistRepo.setActiveChecklist(listTitleValidated);
        });
    }

    public void moveChecklistToTrash(String checklistTitle) throws InvalidNameException {
        // Moving a Checklist to the trash means prefixing its title with TRASH_LABEL.
        // Those Checklists won't be displayed in the NavDrawer.
        assert mChecklistTitles.getValue() != null;
        if (!mChecklistTitles.getValue().contains(checklistTitle)) {
            throw new InvalidNameException("List \"" + checklistTitle +  "\" does not exists");
        }
        mExecutor.execute(() -> {
            String trashed_title =
                    TRASH_LABEL + checklistTitle + "(" + Calendar.getInstance().getTime() + ")";
            mChecklistRepo.updateChecklistTitle(checklistTitle, trashed_title);
            assert mChecklistTitles.getValue() != null;
            String ac = mChecklistTitles.getValue().stream()
                    .filter(listTitle ->
                            !listTitle.contains(TRASH_LABEL) &&
                            // Required because old list title is still present at this point (why?)
                            !listTitle.equals(checklistTitle))
                    .findFirst()
                    .orElse(null);
            mChecklistRepo.setActiveChecklist(ac);
        });
    }

    public void renameChecklist(@NonNull final String title,
                                @NonNull final String newTitle) throws InvalidNameException {
        assert mChecklistTitles.getValue() != null;
        if (mChecklistTitles.getValue().contains(title)) {
            String newTitleValidated = validateChecklistTitle(newTitle);
            mExecutor.execute(() -> {
                mChecklistRepo.updateChecklistTitle(title, newTitleValidated);
            });
        } else {
            throw new InvalidNameException("List \"" + title + "\" doesn't exist" );
        }
    }

    public LiveData<List<ChecklistItem>> getItemsSortedByPosition(String listTitle,
                                                                  boolean isChecked) {
        return Transformations.map(
                mChecklistRepo.getItemSubsetSortedLiveData(listTitle, isChecked),
                MainViewModel::toChecklistItems);
    }

    public void deleteItem(@NonNull String listTitle, @NonNull ChecklistItem clItem) {
        mExecutor.execute(() -> {
            DbChecklistItem dbItem =
                    findDbItem(mChecklistRepo.getItems(listTitle), clItem.getName());
            assert dbItem != null;
            mChecklistRepo.deleteItem(dbItem);
        });
    }

    public ListenableFuture<Void> insertItem(final @NonNull String listTitle,
                                             final boolean isChecked,
                                             final @NonNull String name) {
        // Insert a new item named "name" to Checklist "listTitle".
        // If an item with the same name already exists, then either move it to the bottom of the
        // list (if "isChecked" equals the existing item's "isChecked"), or flip it (if "isChecked"
        // differs).
        return mListeningExecutor.submit(() -> {
            String strippedName = stripWhitespace(name);
            if (strippedName.isEmpty()) {
                throw new InvalidNameException("Name is empty");
            }
            DbChecklistItem dbItem = findDbItem(mChecklistRepo.getItems(listTitle), strippedName);
            if (dbItem == null){
                // Item does not exist in database, so insert a new item.
                // The new item will have the lowest incidence.
                long incidence = mChecklistRepo.getMinIncidence(listTitle) - 1;
                DbChecklistItem newDbItem = new DbChecklistItem(
                        strippedName, isChecked, null, listTitle, incidence);
                // Within each executor runnable we must only perform a single database transaction
                // to avoid multiple LiveData updates. So we need to get a copy the list, modify
                // the copy and then update the database using that copy.
                List<DbChecklistItem> dbItems =
                        mChecklistRepo.getItemSubsetSorted(listTitle, isChecked);
                dbItems.add(newDbItem);
                // This function updates all positions, but we only care about the newDbItem's
                // position.
                updatePositionByOrder(dbItems);
                // Single database transaction
                mChecklistRepo.insertAndUpdateItems(newDbItem, dbItems);
            } else {
                // Item already exists in database.
                if (dbItem.isChecked() == isChecked) {
                    // The user tries to add an item with the same "isChecked" as the existing item,
                    // so just move it to the bottom of the list (as a visual feedback).
                    List<DbChecklistItem> items = mChecklistRepo
                            .getItemSubsetSorted(listTitle, dbItem.isChecked());
                    assert dbItem.getPosition() != null: "dbItem.getPosition() returned null";
                    // Remove and add the same item to move it to the end of the list.
                    items.remove((int)dbItem.getPosition());
                    items.add(dbItem);
                    updatePositionByOrder(items);
                    mChecklistRepo.updateItems(items);
                } else {
                    // The user tries to add an item with the opposite "isChecked", so we can simply
                    // flip the existing item (to make it appear in the list that the user is trying
                    // to add it to).
                    flipItem(listTitle, dbItem.getName());
                }
            }
            return null;
        });
    }

    public void flipItem(String listTitle, String name) {
        // Move an item from "checked" to "unchecked" and vice versa, and increment the
        // item's incidence.
        mExecutor.execute(() -> {
            // Get a copy of the list in the database, so that we can apply several modifications
            // but only perform a single database transaction at the end.
            List<DbChecklistItem> allItems = mChecklistRepo.getItems(listTitle);
            DbChecklistItem itemToFlip = findDbItem(allItems, name);
            assert itemToFlip != null: "findDbItem() returned null for name == " + name;
            // If an item is flipped from "checked" to "unchecked", we want it to be placed at
            // the end of the list.
            if (itemToFlip.isChecked()) {
                itemToFlip.setPosition(Integer.MAX_VALUE);
            }
            // Increment the incidence every time an item is flipped.
            itemToFlip.setIncidence(itemToFlip.getIncidence() + 1);
            // Invert "isChecked", i.e. flip it.
            itemToFlip.setChecked(!itemToFlip.isChecked());
            // After the item has been flipped, we need to update the positions of all items.
            // Items are sorted differently depending on whether they are "checked" or "unchecked",
            // so we need to filter them first:
            List<DbChecklistItem> uncheckedItems = filterByChecked(allItems, false);
            sortByPositionAscending(uncheckedItems);
            updatePositionByOrder(uncheckedItems);
            List<DbChecklistItem> checkedItems = filterByChecked(allItems, true);
            sortByIncidenceDescending(checkedItems); // Only "checked" items are sorted by incidence
            updatePositionByOrder(checkedItems);
            // Commit the local changes in a single database transaction
            mChecklistRepo.updateItems(allItems);
        });
    }

    public void itemsHaveBeenMoved(String listTitle,
                                   boolean areChecked,
                                   final List<ChecklistItem> items) {
        // Update the database's item positions to match the order as they are in "items". If
        // "checked" items have been moved, update the their incidences accordingly.
        mExecutor.execute(() -> {
            // Get a copy of the list in the database, so that we can apply several modifications
            // but only perform a single database transaction at the end.
            List<DbChecklistItem> dbItems
                    = mChecklistRepo.getItemSubsetSorted(listTitle, areChecked);
            // Number of items should match the database.
            assert items.size() == dbItems.size(): "Unexpected number of items";
            long prevIncidence = 0;
            for (int i = 0; i < items.size(); i++) {
                ChecklistItem item = items.get(i);
                DbChecklistItem dbItem = findDbItem(dbItems, item.getName());
                assert dbItem != null: "Could not find item with name = " + item.getName();
                dbItem.setPosition(i);
                if (areChecked && (i > 0)) {
                    if (item.getIncidence() >= prevIncidence) {
                        // Note: Incidences may become negative to maintain existing order.
                        dbItem.setIncidence(prevIncidence - 1);
                    }
                }
                prevIncidence = dbItem.getIncidence();
            }
            // Commit the local changes in a single database transaction
            mChecklistRepo.updateItems(dbItems);
        });
    }

    // Helper functions:

    private static String stripWhitespace(@NonNull final String s) {
        // Remove leading and trailing spaces, and replace all multi-spaces with single
        // spaces.
        return s.strip().replaceAll(" +", " ");
    }

    private static List<ChecklistItem> toChecklistItems(List<DbChecklistItem> dbItems) {
        // Convert DbChecklistItems to ChecklistItems.
        return dbItems.stream()
                .map(dbChecklistItem -> new ChecklistItem(
                        dbChecklistItem.getName(),
                        dbChecklistItem.getIncidence()))
                .collect(Collectors.toList());
    }

    @Nullable
    private DbChecklistItem findDbItem(List<DbChecklistItem> dbItems, @NonNull String name) {
        List<DbChecklistItem> found = dbItems
                .stream()
                .filter(item -> item.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
        assert found.size() <= 1 : "More than one item found with name = " + name;
        if (found.isEmpty()) {
            return null;
        } else {
            return found.get(0);
        }
    }

    private static List<DbChecklistItem> filterByChecked(List<DbChecklistItem> items,
                                                         boolean isChecked) {
        return items.stream()
                .filter(item -> item.isChecked() == isChecked)
                .collect(Collectors.toList());
    }

    private static void updatePositionByOrder(List<DbChecklistItem> dbItems) {
        // Update the "position" field of each item based on its position in the list starting
        // with 0. List is modified in place.
        for (int i = 0; i < dbItems.size(); i++) {
            dbItems.get(i).setPosition(i);
        }
    }

    private static void sortByPositionAscending(List<DbChecklistItem> dbItems) {
        // List is modified in place.
        dbItems.sort(
                Comparator.comparing(
                        DbChecklistItem::getPosition,
                        Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private static void sortByIncidenceDescending(List<DbChecklistItem> dbItems) {
        // List is modified in place.
        dbItems.sort(
                Comparator.comparingLong(
                        DbChecklistItem::getIncidence)
                .reversed());
    }
}
