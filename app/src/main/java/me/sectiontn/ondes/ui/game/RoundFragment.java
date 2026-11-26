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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.game.GameRound;
import me.sectiontn.ondes.game.ScoreCalculator;
import me.sectiontn.ondes.ui.common.EqualizerView;

/** One round: the live station, the timer, three hints and four answers. */
public class RoundFragment extends Fragment {

    private static final int[] HINT_LABELS = {R.string.hint_language, R.string.hint_continent, R.string.hint_region};
    private static final int[] HINT_IDS = {R.id.hint_language, R.id.hint_continent, R.id.hint_region};
    private static final int[] ANSWER_LETTERS = {R.string.answer_a, R.string.answer_b, R.string.answer_c, R.string.answer_d};
    private static final int[] ANSWER_IDS = {R.id.answer_a, R.id.answer_b, R.id.answer_c, R.id.answer_d};
    private static final int MIN_TEXT_SP = 8;
    private static final int COST_TEXT_SP = 13;
    private static final float DISABLED_ALPHA = 0.4f;

    private GameViewModel game;

    public RoundFragment() {
        super(R.layout.fragment_round);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        game = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        View topBar = view.findViewById(R.id.top_bar);
        GameTopBar.bind(topBar, game.roundNumber(), GameViewModel.ROUND_COUNT, game.score(), game.outcomes(), true);
        topBar.findViewById(R.id.close_button).setOnClickListener(v -> ((GameActivity) requireActivity()).confirmQuit());

        CircularProgressIndicator ring = view.findViewById(R.id.timer_ring);
        ring.setMax(ScoreCalculator.ROUND_SECONDS * 1000);
        TextView timer = view.findViewById(R.id.timer_text);
        game.remainingMs().observe(getViewLifecycleOwner(), remaining -> {
            ring.setProgressCompat((int) (long) remaining, false);
            // Rounded up, so the ring reads 0:20 until a whole second has passed
            int seconds = (int) ((remaining + 999) / 1000);
            timer.setText(getString(R.string.timer, seconds / 60, seconds % 60));
        });

        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.hints_title), true);
        for (int i = 0; i < HINT_IDS.length; i++) {
            final int hint = i;
            view.findViewById(HINT_IDS[i]).setOnClickListener(v -> game.revealHint(hint));
        }
        for (int i = 0; i < ANSWER_IDS.length; i++) {
            final int option = i;
            View answer = view.findViewById(ANSWER_IDS[i]);
            ((TextView) answer.findViewById(R.id.answer_letter)).setText(ANSWER_LETTERS[i]);
            answer.setOnClickListener(v -> game.answer(option));
        }
        view.findViewById(R.id.retry_button).setOnClickListener(v -> game.retry());

        game.round().observe(getViewLifecycleOwner(), round -> bindRound(view, round));
        game.status().observe(getViewLifecycleOwner(), status -> bindStatus(view, status));
    }

    private void bindRound(View view, GameRound round) {
        if (round == null) {
            return;
        }
        for (int i = 0; i < HINT_IDS.length; i++) {
            bindHint(view.findViewById(HINT_IDS[i]), i, round);
        }
        TextView max = view.findViewById(R.id.hints_max);
        max.setText(getString(R.string.points_signed, ScoreCalculator.maxPoints(round.hintsOpen)));

        // The answers unlock once sound has played, and lock again after the first tap
        boolean canAnswer = round.started && !round.finished;
        for (int i = 0; i < ANSWER_IDS.length; i++) {
            View answer = view.findViewById(ANSWER_IDS[i]);
            TextView name = answer.findViewById(R.id.answer_name);
            boolean hasOption = i < round.options.size();
            name.setText(hasOption ? round.options.get(i).name : "");
            answer.setEnabled(canAnswer && hasOption);
            answer.setAlpha(hasOption ? 1f : DISABLED_ALPHA);
        }
    }

    private void bindStatus(View view, GameViewModel.Status status) {
        boolean error = status == GameViewModel.Status.NETWORK_ERROR || status == GameViewModel.Status.STREAM_ERROR;
        boolean playing = status == GameViewModel.Status.PLAYING;
        boolean loading = status == GameViewModel.Status.LOADING;

        view.findViewById(R.id.live_pill).setVisibility(error ? View.INVISIBLE : View.VISIBLE);
        view.findViewById(R.id.timer_frame).setVisibility(error ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.dial_strip).setVisibility(error ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.round_error).setVisibility(error ? View.VISIBLE : View.GONE);
        if (error) {
            TextView message = view.findViewById(R.id.round_error_text);
            message.setText(status == GameViewModel.Status.NETWORK_ERROR ? R.string.error_directory : R.string.error_streams);
        }

        view.findViewById(R.id.timer_spinner).setVisibility(loading ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.timer_loading).setVisibility(loading ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.timer_ring).setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        view.findViewById(R.id.timer_text).setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        EqualizerView equalizer = view.findViewById(R.id.timer_equalizer);
        equalizer.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        equalizer.setPlaying(playing);
    }

    private void bindHint(View tile, int hint, GameRound round) {
        @StringRes int label = HINT_LABELS[hint];
        int cost = ScoreCalculator.HINT_COSTS[hint];
        String value = round.hintValue(hint);
        boolean open = round.hintsOpen[hint];
        // No data for this hint, or the round is over: the tile cannot be opened
        boolean available = value != null && !round.finished;

        ImageView icon = tile.findViewById(R.id.hint_icon);
        TextView labelView = tile.findViewById(R.id.hint_label);
        TextView detail = tile.findViewById(R.id.hint_detail);
        labelView.setText(label);
        if (open) {
            tile.setBackgroundResource(R.drawable.bg_hint_open);
            tile.setClickable(false);
            tile.setAlpha(1f);
            icon.setVisibility(View.GONE);
            // The answer may take two lines, the cost always fits on one. Autosize goes off first or it keeps its size
            TextViewCompat.setAutoSizeTextTypeWithDefaults(detail, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            TextViewCompat.setTextAppearance(detail, R.style.TextAppearance_Ondes_Label);
            detail.setMaxLines(2);
            detail.setText(value);
            tile.setContentDescription(null);
        } else {
            tile.setBackgroundResource(R.drawable.bg_dashed_rounded);
            tile.setClickable(available);
            tile.setEnabled(available);
            tile.setAlpha(value == null ? DISABLED_ALPHA : 1f);
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
