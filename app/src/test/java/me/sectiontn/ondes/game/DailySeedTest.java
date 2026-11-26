package me.sectiontn.ondes.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

public class DailySeedTest {

    @Test
    public void firstDayIsNumberOne() {
        assertEquals(1, DailySeed.dayNumber(2026, 7, 3));
    }

    @Test
    public void dayNumber_21November2026_is142() {
        assertEquals(142, DailySeed.dayNumber(2026, 11, 21));
    }

    @Test
    public void playerNumber_startsAtOneOnTheFirstDayAndCountsUp() {
        assertEquals(1, DailySeed.playerNumber(147, 147));
        assertEquals(2, DailySeed.playerNumber(148, 147));
        assertEquals(31, DailySeed.playerNumber(177, 147));
    }

    @Test
    public void playerNumber_staysAtOneWhenTheClockGoesBack() {
        assertEquals(1, DailySeed.playerNumber(146, 147));
    }

    @Test
    public void daylightSavingChangeStillCountsOneDay() {
        // Europe moves its clocks back on 25 October 2026
        Calendar before = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        before.clear();
        before.set(2026, Calendar.OCTOBER, 25, 23, 30);
        Calendar after = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        after.clear();
        after.set(2026, Calendar.OCTOBER, 26, 0, 30);
        assertEquals(DailySeed.dayNumber(before) + 1, DailySeed.dayNumber(after));
    }

    @Test
    public void millisUntilMidnight_countsToTheNextDay() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        now.clear();
        now.set(2026, Calendar.NOVEMBER, 21, 18, 18);
        assertEquals((5 * 60 + 42) * 60_000L, DailySeed.millisUntilMidnight(now));
    }

    @Test
    public void countries_areFiveDistinctAndTheSameForEveryone() {
        List<String> pool = Arrays.asList("AR", "BR", "DE", "FR", "IN", "JP", "KE", "MX", "PL", "PT", "US");
        List<String> first = DailySeed.countries(142, pool);
        assertEquals(5, first.size());
        assertEquals(5, new HashSet<>(first).size());
        assertEquals(first, DailySeed.countries(142, pool));
    }

    @Test
    public void countries_doNotDependOnThePoolOrder() {
        List<String> pool = new ArrayList<>(Arrays.asList("AR", "BR", "DE", "FR", "IN", "JP", "KE", "MX", "PL", "PT", "US"));
        List<String> reversed = new ArrayList<>(pool);
        Collections.reverse(reversed);
        assertEquals(DailySeed.countries(142, pool), DailySeed.countries(142, reversed));
    }

    @Test
    public void stationOrder_isStableForADayAndChangesTheNextDay() {
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            uuids.add("station-" + i);
        }
        List<String> today = DailySeed.stationOrder(142, uuids);
        Collections.shuffle(uuids);
        assertEquals(today, DailySeed.stationOrder(142, uuids));

        // A plain String hash would pick the same station every day, SHA-256 does not
        int changes = 0;
        for (int day = 142; day < 149; day++) {
            if (!DailySeed.stationOrder(day, uuids).get(0).equals(DailySeed.stationOrder(day + 1, uuids).get(0))) {
                changes++;
            }
        }
        assertNotEquals(0, changes);
    }
}
