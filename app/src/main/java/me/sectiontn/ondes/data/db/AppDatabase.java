package me.sectiontn.ondes.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** The app's SQLite database. Room writes the code behind the DAOs. */
@Database(entities = {StationEntity.class, StampEntity.class, DailyResultEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract StationDao stations();

    public abstract StampDao stamps();

    public abstract DailyResultDao dailyResults();

    public static synchronized AppDatabase get(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "ondes.db").build();
        }
        return instance;
    }
}
