package com.bennsch.shoppinglist.data;

import android.content.Context;
import android.util.Log;

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

import com.bennsch.shoppinglist.BuildConfig;
import com.bennsch.shoppinglist.GlobalConfig;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(
        entities = {DbChecklist.class, DbChecklistItem.class},
        version = 1,
        autoMigrations = {
//                @AutoMigration(from = 1, to = 2)
        }
         /*exportSchema = false*/)
public abstract class ChecklistDatabase extends RoomDatabase {

    private static final String TAG = "ChecklistDatabase";

    @Dao
    interface ItemDao {

        // DAO (Data Access Object) validates SQL at compile-time and associates
        // it with a method. Annotations (@Insert, @Update etc..) will generate
        // common SQL queries.
        // For methods which return LiveData, Room will generate all the necessary
        // code to update LiveData if the database is updated.

        @Insert
        void insert(DbChecklist checklist);

        @Insert
        void insert(DbChecklistItem item);

        @Delete
        void delete(DbChecklistItem item);

        @Update
        void update(List<DbChecklistItem> items);

        @Query("UPDATE Dbchecklist SET checklistTitle = :newChecklistName WHERE checklistTitle LIKE :checklistName")
        void update(String checklistName, String newChecklistName);

        @Query("DELETE FROM dbchecklist WHERE checklistTitle LIKE :checklistTitle")
        void delete(String checklistTitle);

        @Query("UPDATE DbChecklist SET active = 1 WHERE checklistTitle LIKE :checklistName")
        void setChecklistActive(String checklistName);

        @Query("UPDATE DbChecklist SET active = 0")
        void setAllChecklistsInactive();

        @Query("SELECT * FROM DbChecklistItem")
        List<DbChecklistItem> getAllItemsFromAllLists();

        @Query("SELECT * FROM DbChecklist")
        LiveData<List<DbChecklist>> getAllChecklists();

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle AND isChecked == :isChecked")
        List<DbChecklistItem> getItems(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle AND isChecked == :isChecked ORDER BY position ASC")
        List<DbChecklistItem> getItemsSortedByPosition(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle AND isChecked == :isChecked ORDER BY position ASC")
        LiveData<List<DbChecklistItem>> getItemsSortedByPositionLiveData(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle")
        List<DbChecklistItem> getItemsFromChecklist(@NonNull final String listTitle);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle")
        LiveData<List<DbChecklistItem>> getItemsFromChecklistLiveData(@NonNull final String listTitle);

        @Query("SELECT * FROM DbChecklist WHERE active == 1")
        LiveData<DbChecklist> getActiveChecklist();

        @Query("SELECT COUNT(*) FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle")
        LiveData<Integer> getItemsFromChecklistCount(@NonNull final String listTitle);

        // Returns 0 if list is empty
        @Query("SELECT MIN(incidence) FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle")
        long getMinIncidence(@NonNull final String listTitle);

        @Transaction
        default void insertAndUpdate(DbChecklistItem itemToInsert, List<DbChecklistItem> itemsToUpdate) {
            // Use Transaction so that multiple Queries will result in only one LiveData event.
            insert(itemToInsert);
            update(itemsToUpdate);
        }

        @Transaction
        default void setActiveChecklist(@Nullable String checklistName) {
            // Use Transaction so that multiple Queries will result in only one LiveData event.
            // Only activate a single checklist and make every other inactive.
            setAllChecklistsInactive();
            setChecklistActive(checklistName);
        }
    }


    private static volatile ChecklistDatabase INSTANCE;
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    abstract ItemDao itemDao();

    private static final RoomDatabase.Callback onCreateCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            executor.execute(() -> {
                Log.d(TAG, "initialCallback()");
                INSTANCE.clearAllTables();
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Populating database");
                    ItemDao dao = INSTANCE.itemDao();
                    DbChecklist shortList = new DbChecklist("Short List", false);
                    dao.insert(shortList);
                    dao.insert(new DbChecklistItem("Wood", false, 0, shortList.getChecklistTitle(), 0));
                    dao.insert(new DbChecklistItem("Timber", false, 1, shortList.getChecklistTitle(), 0));
                    dao.insert(new DbChecklistItem("Tree", false, 2, shortList.getChecklistTitle(), 0));
                    DbChecklist emptyList = new DbChecklist("Empty List", false);
                    dao.insert(emptyList);
                    DbChecklist longList = new DbChecklist("Long", true);
                    dao.insert(longList);
                    int sizeUnchecked = 20;
                    for (int i = 0; i < sizeUnchecked; i++) {
                        dao.insert(new DbChecklistItem("Item " + i, false, i, longList.getChecklistTitle(), 0));
                    }
                    for (int i = 0; i < 10; i++) {
                        dao.insert(new DbChecklistItem("Item " + (i + sizeUnchecked), true, i, longList.getChecklistTitle(), 0));
                    }
                }
            });
        }
    };

    private static final RoomDatabase.Callback onOpenCallback = new Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            executor.execute(() -> {
                Log.d(TAG, "onOpenCallback()");
                if (GlobalConfig.DBG_FIRST_STARTUP) {
                    INSTANCE.clearAllTables();
                    Log.d(TAG, "onOpen: " + "Database cleared");
                }
            });
        }
    };

    // TODO: Singleton cannot have argument!! (maybe store one instance per context (map)?)
    static ChecklistDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (ChecklistDatabase.class) {
                if (INSTANCE == null) {
                    // Create the database if and call "initialCallback" no data is present yet.
                    // (e.g. app just got installed or user deleted app storage).
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ChecklistDatabase.class, "checklist_database")
                            .addCallback(onCreateCallback)
                            .addCallback(onOpenCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

