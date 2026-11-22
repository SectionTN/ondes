package me.sectiontn.ondes.ui.game;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.game.Outcome;
import me.sectiontn.ondes.sample.SampleData;

/** One round: the live station, the timer, three hints and four answers. */
public class RoundFragment extends Fragment {

    private static final String ARG_ROUND = "round";
    private static final int[] HINT_LABELS = {R.string.hint_language, R.string.hint_continent, R.string.hint_region};
    private static final int[] HINT_COSTS = {50, 100, 200};
    private static final int[] ANSWER_LETTERS = {R.string.answer_a, R.string.answer_b, R.string.answer_c, R.string.answer_d};
    private static final int[] ANSWER_IDS = {R.id.answer_a, R.id.answer_b, R.id.answer_c, R.id.answer_d};
    private static final int MIN_TEXT_SP = 8;
    private static final int COST_TEXT_SP = 13;

    // The sample round starts with the Language hint open, as in the design
    private final boolean[] hintOpen = {true, false, false};
    // Set by the first answer tap: the spec allows one answer per round
    private boolean answered = false;

    public RoundFragment() {
        super(R.layout.fragment_round);
    }

    public static RoundFragment newInstance(int roundNumber) {
        RoundFragment fragment = new RoundFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ROUND, roundNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        int roundNumber = requireArguments().getInt(ARG_ROUND);
        List<Outcome> outcomes = new ArrayList<>();
        for (int i = 0; i < roundNumber - 1; i++) {
            outcomes.add(SampleData.ROUNDS.get(i).outcome);
        }
        View topBar = view.findViewById(R.id.top_bar);
        GameTopBar.bind(topBar, roundNumber, SampleData.ROUND_COUNT, SampleData.scoreBefore(roundNumber), outcomes, true);
        topBar.findViewById(R.id.close_button).setOnClickListener(v -> requireActivity().finish());

        CircularProgressIndicator ring = view.findViewById(R.id.timer_ring);
        ring.setMax(SampleData.ROUND_SECONDS);
        ring.setProgress(SampleData.SECONDS_LEFT);
        TextView timer = view.findViewById(R.id.timer_text);
        timer.setText(getString(R.string.timer, SampleData.SECONDS_LEFT / 60, SampleData.SECONDS_LEFT % 60));

        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.hints_title), true);
        bindHints(view);

        for (int i = 0; i < ANSWER_IDS.length; i++) {
            View answer = view.findViewById(ANSWER_IDS[i]);
            ((TextView) answer.findViewById(R.id.answer_letter)).setText(ANSWER_LETTERS[i]);
            ((TextView) answer.findViewById(R.id.answer_name)).setText(SampleData.ANSWERS[i]);
            answer.setOnClickListener(v -> {
                if (answered) {
                    return;
                }
                answered = true;
                ((GameActivity) requireActivity()).showResult();
            });
        }
    }

    private void bindHints(View view) {
        View[] tiles = {view.findViewById(R.id.hint_language), view.findViewById(R.id.hint_continent),
                view.findViewById(R.id.hint_region)};
        int maxPoints = SampleData.BASE_POINTS;
        for (int i = 0; i < tiles.length; i++) {
            final int index = i;
            bindHint(tiles[i], HINT_LABELS[i], HINT_COSTS[i], SampleData.HINT_VALUES[i], hintOpen[i]);
            if (hintOpen[i]) {
                maxPoints -= HINT_COSTS[i];
            } else {
                tiles[i].setOnClickListener(v -> {
                    hintOpen[index] = true;
                    bindHints(view);
                });
            }
        }
        TextView max = view.findViewById(R.id.hints_max);
        max.setText(getString(R.string.points_signed, maxPoints));
    }

    private void bindHint(View tile, @StringRes int label, int cost, String value, boolean open) {
        ImageView icon = tile.findViewById(R.id.hint_icon);
        TextView labelView = tile.findViewById(R.id.hint_label);
        TextView detail = tile.findViewById(R.id.hint_detail);
        labelView.setText(label);
        if (open) {
            tile.setBackgroundResource(R.drawable.bg_hint_open);
            tile.setOnClickListener(null);
            tile.setClickable(false);
            icon.setVisibility(View.GONE);
            // The answer may take two lines, the cost always fits on one. Autosize goes off first or it keeps its size
            TextViewCompat.setAutoSizeTextTypeWithDefaults(detail, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            TextViewCompat.setTextAppearance(detail, R.style.TextAppearance_Ondes_Label);
            detail.setMaxLines(2);
            detail.setText(value);
            tile.setContentDescription(null);
        } else {
            tile.setBackgroundResource(R.drawable.bg_dashed_rounded);
            icon.setVisibility(View.VISIBLE);
            TextViewCompat.setTextAppearance(detail, R.style.TextAppearance_Ondes_Mono_HintCost);
            detail.setMaxLines(1);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(detail, MIN_TEXT_SP, COST_TEXT_SP, 1, TypedValue.COMPLEX_UNIT_SP);
            detail.setText(getString(R.string.hint_cost, cost));
            String name = getString(label).toLowerCase(Locale.getDefault());
            tile.setContentDescription(getString(R.string.hint_reveal, name, cost));
        }
    }
}
