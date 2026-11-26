package me.sectiontn.ondes.game;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.data.countries.Country;

/** One round of a game: the country to find, its stations, the hints and the answer. */
public class GameRound {

    public static final int HINT_LANGUAGE = 0;
    public static final int HINT_CONTINENT = 1;
    public static final int HINT_REGION = 2;

    public final Country answer;
    /** Stations of the country in the order to try them: when one is dead, the next one plays. */
    public List<Station> candidates = new ArrayList<>();
    public int candidateIndex = 0;
    /** The four countries on the buttons, set once the stations are known. */
    public List<Country> options = new ArrayList<>();
    public final boolean[] hintsOpen = new boolean[3];
    /** Milliseconds of sound heard so far. The 20-second timer counts only these. */
    public long playedMs = 0;
    /** True once sound has played, so the answers unlock. */
    public boolean started = false;

    public boolean finished = false;
    /** The player's pick, null after a timeout. */
    @Nullable
    public Country guess;
    public int points;
    public Outcome outcome;

    public GameRound(Country answer) {
        this.answer = answer;
    }

    @Nullable
    public Station station() {
        return candidates.isEmpty() ? null : candidates.get(candidateIndex % candidates.size());
    }

    public boolean isCorrect() {
        return guess != null && guess.code.equals(answer.code);
    }

    /** The hint's text, or null when the station has no data for it (the tile is then disabled). */
    @Nullable
    public String hintValue(int hint) {
        Station station = station();
        switch (hint) {
            case HINT_LANGUAGE:
                return station == null || station.languages.isEmpty() ? null : station.languages.get(0);
            case HINT_CONTINENT:
                return answer.continentHint();
            default:
                return station == null || station.state.isEmpty() ? null : station.state;
        }
    }

    public long remainingMs() {
        return Math.max(0, ScoreCalculator.ROUND_SECONDS * 1000L - playedMs);
    }

    /** Ends the round with a pick, or with null when the time ran out. */
    public void finish(@Nullable Country pick) {
        finished = true;
        guess = pick;
        boolean correct = isCorrect();
        points = ScoreCalculator.points(correct, hintsOpen, playedMs);
        outcome = ScoreCalculator.outcome(correct, hintsOpen);
    }

    public RoundRecord toRecord() {
        RoundRecord record = new RoundRecord();
        Station station = station();
        record.countryCode = answer.code;
        record.stationName = station != null ? station.name : "";
        record.state = station != null ? station.state : "";
        record.guessCode = !isCorrect() && guess != null ? guess.code : null;
        record.outcome = outcome;
        record.points = points;
        return record;
    }
}
