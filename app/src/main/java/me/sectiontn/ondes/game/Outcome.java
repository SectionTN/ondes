package me.sectiontn.ondes.game;

/** How a round ended. It sets the round's color: green, yellow or red. */
public enum Outcome {
    /** Right country, no hint used. */
    PERFECT,
    /** Right country after at least one hint. */
    HINT,
    /** Wrong country, or the timer ran out. */
    WRONG
}
