package com.example.shoppinglist;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
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
            ChecklistDatabase.Checklist.class,
            RepoItem.class}
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
                    Checklist checklist = new Checklist("NuovoList A");
                    dao.insert(checklist);
                    List<RepoItem> items = new ArrayList<>();
                    for (long i = 0; i < 3; ++i) {
                        RepoItem item = new RepoItem("RepoItem (Unchecked)" + i, false, i, checklist.checklistTitle);
                        items.add(item);
                    }
                    for (long i = 0; i < 2; ++i) {
                        RepoItem item = new RepoItem("RepoItem (Checked)" + i, true, i, checklist.checklistTitle);
                        items.add(item);
                    }
                    dao.insert(items);
                }
                {
                    Checklist checklist = new Checklist("NuovoList B");
                    dao.insert(checklist);
                    List<RepoItem> items = new ArrayList<>();
                    for (long i = 0; i < 3; ++i) {
                        RepoItem item = new RepoItem("RepoItem (Unchecked)" + i, false, i, checklist.checklistTitle);
                        items.add(item);
                    }
                    for (long i = 0; i < 2; ++i) {
                        RepoItem item = new RepoItem("RepoItem (Checked)" + i, true, i, checklist.checklistTitle);
                        items.add(item);
                    }
                    dao.insert(items);
                }
            });
        }
    };


    @Entity(tableName = "checklists")
    static class Checklist {
        @PrimaryKey @NonNull private String checklistTitle;

        public Checklist(@NonNull String checklistTitle) {
            this.checklistTitle = checklistTitle;
        }

        @NonNull
        public String getChecklistTitle() {
            return checklistTitle;
        }
    }


    @Dao
    interface ItemDao {

        @Transaction
        default void insertAndUpdate(RepoItem itemToInsert, List<RepoItem> itemsToUpdate) {
            insert(itemToInsert);
            update(itemsToUpdate);
        }

        @Query("SELECT * FROM items WHERE belongsToChecklistTitle LIKE :checklistTitle")
        List<RepoItem> getAllItemsFromChecklist(String checklistTitle);

        @Query("SELECT * FROM checklists")
        LiveData<List<Checklist>> getAllChecklists();

        @Query("SELECT * FROM items WHERE belongsToChecklistTitle LIKE :listTitle AND name LIKE :name")
        RepoItem getItem(String listTitle, String name);

        @Query("SELECT * FROM items WHERE belongsToChecklistTitle LIKE :listTitle AND isChecked == :isChecked ORDER BY positionInSublist ASC")
        LiveData<List<RepoItem>> getSubsetSortedByPositionAsLiveData(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM items WHERE belongsToChecklistTitle LIKE :listTitle AND isChecked == :isChecked ORDER BY positionInSublist ASC")
        List<RepoItem> getSubsetSortedByPosition(String listTitle, Boolean isChecked);

        @Insert
        void insert(Checklist checklist);

        @Insert
        void insert(RepoItem item);

        @Insert
        void insert(List<RepoItem> items);

        @Update
        void update(List<RepoItem> items);
    }

}

