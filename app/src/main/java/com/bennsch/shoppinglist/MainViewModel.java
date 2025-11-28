package com.bennsch.shoppinglist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.bennsch.shoppinglist.data.ChecklistRepository;
import com.bennsch.shoppinglist.data.DbChecklistItem;
import com.bennsch.shoppinglist.data.PreferencesRepository;
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
     * The ViewModel is the interface between the UI-layer (Activities, Fragments etc.) and the
     * data-layer (databases, repositories etc.). All the application's business logic should be
     * handled here. The ViewModel should be unaware of any UI implementation details. A single
     * instance of the MainViewModel is used for the entire app.
     *
     * Note: The UI-layer components (Activities, Fragments etc.) should not perform any logic or
     * modify data. Their sole purpose is to display the data provided by the ViewModel, and to
     * notify the ViewModel about external events (e.g user inputs).
     *
     * Example: User presses a button to add a new item:
     *
     *    1) User presses button
     *
     *    2) UI-layer (e.g. MainActivity) notifies the ViewModel that a new item needs to be added
     *
     *    3) ViewModel performs some logic related to adding a new item (e.g. check item contents)
     *       and adds a new item to the data repository
     *
     *    4) UI-layer, which is observing the items (LiveData objects retrieved from the ViewModel),
     *       gets notified that the items have changed and updates the UI accordingly.
     */


    public static class Onboarding {
        /*
         * This class encapsulates all the logic related to the onboarding process.
         * When the app is launched for the first time, a series of hints are displayed introducing
         * the user how to use the app. It's implemented as a state machine, that advances depending
         * on the user's actions.
         */

        public enum Event {
            START_ONBOARDING, // Start the onboarding process
            ITEM_TAPPED,      // A list item has been tapped
            SWIPED,           // The ViewPager has been swiped
            LIST_EMPTY,       // The last item in a list got deleted, or an empty list has been selected
            LIST_NOT_EMPTY,   // The first item got added to a list, or a non-empty list has been selected
        }

        public enum State {
            INIT,       // Onboarding has not started yet
            HIDE,       // Hide the onboarding message
            TAP_ITEM,   // Let the user know items can be tapped
            SWIPE,      // Let the user know the page can be swiped left or right
            COMPLETED   // Onboarding complete
        }

        public interface CompletedListener {
            void onCompleted();
        }

        private final MutableLiveData<State> mStage;
        private final CompletedListener mCompletedListener;
        private boolean mUserHasTapped;
        private boolean mUserHasSwiped;

        private Onboarding(boolean isCompleted, @NonNull CompletedListener listener) {
            mCompletedListener = listener;
            if (isCompleted) {
                mStage = new MutableLiveData<>(State.COMPLETED);
            } else {
                mStage = new MutableLiveData<>(State.INIT);
                mUserHasTapped = false;
                mUserHasSwiped = false;
            }
        }

        public LiveData<State> getState() {
            return Transformations.distinctUntilChanged(mStage);
        }

        public void notify(Event event) {
            if (mStage.getValue() == State.INIT) {
                if (event == Event.START_ONBOARDING) {
                    mStage.setValue(State.TAP_ITEM);
                }
            } else if (mStage.getValue() == State.COMPLETED) {
                // Do nothing
            } else {
                switch (event) {
                    case LIST_EMPTY:
                        mStage.setValue(State.HIDE);
                        return;
                    case ITEM_TAPPED:
                        mUserHasTapped = true;
                        break;
                    case SWIPED:
                        mUserHasSwiped = true;
                        break;
                    case LIST_NOT_EMPTY:
                        break;
                }
                // Update Stage depending on what the user has done already:
                if (mUserHasTapped && mUserHasSwiped) {
                    mStage.setValue(State.COMPLETED);
                    mCompletedListener.onCompleted();
                } else if (mUserHasTapped) {
                    mStage.setValue(State.SWIPE);
                } else {
                    mStage.setValue(State.TAP_ITEM);
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
    private static final ExecutorService mExecutor =
            Executors.newSingleThreadExecutor();
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
                    mChecklistRepo.getAllItemsLiveData(listTitle),
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
        List<String> currentTitles = mChecklistTitles.getValue();
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

    public void moveChecklistToTrash(String checklistTitle) {
        assert mChecklistTitles.getValue() != null;
        if (!mChecklistTitles.getValue().contains(checklistTitle)) {
            // TODO: replace with appropriate exception
            throw new IllegalArgumentException("List with title \"" + checklistTitle +  "\" does not exists");
        }
        mExecutor.execute(() -> {
            String trashed_title = TRASH_LABEL + checklistTitle + "(" + Calendar.getInstance().getTime() + ")";
            mChecklistRepo.updateChecklistName(checklistTitle, trashed_title);
            assert mChecklistTitles.getValue() != null;
            String ac =                     mChecklistTitles.getValue().stream()
                    .filter(listTitle ->
                            !listTitle.contains(TRASH_LABEL) &&
                            // Required because old list title is still present at this point (why?)
                            !listTitle.equals(checklistTitle))
                    .findFirst()
                    .orElse(null);
            mChecklistRepo.setActiveChecklist(ac);
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

                    // A new item will have the lowest incidence.
                    long incidence = mChecklistRepo.getMinIncidence(listTitle) - 1;
                    // TODO: maybe don't use integral type for 'position', so that undefined position is allowed
                    DbChecklistItem newDbItem = new DbChecklistItem(
                            strippedName, isChecked, 0, listTitle, incidence);

                    List<DbChecklistItem> dbItems = mChecklistRepo.getItemsSortedByPosition(listTitle, isChecked);
                    dbItems.add(newDbItem);

                    // TODO: is this really necessary?
                    if (isChecked) {
                        sortByIncidenceDescending(dbItems);
                    }
                    updatePositionByOrder(dbItems);

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
