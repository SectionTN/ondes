package me.sectiontn.ondes.game;

/** Points of one round: 500, minus the hints used, minus 5 per second of listening. */
public final class ScoreCalculator {

    public static final int BASE_POINTS = 500;
    public static final int ROUND_SECONDS = 20;
    public static final int POINTS_PER_SECOND = 5;
    /** Language, Continent, Region, in the order of the hint tiles. */
    public static final int[] HINT_COSTS = {50, 100, 200};

    private ScoreCalculator() {
    }

    /** Total cost of the open hints. */
    public static int hintCost(boolean[] hintsOpen) {
        int cost = 0;
        for (int i = 0; i < HINT_COSTS.length; i++) {
            if (hintsOpen[i]) {
                cost += HINT_COSTS[i];
            }
        }
        return cost;
    }

    /** 5 points per whole second of playback before the answer: 6.9 seconds costs 30. */
    public static int timePenalty(long playedMs) {
        return POINTS_PER_SECOND * (int) (playedMs / 1000);
    }

    /** What the Round screen promises: the base minus the open hints, before the time penalty. */
    public static int maxPoints(boolean[] hintsOpen) {
        return BASE_POINTS - hintCost(hintsOpen);
    }

    /** 0 for a wrong answer or a timeout, otherwise the base minus hints and time, never below 0. */
    public static int points(boolean correct, boolean[] hintsOpen, long playedMs) {
        if (!correct) {
            return 0;
        }
        return Math.max(0, BASE_POINTS - hintCost(hintsOpen) - timePenalty(playedMs));
    }

    public static Outcome outcome(boolean correct, boolean[] hintsOpen) {
        if (!correct) {
            return Outcome.WRONG;
        }
        return hintCost(hintsOpen) == 0 ? Outcome.PERFECT : Outcome.HINT;
    }
}
