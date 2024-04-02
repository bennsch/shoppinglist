package com.example.shoppinglist.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class DbChecklistItem {
    @PrimaryKey(autoGenerate = true) // autoGenerate: null is treated as "non-set"
    private Long itemId;

    private String belongsToChecklistTitle;

    @NonNull
    private String name;

    private boolean isChecked;

    private long positionInSublist;

    public DbChecklistItem(@NonNull String name, boolean isChecked, long positionInSublist, String belongsToChecklistTitle) {
        // itemId not available as parameter because Database should be the only one which set this id.
        // (null means generate new id)
        this.name = name;
        this.isChecked = isChecked;
        this.positionInSublist = positionInSublist;
        this.belongsToChecklistTitle = belongsToChecklistTitle;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getBelongsToChecklistTitle() {
        return belongsToChecklistTitle;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public long getPositionInSublist() {
        return positionInSublist;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public void setBelongsToChecklistTitle(String belongsToChecklistTitle) {
        this.belongsToChecklistTitle = belongsToChecklistTitle;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public void setPositionInSublist(long positionInSublist) {
        this.positionInSublist = positionInSublist;
    }
}