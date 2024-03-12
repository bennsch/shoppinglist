package com.example.shoppinglist;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(entities = {ChecklistDatabase.Item.class}, version = 1 /*exportSchema = false*/)
public abstract class ChecklistDatabase extends RoomDatabase {

    private static final String TAG = "ListItemDatabase";

    private static volatile ChecklistDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

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

            // If you want to keep data through app restarts,
            // comment out the following block
            databaseWriteExecutor.execute(() -> {
                // Populate the database in the background.
                // If you want to start with more words, just add them.
                ItemDao dao = INSTANCE.itemDao();
                dao.deleteAll();
                List<Item> items = new ArrayList<>();
                for (int i = 0; i < 3; ++i) {
                    Item item = new ChecklistDatabase.Item(null, "List A", "Item " + i, false, i);
                    items.add(item);
                }
                for (int i = 0; i < 3; ++i) {
                    Item item = new ChecklistDatabase.Item(null, "List A", "Item " + i, true, i);
                    items.add(item);
                }
//                for (int i = 0; i < 2; ++i) {
//                    ListItem item = new ListItem("List B", "ListB_open_" + i, ListItem.Designation.OPEN);
//                    item.setPosition(i);
//                    items.add(item);
//                }
//                for (int i = 0; i < 10; ++i) {
//                    ListItem item = new ListItem("List B", "ListB_closed_" + i, ListItem.Designation.CLOSED);
//                    item.setPosition(i);
//                    items.add(item);
//                }
                dao.insert(items);
            });
        }
    };



    @Entity(tableName = "items")
    static class Item{

        @PrimaryKey(autoGenerate = true) // autoGenerate: null is treated as "non-set"
        @ColumnInfo(name = "uid")
        protected Integer mUid;

        @NonNull
        @ColumnInfo(name = "list_title")
        protected String mListTitle;

        @NonNull
        @ColumnInfo(name = "name")
        protected String mName;

        @NonNull
        @ColumnInfo(name = "is_checked")
        protected Boolean mIsChecked;

        @ColumnInfo(name = "position")
        protected Integer mPosition;

        public Item(Integer uid, @NonNull String listTitle, @NonNull String name, @NonNull Boolean isChecked, Integer position) {
            // Don't provide any access to the UID, so the Database is the only one who can modify it
            mUid = uid;
            mListTitle = listTitle;
            mName = name;
            mIsChecked = isChecked;
            mPosition = position;
        }

        @NonNull
        public String getListTitle() {
            return mListTitle;
        }

        @NonNull
        public String getName() {
            return mName;
        }

        @NonNull
        public Boolean isChecked() {
            return mIsChecked;
        }

        public Integer getPosition() {
            return mPosition;
        }

//        public void setPosition(int position) {
//            this.mPosition = position;
//        } // TODO: 3/3/2024 make private
//
        public Integer getUID() {
            return mUid;
        }
    }


    @Dao
    interface ItemDao {
        @Query("SELECT * FROM items")
        LiveData<List<Item>> getAllItems();

        @Query("SELECT * FROM items WHERE uid == :uid LIMIT 1")
        Item getItem(Integer uid);

//        @Query("SELECT COUNT() FROM items WHERE list_title LIKE :listTitle AND is_checked == :isChecked")


        @Query("SELECT * FROM items WHERE list_title LIKE :listTitle AND is_checked LIKE :isChecked")
        LiveData<List<Item>> getSubsetAsLiveData(String listTitle, boolean isChecked);


        @Query("SELECT MAX(position) FROM items WHERE list_title LIKE :listTitle AND is_checked LIKE :isChecked")
        int getSubSetMaxPosition(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM items WHERE list_title LIKE :listTitle AND is_checked == :isChecked ORDER BY position ASC")
        List<Item> getSubsetSortedByPosition(String listTitle, Boolean isChecked);

        @Query("SELECT * FROM items WHERE list_title LIKE :listTitle")
        List<Item> getList(String listTitle);

        @Query("SELECT * FROM items WHERE list_title LIKE :listTitle AND is_checked == :isChecked ORDER BY position ASC")
        LiveData<List<Item>> getSubsetSortedByPositionAsLiveData(String listTitle, Boolean isChecked);

//        @Transaction
//        default void trans(Item item){
//            delete(item);
//            insert(item);
//        }

        @Query("DELETE FROM items WHERE uid == :uid")
        void delete(Integer uid);

        @Query("DELETE FROM items")
        void deleteAll();

        @Delete
        void delete(Item item);
        
        @Update
        void update(Item item);

        @Update
        void update(Item... items);

        @Update
        void update(List<Item> items);

        @Insert(onConflict = OnConflictStrategy.ABORT)
        void insert(Item item);

        @Insert(onConflict = OnConflictStrategy.ABORT)
        void insert(List<Item> items);

        @Insert(onConflict = OnConflictStrategy.ABORT)
        void insert(Item... items);


    }

}

