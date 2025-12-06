package com.bennsch.shoppinglist.datamodel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(
        entities = {DbChecklist.class, DbChecklistItem.class},
        version = 1,
        autoMigrations = {/* @AutoMigration(from = 1, to = 2) */}
        /*exportSchema = false*/ )
public abstract class ChecklistDatabase extends RoomDatabase {
    /*
     *  Persistent data storage based on SQLite.
     *  This database should only be accessed via the "ChecklistRepository".
     */

    @Dao
    interface ItemDao {
        /*
         *  Doc: DAO (Data Access Object) validates SQL at compile-time and associates it with a
         *  method. Annotations (@Insert, @Update etc..) will generate common SQL queries. For
         *  methods which return LiveData, Room will generate all the necessary code to update
         *  LiveData if the database is updated.
         */

        @Insert
        void insert(DbChecklist checklist);

        @Insert
        void insert(DbChecklistItem item);

        @Delete
        void delete(DbChecklistItem item);

        @Update
        void update(List<DbChecklistItem> items);

        @Query("UPDATE Dbchecklist SET listTitle = :newChecklistName WHERE listTitle LIKE :listTitle")
        void update(String listTitle, String newChecklistName);

        @Query("DELETE FROM dbchecklist WHERE listTitle LIKE :checklistTitle")
        void delete(String checklistTitle);

        @Query("UPDATE DbChecklist SET active = 1 WHERE listTitle LIKE :listTitle")
        void setChecklistActive(String listTitle);

        @Query("UPDATE DbChecklist SET active = 0")
        void setAllChecklistsInactive();

        @Query("SELECT * FROM DbChecklistItem")
        List<DbChecklistItem> getAllItems();

        @Query("SELECT * FROM DbChecklist")
        LiveData<List<DbChecklist>> getAllChecklists();

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle AND " +
                "isChecked == :isChecked ORDER BY position ASC")
        List<DbChecklistItem> getItemSubsetSorted(@NonNull String listTitle,
                                                  @NonNull Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle AND " +
                "isChecked == :isChecked ORDER BY position ASC")
        LiveData<List<DbChecklistItem>> getItemSubsetSortedLiveData(@NonNull String listTitle,
                                                                    @NonNull Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle")
        List<DbChecklistItem> getItems(@NonNull String listTitle);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle")
        LiveData<List<DbChecklistItem>> getItemsLiveData(@NonNull String listTitle);

        @Query("SELECT * FROM DbChecklist WHERE active == 1")
        LiveData<DbChecklist> getActiveChecklist();

        @Query("SELECT MIN(incidence) FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle")
        long getMinIncidence(@NonNull String listTitle); // Returns 0 if list is empty.

        // Use "Transaction" so that multiple Queries will result in only one LiveData event:
        @Transaction
        default void insertAndUpdate(@NonNull DbChecklistItem itemToInsert,
                                     @NonNull List<DbChecklistItem> itemsToUpdate) {
            insert(itemToInsert);
            update(itemsToUpdate);
        }

        @Transaction
        default void setActiveChecklist(@Nullable String listTitle) {
            // Set the checklist "listTitle" active, set all other checklists "inactive". If title
            // is null, set all checklists inactive.
            setAllChecklistsInactive();
            setChecklistActive(listTitle);
        }
    }


    private static volatile ChecklistDatabase INSTANCE;
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
    private static final String DATABASE_NAME = "checklist_database";

    abstract ItemDao itemDao();

    private static final Runnable populateDatabaseRunnable = () -> {
        // Create an initial list for demonstration purposes.
        INSTANCE.clearAllTables();
        ItemDao dao = INSTANCE.itemDao();
        DbChecklist list = new DbChecklist("Groceries", true);
        dao.insert(list);
        dao.insert(new DbChecklistItem("Bacon", false, 0, list.getListTitle(), 0));
        dao.insert(new DbChecklistItem("Eggs", false, 1, list.getListTitle(), 0));
        dao.insert(new DbChecklistItem("Orange Juice", false, 2, list.getListTitle(), 0));
        dao.insert(new DbChecklistItem("Butter", true, 0, list.getListTitle(), 0));
        dao.insert(new DbChecklistItem("Avocados", true, 1, list.getListTitle(), 0));
    };

    private static final RoomDatabase.Callback onCreateCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            // Called only if no data is present yet (after app got installed or user deleted app
            // storage)
            super.onCreate(db);
            executor.execute(populateDatabaseRunnable);
        }
    };

    private static final RoomDatabase.Callback onOpenCallback = new Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            // Called everytime the database is opened.
            super.onOpen(db);
            if (PreferencesRepository.DBG_PRETEND_FIRST_STARTUP) {
                executor.execute(populateDatabaseRunnable);
            }
        }
    };

    static ChecklistDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ChecklistDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context, ChecklistDatabase.class, DATABASE_NAME)
                            .addCallback(onCreateCallback)
                            .addCallback(onOpenCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

