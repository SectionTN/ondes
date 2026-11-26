package me.sectiontn.ondes.data.repo;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.sectiontn.ondes.data.db.AppDatabase;
import me.sectiontn.ondes.data.db.DailyResultEntity;
import me.sectiontn.ondes.data.db.StampEntity;
import me.sectiontn.ondes.game.DailySeed;
import me.sectiontn.ondes.game.RoundRecord;
import me.sectiontn.ondes.game.Streak;

/** The player's progress: passport stamps, finished dailies and the streak. */
public final class ProgressRepository {

    private static ProgressRepository instance;
    private static final String KEY_FIRST_DAY = "first_day";

    private final AppDatabase database;
    private final SharedPreferences prefs;
    // Room refuses queries on the main thread, so every write goes through this thread
    private final ExecutorService disk = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private ProgressRepository(Context context) {
        database = AppDatabase.get(context);
        prefs = context.getApplicationContext().getSharedPreferences("progress", Context.MODE_PRIVATE);
    }

    public static synchronized ProgressRepository get(Context context) {
        if (instance == null) {
            instance = new ProgressRepository(context);
        }
        return instance;
    }

    public LiveData<List<StampEntity>> stamps() {
        return database.stamps().all();
    }

    /** Stamps the country, keeping the first date if it already has a stamp. */
    public void addStamp(String countryCode) {
        StampEntity stamp = new StampEntity();
        stamp.countryCode = countryCode;
        stamp.firstDate = System.currentTimeMillis();
        disk.execute(() -> database.stamps().add(stamp));
    }

    /** The player's own daily number for a day: the day this phone first opened the app is #1. */
    public synchronized int playerNumber(int dayNumber) {
        if (!prefs.contains(KEY_FIRST_DAY)) {
            prefs.edit().putInt(KEY_FIRST_DAY, DailySeed.today()).apply();
        }
        return DailySeed.playerNumber(dayNumber, prefs.getInt(KEY_FIRST_DAY, dayNumber));
    }

    public LiveData<DailyResultEntity> dailyResult(int dayNumber) {
        return database.dailyResults().forDay(dayNumber);
    }

    /** Days in a row with a finished daily, recounted whenever a daily is saved. */
    public LiveData<Integer> streak() {
        return Transformations.map(database.dailyResults().playedDays(),
                days -> Streak.length(days, DailySeed.today()));
    }

    public void saveDaily(int dayNumber, int score, List<RoundRecord> rounds) {
        StringBuilder squares = new StringBuilder();
        for (RoundRecord round : rounds) {
            squares.append(round.squareLetter());
        }
        DailyResultEntity result = new DailyResultEntity();
        result.dayNumber = dayNumber;
        result.date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        result.score = score;
        result.squares = squares.toString();
        result.roundsJson = gson.toJson(rounds);
        disk.execute(() -> database.dailyResults().add(result));
    }

    public List<RoundRecord> rounds(DailyResultEntity result) {
        List<RoundRecord> rounds = gson.fromJson(result.roundsJson, new TypeToken<List<RoundRecord>>() { }.getType());
        return rounds != null ? rounds : new ArrayList<>();
    }
}
