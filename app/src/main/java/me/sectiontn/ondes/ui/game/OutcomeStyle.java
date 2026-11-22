package me.sectiontn.ondes.ui.game;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.game.Outcome;

/** The color, icon and spoken label of a round outcome, shared by every game screen. */
final class OutcomeStyle {

    private OutcomeStyle() {
    }

    @ColorRes
    static int color(Outcome outcome) {
        switch (outcome) {
            case PERFECT:
                return R.color.correct;
            case HINT:
                return R.color.hint;
            default:
                return R.color.wrong;
        }
    }

    @DrawableRes
    static int icon(Outcome outcome) {
        switch (outcome) {
            case PERFECT:
                return R.drawable.ic_check;
            case HINT:
                return R.drawable.ic_lightbulb;
            default:
                return R.drawable.ic_close;
        }
    }

    @StringRes
    static int description(Outcome outcome) {
        switch (outcome) {
            case PERFECT:
                return R.string.outcome_correct;
            case HINT:
                return R.string.outcome_hint;
            default:
                return R.string.outcome_wrong;
        }
    }
}
