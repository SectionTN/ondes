package me.sectiontn.ondes.sample;

import java.util.ArrayList;
import java.util.List;

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
