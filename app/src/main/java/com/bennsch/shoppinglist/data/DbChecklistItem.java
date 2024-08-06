package com.bennsch.shoppinglist.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;


// Using Foreign Key to enforce a relationship between DbChecklist and
// the DbChecklistItems which belong to it.
// ForeignKey.CASCADE will make sure that if a DbChecklist is deleted/updated,
// so are its DbChecklistItems
@Entity(foreignKeys = {
            @ForeignKey(
                entity = DbChecklist.class,
                parentColumns = "checklistTitle",
                childColumns = "belongsToChecklist",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)
})
public class DbChecklistItem {

    // autoGenerate: null is treated as "non-set".
    @PrimaryKey(autoGenerate = true)
    private Long itemId;
    // Items can have duplicate names, since itemId is unique.
    @NonNull private String name;
    // Link this item to a checklist.
    @NonNull private final String belongsToChecklist;
    // Item is checked or not.
    private boolean isChecked;
    // Position in relation to other items with same "isChecked".
    private long position;
    // How often the user used this item. Incremented whenever the user
    // ?ed this item.
    private long incidence;

    // private date_created (e.g. UUID)

    public DbChecklistItem(@NonNull String name,
                           boolean isChecked,
                           long position,
                           @NonNull String belongsToChecklist,
                           long incidence) {
        // Only the database should generate a unique "itemId".
        // "null" means "generate new ID".
        this.itemId = null;
        this.name = name;
        this.isChecked = isChecked;
        this.position = position;
        this.incidence = incidence;
        this.belongsToChecklist = belongsToChecklist;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        // Needs to be publicly available so that auto generated Room
        // code can access it
        this.itemId = itemId;
    }

    @NonNull
    public String getBelongsToChecklist() {
        return belongsToChecklist;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getIncidence() {
        return incidence;
    }

    public void setIncidence(long incidence) {
        this.incidence = incidence;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public long getPosition() {
        return position;
    }
}