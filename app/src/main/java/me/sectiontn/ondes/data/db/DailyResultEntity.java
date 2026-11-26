package me.sectiontn.ondes.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** A finished daily challenge. One row per day, so a day cannot be played twice. */
@Entity(tableName = "daily_results")
public class DailyResultEntity {

    @PrimaryKey
    public int dayNumber;
    /** The local date, for example "2026-11-21". */
    public String date;
    public int score;
    /** One letter per round: G perfect, Y with a hint, R wrong. For example "GGYRY". */
    public String squares;
    /** The five rounds as JSON, so Home can show the Summary again. */
    public String roundsJson;
}
