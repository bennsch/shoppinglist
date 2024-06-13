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


@Database(entities = {
            DbChecklist.class,
            DbChecklistItem.class}
        , version = 1 /*exportSchema = false*/)
public abstract class ChecklistDatabase extends RoomDatabase {

    private static final String TAG = "ListItemDatabase";

    private static volatile ChecklistDatabase INSTANCE;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(1);

    public abstract ItemDao itemDao();

    static ChecklistDatabase getDatabase(final Context context) {
        Log.d(TAG, "getDatabase: ");
        if (INSTANCE == null) {
            synchronized (ChecklistDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ChecklistDatabase.class, "checklist_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            Log.d(TAG, "onCreate: Callback");

            super.onCreate(db);

            databaseWriteExecutor.execute(() -> {
                ItemDao dao = INSTANCE.itemDao();
                INSTANCE.clearAllTables();
                {
                    DbChecklist checklist = new DbChecklist("NuovoList A", false);
                    dao.insert(checklist);
                    List<DbChecklistItem> items = new ArrayList<>();
                    for (long i = 0; i < 3; ++i) {
                        DbChecklistItem item = new DbChecklistItem("RepoItem (Unchecked)" + i, false, i, checklist.getChecklistTitle());
                        items.add(item);
                    }
                    for (long i = 0; i < 2; ++i) {
                        DbChecklistItem item = new DbChecklistItem("RepoItem (Checked)" + i, true, i, checklist.getChecklistTitle());
                        items.add(item);
                    }
                    dao.insert(items);
                }
                {
                    DbChecklist checklist = new DbChecklist("NuovoList B", true);
                    dao.insert(checklist);
                    List<DbChecklistItem> items = new ArrayList<>();
                    for (long i = 0; i < 3; ++i) {
                        DbChecklistItem item = new DbChecklistItem("RepoItem (Unchecked)" + i, false, i, checklist.getChecklistTitle());
                        items.add(item);
                    }
                    for (long i = 0; i < 2; ++i) {
                        DbChecklistItem item = new DbChecklistItem("RepoItem (Checked)" + i, true, i, checklist.getChecklistTitle());
                        items.add(item);
                    }
                    dao.insert(items);
                }
            });
        }
    };



    @Dao
    interface ItemDao {

        @Transaction
        default void insertAndUpdate(DbChecklistItem itemToInsert, List<DbChecklistItem> itemsToUpdate) {
            insert(itemToInsert);
            update(itemsToUpdate);
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
    }

}

