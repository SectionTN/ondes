package me.sectiontn.ondes.ui.game;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import java.text.NumberFormat;
import java.util.List;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.game.Outcome;
import me.sectiontn.ondes.ui.common.Motion;
import me.sectiontn.ondes.ui.common.UiFormat;

/** Fills the top bar shared by the Round and Result screens. */
final class GameTopBar {

    private GameTopBar() {
    }

    /**
     * @param outcomes      outcomes of the finished rounds, in order
     * @param roundPlaying  true on the Round screen, where the current round's segment is accent
     */
    static void bind(View topBar, int roundNumber, int roundCount, int score, List<Outcome> outcomes,
                     boolean roundPlaying) {
        Context context = topBar.getContext();
        TextView label = topBar.findViewById(R.id.round_label);
        label.setText(context.getString(R.string.round_label, roundNumber, roundCount));
        TextView chip = topBar.findViewById(R.id.score_chip);
        chip.setText(UiFormat.numbers(chip.getContext()).format(score));

        ViewGroup segments = topBar.findViewById(R.id.segments);
        for (int i = 0; i < segments.getChildCount(); i++) {
            @ColorRes int color;
            if (i < outcomes.size()) {
                color = OutcomeStyle.color(outcomes.get(i));
            } else if (roundPlaying && i == roundNumber - 1) {
                color = R.color.accent;
            } else {
                color = R.color.surface_2;
            }
            ViewCompat.setBackgroundTintList(segments.getChildAt(i), ContextCompat.getColorStateList(context, color));
        }
    }

    /** On the Result screen: the score counts up by the round's points, and its segment turns from accent to its outcome color. */
    static void showOutcome(View topBar, int roundNumber, int points, int score, Outcome outcome) {
        Context context = topBar.getContext();
        NumberFormat numbers = UiFormat.numbers(context);
        Motion.count(topBar.findViewById(R.id.score_chip), score - points, score, Motion.COUNT_MS, numbers::format);
        ViewGroup segments = topBar.findViewById(R.id.segments);
        Motion.tint(segments.getChildAt(roundNumber - 1), ContextCompat.getColor(context, R.color.accent),
                ContextCompat.getColor(context, OutcomeStyle.color(outcome)));
    }
}
