package me.sectiontn.ondes.game;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

/**
 * Makes the daily challenge the same for every player without a server: everything is
 * computed from the day number, so two phones on the same date pick the same stations.
 */
public final class DailySeed {

    public static final int ROUND_COUNT = 5;
    private static final long MS_PER_DAY = 24L * 60 * 60 * 1000;
    // Day 1 of the count every phone shares. It seeds the picks and keys the saved results
    private static final long FIRST_DAY_UTC = utcMidnight(2026, 7, 3);

    private DailySeed() {
    }

    /** 3 July 2026 is day 1, 21 November 2026 is day 142. Month is 1 to 12. */
    public static int dayNumber(int year, int month, int day) {
        // Both dates as UTC midnights, so a daylight saving change never makes a day 23 hours long
        return (int) ((utcMidnight(year, month, day) - FIRST_DAY_UTC) / MS_PER_DAY) + 1;
    }

    /** The day number of a date in the phone's own time zone. */
    public static int dayNumber(Calendar local) {
        return dayNumber(local.get(Calendar.YEAR), local.get(Calendar.MONTH) + 1, local.get(Calendar.DAY_OF_MONTH));
    }

    public static int today() {
        return dayNumber(Calendar.getInstance());
    }

    /**
     * The number the player sees: the day they first opened the app is their Daily #1.
     * It never goes below 1, even when the phone's clock is set back before that day.
     */
    public static int playerNumber(int dayNumber, int firstDay) {
        return Math.max(1, dayNumber - firstDay + 1);
    }

    /** Time left before the next daily starts, at local midnight. */
    public static long millisUntilMidnight(Calendar now) {
        Calendar midnight = (Calendar) now.clone();
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return midnight.getTimeInMillis() - now.getTimeInMillis();
    }

    /** The day's five countries: a shuffle of the pool seeded with the day number. */
    public static List<String> countries(int dayNumber, List<String> poolCodes) {
        List<String> codes = new ArrayList<>(poolCodes);
        // Sorted first, so the order of the JSON file cannot change the pick
        Collections.sort(codes);
        Collections.shuffle(codes, new Random(dayNumber));
        return new ArrayList<>(codes.subList(0, Math.min(ROUND_COUNT, codes.size())));
    }

    /**
     * Orders a country's stations for the day: the first one plays, the next ones replace it
     * when its stream is dead. Each station gets the score SHA-256("day:uuid") and the lowest wins.
     * A new day gives every station a new, unrelated score.
     */
    public static List<String> stationOrder(int dayNumber, List<String> uuids) {
        List<String> order = new ArrayList<>(uuids);
        Collections.sort(order, Comparator.comparingLong((String uuid) -> score(dayNumber, uuid))
                .thenComparing(uuid -> uuid));
        return order;
    }

    /** Seeds the shuffle of the four answers, so every player sees them in the same order. */
    public static Random optionsRandom(int dayNumber, int roundIndex) {
        return new Random(dayNumber * 31L + roundIndex);
    }

    // The first 8 bytes of the hash as a number. The sign bit is cleared so the comparison is plain
    private static long score(int dayNumber, String uuid) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((dayNumber + ":" + uuid).getBytes(StandardCharsets.UTF_8));
            long value = 0;
            for (int i = 0; i < 8; i++) {
                value = (value << 8) | (hash[i] & 0xFF);
            }
            return value & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    private static long utcMidnight(int year, int month, int day) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(year, month - 1, day);
        return utc.getTimeInMillis();
    }
}
