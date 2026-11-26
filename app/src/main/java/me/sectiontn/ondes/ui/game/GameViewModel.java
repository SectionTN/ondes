package me.sectiontn.ondes.ui.game;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.data.countries.Country;
import me.sectiontn.ondes.data.countries.CountryCatalog;
import me.sectiontn.ondes.data.db.DailyResultEntity;
import me.sectiontn.ondes.data.repo.ProgressRepository;
import me.sectiontn.ondes.data.repo.StationRepository;
import me.sectiontn.ondes.game.DailySeed;
import me.sectiontn.ondes.game.DistractorPicker;
import me.sectiontn.ondes.game.GameRound;
import me.sectiontn.ondes.game.Outcome;
import me.sectiontn.ondes.game.RoundRecord;
import me.sectiontn.ondes.player.RadioPlayer;

/**
 * The state of one game, kept across screen changes: the rounds, the timer and the player.
 * GameActivity shows the screen it asks for, and each fragment draws from it.
 */
public class GameViewModel extends AndroidViewModel {

    public enum Screen { ROUND, RESULT, SUMMARY }

    /** What the Round screen shows in the ring. */
    public enum Status { LOADING, PLAYING, PAUSED, NETWORK_ERROR, STREAM_ERROR }

    /** A finished game, or a saved daily opened again from Home. */
    public static class Summary {
        public final boolean daily;
        public final int dayNumber;
        public final int score;
        public final List<RoundRecord> rounds;

        Summary(boolean daily, int dayNumber, int score, List<RoundRecord> rounds) {
            this.daily = daily;
            this.dayNumber = dayNumber;
            this.score = score;
            this.rounds = rounds;
        }
    }

    public static final int ROUND_COUNT = DailySeed.ROUND_COUNT;
    private static final int DAILY_STATION_LIMIT = 30;
    private static final int FREE_STATION_LIMIT = 60;
    /** Dead streams in a row before the Round screen asks to retry. */
    private static final int MAX_FAILURES = 3;
    private static final long TICK_MS = 50;

