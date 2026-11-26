package me.sectiontn.ondes.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StationDao {

    int RECENT_LIMIT = 20;

    @Query("SELECT * FROM stations WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT " + RECENT_LIMIT)
    LiveData<List<StationEntity>> recent();

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE")
    LiveData<List<StationEntity>> favorites();

    @Query("SELECT uuid FROM stations WHERE isFavorite = 1")
    LiveData<List<String>> favoriteIds();

    @Query("SELECT * FROM stations WHERE uuid = :uuid")
    StationEntity find(String uuid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(StationEntity station);

    @Delete
    void delete(StationEntity station);

    /** Keeps the 20 most recent stations plus the favorites, and drops the rest. */
    @Query("DELETE FROM stations WHERE isFavorite = 0 AND uuid NOT IN "
            + "(SELECT uuid FROM stations WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT " + RECENT_LIMIT + ")")
    void pruneRecent();
}
