package me.sectiontn.ondes.game;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ScoreCalculatorTest {

    private static final boolean[] NO_HINTS = {false, false, false};
    private static final boolean[] LANGUAGE_HINT = {true, false, false};
    private static final boolean[] ALL_HINTS = {true, true, true};

    @Test
    public void designExample_languageHintAndSixSeconds_gives420() {
        // 14 seconds left on the ring means 6 seconds played
        assertEquals(420, ScoreCalculator.points(true, LANGUAGE_HINT, 6_000));
    }

    @Test
    public void partOfASecondDoesNotCount() {
        assertEquals(30, ScoreCalculator.timePenalty(6_999));
        assertEquals(0, ScoreCalculator.timePenalty(999));
    }

    @Test
    public void instantAnswerWithoutHints_givesTheFullBase() {
        assertEquals(500, ScoreCalculator.points(true, NO_HINTS, 0));
        assertEquals(Outcome.PERFECT, ScoreCalculator.outcome(true, NO_HINTS));
    }

    @Test
    public void wrongAnswer_givesZeroWhateverTheTime() {
        assertEquals(0, ScoreCalculator.points(false, NO_HINTS, 1_000));
        assertEquals(Outcome.WRONG, ScoreCalculator.outcome(false, NO_HINTS));
    }

    @Test
    public void hintCostsAddUp() {
        assertEquals(350, ScoreCalculator.hintCost(ALL_HINTS));
        assertEquals(150, ScoreCalculator.maxPoints(ALL_HINTS));
        assertEquals(Outcome.HINT, ScoreCalculator.outcome(true, ALL_HINTS));
    }

    @Test
    public void pointsNeverGoBelowZero() {
        // 350 for hints and 19 seconds (95) leaves 55, then the full 20 seconds would leave 50
        assertEquals(55, ScoreCalculator.points(true, ALL_HINTS, 19_000));
        assertEquals(0, ScoreCalculator.points(true, ALL_HINTS, 120_000));
    }
}