    private final CountryCatalog catalog;
    private final StationRepository stations;
    private final ProgressRepository progress;
    private final RadioPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final MutableLiveData<Screen> screen = new MutableLiveData<>();
    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.LOADING);
    private final MutableLiveData<Long> remainingMs = new MutableLiveData<>(0L);
    private final MutableLiveData<GameRound> round = new MutableLiveData<>();
    private final MediatorLiveData<Summary> summary = new MediatorLiveData<>();

    private boolean started = false;
    private boolean daily = false;
    private int dayNumber;
    private List<String> dailyCountries = new ArrayList<>();
    private final List<GameRound> rounds = new ArrayList<>();
    private int failures = 0;
    private boolean ticking = false;
    private long lastTickAt;
    // Each station request gets a number, so a late answer to an old request is ignored
    private int requestId = 0;

    private final Observer<RadioPlayer.State> playerObserver = this::onPlayerState;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            GameRound current = current();
            if (current == null || !ticking) {
                return;
            }
            addPlayedTime(current);
            remainingMs.setValue(current.remainingMs());
            if (current.remainingMs() == 0) {
                finishRound(null);
            } else {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    public GameViewModel(@NonNull Application application) {
        super(application);
        catalog = CountryCatalog.get(application);
        stations = StationRepository.get(application);
        progress = ProgressRepository.get(application);
        player = RadioPlayer.get(application);
        player.state().observeForever(playerObserver);
    }

    // Starting

    public boolean isStarted() {
        return started;
    }

    /** Starts a new game: today's daily, or free play with random countries. */
    public void start(boolean isDaily) {
        started = true;
        daily = isDaily;
        dayNumber = DailySeed.today();
        rounds.clear();
        if (daily) {
            List<String> pool = new ArrayList<>();
            for (Country country : catalog.dailyPool()) {
                pool.add(country.code);
            }
            dailyCountries = DailySeed.countries(dayNumber, pool);
        }
        addRound();
        screen.setValue(Screen.ROUND);
        loadRound();
    }

    /** Shows the Summary of a daily already played, read back from the database. */
    public void showSavedDaily(int savedDay) {
        started = true;
        daily = true;
        dayNumber = savedDay;
        LiveData<DailyResultEntity> saved = progress.dailyResult(savedDay);
        summary.addSource(saved, result -> {
            if (result != null) {
                summary.setValue(new Summary(true, result.dayNumber, result.score, progress.rounds(result)));
            }
        });
        screen.setValue(Screen.SUMMARY);
    }

    // What the screens read

    public LiveData<Screen> screen() {
        return screen;
    }

    public LiveData<Status> status() {
        return status;
    }

    public LiveData<Long> remainingMs() {
        return remainingMs;
    }

    public LiveData<GameRound> round() {
        return round;
    }

    public LiveData<Summary> summary() {
        return summary;
    }

    public LiveData<RadioPlayer.State> playerState() {
        return player.state();
    }

    public CountryCatalog catalog() {
        return catalog;
    }

    public boolean isDaily() {
        return daily;
    }

    public int dayNumber() {
        return dayNumber;
    }

    /** 1 to 5. */
    public int roundNumber() {
        return rounds.size();
    }

    @Nullable
    public GameRound current() {
        return rounds.isEmpty() ? null : rounds.get(rounds.size() - 1);
    }

    /** Points of the finished rounds. */
    public int score() {
        int total = 0;
        for (GameRound r : rounds) {
            if (r.finished) {
                total += r.points;
            }
        }
        return total;
    }

    /** Outcomes of the finished rounds, for the progress segments. */
    public List<Outcome> outcomes() {
        List<Outcome> outcomes = new ArrayList<>();
        for (GameRound r : rounds) {
            if (r.finished) {
                outcomes.add(r.outcome);
            }
        }
        return outcomes;
    }

    /** True until the Summary shows, so closing asks first. */
    public boolean isInProgress() {
        return started && screen.getValue() != Screen.SUMMARY;
    }

    // Player actions

    public void answer(int optionIndex) {
        GameRound current = current();
        if (current == null || current.finished || !current.started || optionIndex >= current.options.size()) {
            return;
        }
        finishRound(current.options.get(optionIndex));
    }

    public void revealHint(int hint) {
        GameRound current = current();
        if (current == null || current.finished || current.hintsOpen[hint] || current.hintValue(hint) == null) {
            return;
        }
        current.hintsOpen[hint] = true;
        round.setValue(current);
    }

    /** Next round, or the Summary after the fifth. */
    public void next() {
        GameRound current = current();
        if (current == null || !current.finished) {
            return;
        }
        // The last station stops before the next one loads
        player.stop();
        if (rounds.size() < ROUND_COUNT) {
            addRound();
            screen.setValue(Screen.ROUND);
            loadRound();
        } else {
            finishGame();
        }
    }

    /** Retry after an error: fetch the stations again, or try the next one. */
    public void retry() {
        GameRound current = current();
        if (current == null || current.finished) {
            return;
        }
        if (current.candidates.isEmpty()) {
            loadRound();
        } else {
            failures = 0;
            playCurrentStation();
        }
    }

    /** The play and pause button of the Result screen. */
    public void togglePlayback() {
        RadioPlayer.State state = player.state().getValue();
        if (state == RadioPlayer.State.PLAYING || state == RadioPlayer.State.LOADING) {
            player.pause();
        } else {
            GameRound current = current();
            if (current != null && current.station() != null) {
                if (player.isCurrent(current.station())) {
                    player.resume();
                } else {
                    player.play(current.station());
                }
            }
        }
    }

    public void setSaved(Station station, boolean saved) {
        stations.setFavorite(station, saved);
    }

    public LiveData<java.util.Set<String>> favoriteIds() {
        return stations.favoriteIds();
    }

    /** Play again on the free play Summary. */
    public void playAgain() {
        start(false);
    }

    // The round flow

    private void addRound() {
        Country country = daily ? catalog.find(dailyCountries.get(rounds.size())) : randomUnusedCountry();
        rounds.add(new GameRound(country));
        failures = 0;
        remainingMs.setValue(current().remainingMs());
        round.setValue(current());
    }

    private Country randomUnusedCountry() {
        List<Country> pool = catalog.dailyPool();
        while (true) {
            Country pick = pool.get(random.nextInt(pool.size()));
            boolean used = false;
            for (GameRound r : rounds) {
                used |= r.answer.code.equals(pick.code);
            }
            if (!used) {
                return pick;
            }
        }
    }

    /** Fetches the country's stations, orders them and plays the first. */
    private void loadRound() {
        GameRound current = current();
        status.setValue(Status.LOADING);
        int request = ++requestId;
        int limit = daily ? DAILY_STATION_LIMIT : FREE_STATION_LIMIT;
        stations.topStations(current.answer.code, limit, new StationRepository.Callback<List<Station>>() {
            @Override
            public void onResult(List<Station> found) {
                if (request != requestId || current != current()) {
                    return;
                }
                if (found.isEmpty()) {
                    onNoStations(current);
                    return;
                }
                current.candidates = order(found);
                current.candidateIndex = 0;
                // The first station's language picks the options, so a dead stream does not change them
                Station first = current.candidates.get(0);
                String language = first.languages.isEmpty() ? null : first.languages.get(0);
                Random optionsRandom = daily ? DailySeed.optionsRandom(dayNumber, rounds.size() - 1) : random;
                current.options = DistractorPicker.options(current.answer, language, catalog.all(), optionsRandom);
                failures = 0;
                playCurrentStation();
            }

            @Override
            public void onError(IOException error) {
                if (request == requestId && current == current()) {
                    status.setValue(Status.NETWORK_ERROR);
                }
            }
        });
    }

    /** Daily: the order of the day, the same on every phone. Free play: shuffled. */
    private List<Station> order(List<Station> found) {
        if (!daily) {
            List<Station> shuffled = new ArrayList<>(found);
            Collections.shuffle(shuffled, random);
            return shuffled;
        }
        Map<String, Station> byId = new HashMap<>();
        List<String> ids = new ArrayList<>();
        for (Station station : found) {
            if (!byId.containsKey(station.uuid)) {
                byId.put(station.uuid, station);
                ids.add(station.uuid);
            }
        }
        List<Station> ordered = new ArrayList<>();
        for (String id : DailySeed.stationOrder(dayNumber, ids)) {
            ordered.add(byId.get(id));
        }
        return ordered;
    }

    private void onNoStations(GameRound current) {
        if (daily) {
            status.setValue(Status.STREAM_ERROR);
        } else {
            // Free play can pick another country
            rounds.set(rounds.size() - 1, new GameRound(randomUnusedCountry()));
            round.setValue(current());
            loadRound();
        }
    }

    private void playCurrentStation() {
        GameRound current = current();
        status.setValue(Status.LOADING);
        round.setValue(current);
        player.play(current.station());
    }

    private void onPlayerState(RadioPlayer.State state) {
        GameRound current = current();
        if (current == null || current.finished || current.station() == null || !player.isCurrent(current.station())) {
            return;
        }
        switch (state) {
            case PLAYING:
                failures = 0;
                if (!current.started) {
                    current.started = true;
                    round.setValue(current);
                }
                status.setValue(Status.PLAYING);
                startTicking();
                break;
            case LOADING:
                stopTicking();
                status.setValue(Status.LOADING);
                break;
            case PAUSED:
                stopTicking();
                status.setValue(Status.PAUSED);
                break;
            case FAILED:
                stopTicking();
                onStreamFailed(current);
                break;
            default:
                stopTicking();
                break;
        }
    }

    /** A dead stream: silently try the country's next station, and ask for Retry after 3 in a row. */
    private void onStreamFailed(GameRound current) {
        failures++;
        current.candidateIndex++;
        if (failures >= MAX_FAILURES) {
            status.setValue(Status.STREAM_ERROR);
        } else {
            playCurrentStation();
        }
    }

    private void finishRound(@Nullable Country pick) {
        GameRound current = current();
        stopTicking();
        current.finish(pick);
        if (current.isCorrect()) {
            progress.addStamp(current.answer.code);
        }
        round.setValue(current);
        screen.setValue(Screen.RESULT);
    }

    private void finishGame() {
        List<RoundRecord> records = new ArrayList<>();
        for (GameRound r : rounds) {
            records.add(r.toRecord());
        }
        int total = score();
        if (daily) {
            progress.saveDaily(dayNumber, total, records);
        }
        summary.setValue(new Summary(daily, dayNumber, total, records));
        screen.setValue(Screen.SUMMARY);
    }

    // The timer counts only time with sound

    private void startTicking() {
        if (ticking) {
            return;
        }
        ticking = true;
        lastTickAt = SystemClock.elapsedRealtime();
        handler.post(tick);
    }

    private void stopTicking() {
        if (!ticking) {
            return;
        }
        GameRound current = current();
        if (current != null) {
            addPlayedTime(current);
        }
        ticking = false;
        handler.removeCallbacks(tick);
    }

    private void addPlayedTime(GameRound current) {
        long now = SystemClock.elapsedRealtime();
        current.playedMs += now - lastTickAt;
        lastTickAt = now;
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacks(tick);
        player.state().removeObserver(playerObserver);
        // Leaving the game stops its station
        GameRound current = current();
        if (current != null && current.station() != null && player.isCurrent(current.station())) {
            player.stop();
        }
    }
}
