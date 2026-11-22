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
}
