package me.sectiontn.ondes.game;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class StreakTest {

    @Test
    public void noDailyPlayed_isZero() {
        assertEquals(0, Streak.length(Collections.emptyList(), 147));
    }

    @Test
    public void runEndingToday() {
        assertEquals(3, Streak.length(Arrays.asList(145, 146, 147), 147));
    }

    @Test
    public void runEndingYesterday_stillCounts() {
        assertEquals(2, Streak.length(Arrays.asList(145, 146), 147));
    }

    @Test
    public void missedDayResetsIt() {
        assertEquals(1, Streak.length(Arrays.asList(140, 141, 142, 147), 147));
        assertEquals(0, Streak.length(Arrays.asList(144, 145), 147));
    }
}
