package com.bennsch.shoppinglist.datamodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;


// Using Foreign Key to enforce a relationship between DbChecklist and
// the DbChecklistItems which belong to it.
// ForeignKey.CASCADE will make sure that if a DbChecklist is deleted/updated,
// so are its DbChecklistItems
@Entity(indices = {
            @Index("belongsToChecklist")},
        foreignKeys = {
            @ForeignKey(
                entity = DbChecklist.class,
                parentColumns = "listTitle",
                childColumns = "belongsToChecklist",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)}
)
public class DbChecklistItem {
    /*
     *  Represents a Checklist-Item in the database.
     */

    // autoGenerate: null is treated as "non-set".
    @PrimaryKey(autoGenerate = true)
    private Integer itemId;
    // Items can have duplicate names, since itemId is unique.
    @NonNull private String name;
    // Link this item to a checklist.
    @NonNull private final String belongsToChecklist;
    // Item is checked or not.
    private boolean isChecked;
    // Position in relation to other items with the same "isChecked". Two items in a Checklist can
    // have the same "position" as long as "isChecked" differs. We need to keep track of the
    // position separately like this, because checked and unchecked items will be displayed as
    // separate lists and we need the flexibility to control their positions independently.
    private Integer position;

    // TODO: remove default value for actual release?
    // How often the user used this item. Incremented whenever the user
    // ?ed this item.
    @ColumnInfo(defaultValue = "0") // Default value required for automatic database migration.
    private long incidence;

    // private date_created (e.g. UUID)

    public DbChecklistItem(@NonNull String name,
                           boolean isChecked,
                           Integer position,
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

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        // Needs to be publicly available so that auto generated Room code can access it.
        this.itemId = itemId;
    }

    @NonNull
    public String getBelongsToChecklist() {
        return belongsToChecklist;
    }

    public void setPosition(Integer position) {
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

    @Nullable
    public Integer getPosition() {
        return position;
    }
}