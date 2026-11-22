package me.sectiontn.ondes.sample;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.sectiontn.ondes.game.Outcome;

/** Fixed values from the design mock, shown until the real data sources exist. */
public final class SampleData {

    private SampleData() {
    }

    // Home
    public static final String TODAY_LABEL = "Sat 21 Nov";
    public static final int DAILY_NUMBER = 142;
    public static final int STREAK_DAYS = 6;
    public static final int HOURS_LEFT = 5;
    public static final int MINUTES_LEFT = 42;

    /** A station as the lists show it. */
    public static final class Station {
        public final String countryCode;
        public final String name;
        public final String place;
        public boolean favorite;

        Station(String countryCode, String name, String place, boolean favorite) {
            this.countryCode = countryCode;
            this.name = name;
            this.place = place;
            this.favorite = favorite;
        }
    }

    // Explore
    public static final Station NOW_PLAYING = new Station("BR", "Rádio Gaúcha", "Porto Alegre, Brazil", false);

    public static List<Station> recentStations() {
        List<Station> stations = new ArrayList<>();
        stations.add(new Station("JP", "FM802", "Osaka, Japan", true));
        stations.add(new Station("FR", "TSF Jazz", "Paris, France", false));
        stations.add(new Station("AR", "Cadena 3", "Córdoba, Argentina", false));
        return stations;
    }

    /** A passport stamp: a country guessed right at least once. */
    public static final class Stamp {
        public final String countryCode;
        public final String name;
        public final String continent;
        public final String date;

        Stamp(String countryCode, String name, String continent, String date) {
            this.countryCode = countryCode;
            this.name = name;
            this.continent = continent;
            this.date = date;
        }
    }

    // Passport
    public static final int STAMP_COUNT = 23;
    public static final int TOTAL_COUNTRIES = 195;
    public static final String[] CONTINENTS = {"Africa", "Americas", "Asia", "Europe", "Oceania"};
    public static final int[] CONTINENT_COUNTS = {5, 6, 4, 7, 1};

    /** One finished round of the sample game. */
    public static final class Round {
        public final String country;
        public final String stationName;
        public final String city;
        public final List<String> tags;
        public final Outcome outcome;
        public final int points;
        public final int hintCost;
        public final int timePenalty;
        /** The country the player picked when the answer was wrong, or null after a timeout. */
        @Nullable
        public final String guess;

        Round(String country, String stationName, String city, List<String> tags, Outcome outcome,
              int points, int hintCost, int timePenalty, @Nullable String guess) {
            this.country = country;
            this.stationName = stationName;
            this.city = city;
            this.tags = tags;
            this.outcome = outcome;
            this.points = points;
            this.hintCost = hintCost;
            this.timePenalty = timePenalty;
            this.guess = guess;
        }
    }

    // Game
    public static final int ROUND_COUNT = 5;
    public static final int MAX_POINTS = 2500;
    public static final int SECONDS_LEFT = 14;
    public static final int ROUND_SECONDS = 20;
    public static final String[] ANSWERS = {"Portugal", "Brazil", "Angola", "Mozambique"};
    public static final String[] HINT_VALUES = {"Portuguese", "South America", "Rio Grande do Sul"};

    public static final List<Round> ROUNDS = Arrays.asList(
            new Round("Japan", "FM802", "Osaka", Collections.singletonList("Music"),
                    Outcome.PERFECT, 500, 0, 0, null),
            new Round("Kenya", "Capital FM", "Nairobi", Collections.singletonList("Music"),
                    Outcome.PERFECT, 460, 0, 40, null),
            new Round("Argentina", "Cadena 3", "Córdoba", Arrays.asList("News", "Talk"),
                    Outcome.HINT, 380, 100, 20, null),
            new Round("Poland", "Radio ZET", "Warsaw", Collections.singletonList("Music"),
                    Outcome.WRONG, 0, 0, 0, "Czechia"),
            new Round("Brazil", "Rádio Gaúcha", "Porto Alegre", Arrays.asList("News", "Talk", "Sports"),
                    Outcome.HINT, 420, 50, 30, null));

    /** Points scored in the rounds before this one (1 to 5). */
    public static int scoreBefore(int roundNumber) {
        int score = 0;
        for (int i = 0; i < roundNumber - 1; i++) {
            score += ROUNDS.get(i).points;
        }
        return score;
    }

    /** Newest first, as the grid shows them. */
    public static List<Stamp> stamps() {
        List<Stamp> stamps = new ArrayList<>();
        stamps.add(new Stamp("JP", "Japan", "Asia", "21 Nov"));
        stamps.add(new Stamp("KE", "Kenya", "Africa", "21 Nov"));
        stamps.add(new Stamp("AR", "Argentina", "Americas", "21 Nov"));
        stamps.add(new Stamp("BR", "Brazil", "Americas", "21 Nov"));
        stamps.add(new Stamp("FR", "France", "Europe", "18 Nov"));
        stamps.add(new Stamp("PT", "Portugal", "Europe", "17 Nov"));
        stamps.add(new Stamp("DE", "Germany", "Europe", "16 Nov"));
        stamps.add(new Stamp("IN", "India", "Asia", "15 Nov"));
        stamps.add(new Stamp("MA", "Morocco", "Africa", "14 Nov"));
        stamps.add(new Stamp("CA", "Canada", "Americas", "12 Nov"));
        stamps.add(new Stamp("NZ", "New Zealand", "Oceania", "10 Nov"));
        stamps.add(new Stamp("SN", "Senegal", "Africa", "09 Nov"));
        return stamps;
    }
}
