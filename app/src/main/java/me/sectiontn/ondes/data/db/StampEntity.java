package me.sectiontn.ondes.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** A passport stamp: the first time the user guessed this country right. */
@Entity(tableName = "stamps")
public class StampEntity {

    @PrimaryKey
    @NonNull
    public String countryCode = "";
    /** Milliseconds since 1970, when the stamp was earned. */
    public long firstDate;
}
