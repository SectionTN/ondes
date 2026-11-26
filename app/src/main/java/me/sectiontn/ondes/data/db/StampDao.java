package me.sectiontn.ondes.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StampDao {

    /** Newest first, as the passport grid shows them. */
    @Query("SELECT * FROM stamps ORDER BY firstDate DESC, countryCode")
    LiveData<List<StampEntity>> all();

    /** IGNORE keeps the first date when the country is guessed right again. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void add(StampEntity stamp);
}
