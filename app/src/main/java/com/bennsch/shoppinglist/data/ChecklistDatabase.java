package com.bennsch.shoppinglist.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Database;
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


@Database(entities = {DbChecklist.class,
                      DbChecklistItem.class},
          version = 1
         /*exportSchema = false*/)
public abstract class ChecklistDatabase extends RoomDatabase {

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

        @Query("SELECT * FROM DbChecklist")
        LiveData<List<DbChecklist>> getAllChecklists();

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle AND isChecked == :isChecked ORDER BY positionInSublist ASC")
        LiveData<List<DbChecklistItem>> getSubsetSortedByPositionAsLiveData(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle AND isChecked == :isChecked ORDER BY positionInSublist ASC")
        List<DbChecklistItem> getSubsetSortedByPosition(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklist LIKE :listTitle")
        List<DbChecklistItem> getItemsFromList(@NonNull final String listTitle);

        @Query("SELECT * FROM DbChecklist WHERE active == 1")
        LiveData<DbChecklist> getActiveChecklist();

        @Transaction
        default void insertAndUpdate(DbChecklistItem itemToInsert, List<DbChecklistItem> itemsToUpdate) {
            // Use Transaction so that multiple Queries will result in only one LiveData event.
            insert(itemToInsert);
            update(itemsToUpdate);
        }

        @Transaction
        default void setActiveChecklist(String checklistName) {
            // Use Transaction so that multiple Queries will result in only one LiveData event.
            // Only activate a single checklist and make every other inactive.
            setAllChecklistsInactive();
            setChecklistActive(checklistName);
        }
    }


    private static volatile ChecklistDatabase INSTANCE;
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    abstract ItemDao itemDao();

    private static final RoomDatabase.Callback populateInitialDatabase = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            executor.execute(() -> {
                INSTANCE.clearAllTables();
                ItemDao dao = INSTANCE.itemDao();

                DbChecklist shortList = new DbChecklist("Short List", false);
                dao.insert(shortList);
                dao.insert(new DbChecklistItem("Wood", false, 0, shortList.getChecklistTitle()));
                dao.insert(new DbChecklistItem("Timber", false, 1, shortList.getChecklistTitle()));
                dao.insert(new DbChecklistItem("Tree", false, 2, shortList.getChecklistTitle()));

                DbChecklist longList = new DbChecklist("Long", true);
                dao.insert(longList);
                int sizeUnchecked = 20;
                for (int i = 0; i < sizeUnchecked; i++) {
                    dao.insert(new DbChecklistItem("Item " + i, false, i, longList.getChecklistTitle()));
                }
                for (int i = 0; i < 10; i++) {
                    dao.insert(new DbChecklistItem("Item " + (i + sizeUnchecked), true, i, longList.getChecklistTitle()));
                }
            });
        }
    };

    static ChecklistDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (ChecklistDatabase.class) {
                if (INSTANCE == null) {
                    // Create and populate the database if no data is present
                    // (e.g. app just got installed or user deleted app storage).
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ChecklistDatabase.class, "checklist_database")
                            .addCallback(populateInitialDatabase)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

