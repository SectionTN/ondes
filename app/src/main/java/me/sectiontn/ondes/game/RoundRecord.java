package me.sectiontn.ondes.game;

import androidx.annotation.Nullable;

/** A finished round as the Summary lists it. Saved as JSON with the daily result. */
public class RoundRecord {

    public String countryCode;
    public String stationName;
    /** The station's city or region, may be empty. */
    public String state;
    /** The country the player picked when wrong, null when right or out of time. */
    @Nullable
    public String guessCode;
    public Outcome outcome;
    public int points;

    /** The letter of the share squares: G green, Y yellow, R red. */
    public char squareLetter() {
        switch (outcome) {
            case PERFECT:
                return 'G';
            case HINT:
                return 'Y';
            default:
                return 'R';
        }
    }
}
