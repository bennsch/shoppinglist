package com.bennsch.shoppinglist.data;

import android.content.Context;
import android.util.Log;

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

import java.util.ArrayList;
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

        @Transaction
        default void insertAndUpdate(DbChecklistItem itemToInsert, List<DbChecklistItem> itemsToUpdate) {
            insert(itemToInsert);
            update(itemsToUpdate);
        }

        @Transaction
        default void selectChecklist(String checklistName) {
            clearSelected();
            setSelected(checklistName);
        }

        @Query("SELECT * FROM DbChecklist")
        LiveData<List<DbChecklist>> getAllChecklists();

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklistTitle LIKE :listTitle AND name LIKE :name")
        DbChecklistItem getItem(String listTitle, String name);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklistTitle LIKE :listTitle AND isChecked == :isChecked ORDER BY positionInSublist ASC")
        LiveData<List<DbChecklistItem>> getSubsetSortedByPositionAsLiveData(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklistTitle LIKE :listTitle AND isChecked == :isChecked ORDER BY positionInSublist ASC")
        List<DbChecklistItem> getSubsetSortedByPosition(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM DbChecklistItem WHERE belongsToChecklistTitle LIKE :listTitle")
        List<DbChecklistItem> getItemsFromList(@NonNull final String listTitle);

        @Query("SELECT checklistTitle FROM DbChecklist WHERE selected == 1")
        LiveData<String> getSelectedChecklist();

        @Insert
        void insert(DbChecklist checklist);

        @Insert
        void insert(DbChecklistItem item);

        @Insert
        void insert(List<DbChecklistItem> items);

        @Update
        void update(List<DbChecklistItem> items);

        @Query("DELETE FROM dbchecklist WHERE checklistTitle LIKE :checklistTitle")
        void delete(String checklistTitle);

        @Query("UPDATE dbchecklist SET checklistTitle = :newChecklistName WHERE checklistTitle LIKE :checklistName")
        void update(String checklistName, String newChecklistName);

        @Query("UPDATE DbChecklist SET selected = 1 WHERE checklistTitle LIKE :checklistName")
        void setSelected(String checklistName);

        @Query("UPDATE DbChecklist SET selected = 0")
        void clearSelected();
    }

    private static volatile ChecklistDatabase INSTANCE;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(1);

    public abstract ItemDao itemDao();

    static ChecklistDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ChecklistDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ChecklistDatabase.class, "checklist_database")
                            .addCallback(populateInitialDatabase)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback populateInitialDatabase = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                INSTANCE.clearAllTables();
                ItemDao dao = INSTANCE.itemDao();

                DbChecklist shortList = new DbChecklist("Short List", false, false);
                dao.insert(shortList);
                dao.insert(new DbChecklistItem("Wood", false, 0, shortList.getChecklistTitle()));
                dao.insert(new DbChecklistItem("Timber", false, 1, shortList.getChecklistTitle()));
                dao.insert(new DbChecklistItem("Tree", false, 2, shortList.getChecklistTitle()));

                DbChecklist longList = new DbChecklist("Long", true, true);
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
}

