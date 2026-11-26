package me.sectiontn.ondes.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DailyResultDao {

    @Query("SELECT * FROM daily_results WHERE dayNumber = :dayNumber")
    LiveData<DailyResultEntity> forDay(int dayNumber);

    @Query("SELECT dayNumber FROM daily_results")
    LiveData<List<Integer>> playedDays();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void add(DailyResultEntity result);
}
