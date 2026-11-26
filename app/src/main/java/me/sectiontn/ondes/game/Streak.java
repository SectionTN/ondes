package me.sectiontn.ondes.game;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Counts the days in a row with a finished daily. */
public final class Streak {

    private Streak() {
    }

    /**
     * The run of consecutive played days that ends today, or yesterday when today is not played yet.
     * A missed day ends the run, so the streak goes back to 0.
     */
    public static int length(Collection<Integer> playedDays, int today) {
        Set<Integer> played = new HashSet<>(playedDays);
        int day = played.contains(today) ? today : today - 1;
        int length = 0;
        while (played.contains(day)) {
            length++;
            day--;
        }
        return length;
    }
}
