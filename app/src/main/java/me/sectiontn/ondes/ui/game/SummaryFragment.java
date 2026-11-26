package me.sectiontn.ondes.ui.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.repo.ProgressRepository;
import me.sectiontn.ondes.game.DailySeed;
import me.sectiontn.ondes.game.Outcome;
import me.sectiontn.ondes.game.RoundRecord;
import me.sectiontn.ondes.game.ScoreCalculator;
import me.sectiontn.ondes.ui.common.Motion;
import me.sectiontn.ondes.ui.common.UiFormat;

/** End of a game: the total score, the five rounds, and Share (daily) or Play again (free play). */
public class SummaryFragment extends Fragment {

    // The share squares: green, yellow and red
    private static final String SQUARE_GREEN = "🟩";
    private static final String SQUARE_YELLOW = "🟨";
    private static final String SQUARE_RED = "🟥";
    private static final long SCORE_COUNT_MS = 900;
    // The squares wait for their box to rise in, then come one after another
    private static final long SQUARES_DELAY_MS = 350;
    private static final long SQUARE_GAP_MS = 70;

    private GameViewModel game;
    private boolean entered = false;

    public SummaryFragment() {
        super(R.layout.fragment_summary);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        game = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
        view.findViewById(R.id.close_button).setOnClickListener(v -> requireActivity().finish());

        RecyclerView list = view.findViewById(R.id.rounds_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL);
        divider.setDividerColor(ContextCompat.getColor(requireContext(), R.color.border));
        divider.setLastItemDecorated(false);
        list.addItemDecoration(divider);

        TextView streak = view.findViewById(R.id.summary_streak_text);
        ProgressRepository.get(requireContext()).streak().observe(getViewLifecycleOwner(), days ->
                streak.setText(getResources().getQuantityString(R.plurals.streak, days, days)));

        game.summary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                bind(view, summary, list);
                // Only the first time, and not on a screen Android brought back
                if (!entered && savedInstanceState == null) {
                    enter(view, summary);
                }
                entered = true;
            }
        });
    }

    /** The sections rise in, the score counts up, and the share squares pop in one by one. */
    private void enter(View view, GameViewModel.Summary summary) {
        Motion.riseInChildren(view.findViewById(R.id.summary_content), 1);
        NumberFormat numbers = UiFormat.numbers(requireContext());
        Motion.count(view.findViewById(R.id.summary_score), 0, summary.score, SCORE_COUNT_MS, numbers::format);
        ViewGroup squares = view.findViewById(R.id.share_squares);
        for (int i = 0; i < squares.getChildCount(); i++) {
            Motion.pop(squares.getChildAt(i), Motion.POP_FROM_SMALL, SQUARES_DELAY_MS + i * SQUARE_GAP_MS);
        }
    }

    private void bind(View view, GameViewModel.Summary summary, RecyclerView list) {
        boolean daily = summary.daily;
        NumberFormat numbers = UiFormat.numbers(requireContext());
        String scoreText = numbers.format(summary.score);
        int playerNumber = ProgressRepository.get(requireContext()).playerNumber(summary.dayNumber);

        TextView eyebrow = view.findViewById(R.id.summary_eyebrow);
        eyebrow.setText(daily ? getString(R.string.daily_title, playerNumber) : getString(R.string.mode_free_play));

        TextView scoreView = view.findViewById(R.id.summary_score);
        scoreView.setText(scoreText);
        ViewCompat.setAccessibilityHeading(scoreView, true);
        TextView max = view.findViewById(R.id.summary_max);
        max.setText(getString(R.string.summary_max, numbers.format(ScoreCalculator.BASE_POINTS * GameViewModel.ROUND_COUNT)));

        // Streak and sharing belong to the daily only
        view.findViewById(R.id.summary_streak).setVisibility(daily ? View.VISIBLE : View.GONE);
        list.setAdapter(new RoundAdapter(summary.rounds, game.catalog()));

        view.findViewById(R.id.share_preview).setVisibility(daily ? View.VISIBLE : View.GONE);
        TextView shareLine = view.findViewById(R.id.share_line);
        shareLine.setText(getString(R.string.share_title, playerNumber, scoreText));
        ViewGroup squares = view.findViewById(R.id.share_squares);
        StringBuilder squaresDescription = new StringBuilder();
        for (int i = 0; i < squares.getChildCount(); i++) {
            View square = squares.getChildAt(i);
            if (i >= summary.rounds.size()) {
                square.setVisibility(View.GONE);
                continue;
            }
            Outcome outcome = summary.rounds.get(i).outcome;
            ViewCompat.setBackgroundTintList(square, ContextCompat.getColorStateList(requireContext(), OutcomeStyle.color(outcome)));
            if (i > 0) {
                squaresDescription.append(", ");
            }
            squaresDescription.append(getString(OutcomeStyle.description(outcome)));
        }
        squares.setContentDescription(squaresDescription);

        MaterialButton action = view.findViewById(R.id.summary_action);
        action.setText(daily ? R.string.action_share : R.string.action_play_again);
        action.setIconResource(daily ? R.drawable.ic_share : R.drawable.ic_shuffle);
        action.setOnClickListener(v -> {
            if (daily) {
                share(summary, scoreText);
            } else {
                game.playAgain();
            }
        });

        TextView nextDaily = view.findViewById(R.id.next_daily);
        nextDaily.setVisibility(daily ? View.VISIBLE : View.GONE);
        long left = DailySeed.millisUntilMidnight(Calendar.getInstance());
        nextDaily.setText(getString(R.string.next_daily, (int) TimeUnit.MILLISECONDS.toHours(left),
                (int) (TimeUnit.MILLISECONDS.toMinutes(left) % 60)));
    }

    /** Sends "Ondes #7 · 1,760 pts" and a line of colored squares to any app. */
    private void share(GameViewModel.Summary summary, String scoreText) {
        int playerNumber = ProgressRepository.get(requireContext()).playerNumber(summary.dayNumber);
        String text = getString(R.string.share_text, playerNumber, scoreText) + "\n" + squares(summary.rounds);
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser)));
    }

    static String squares(List<RoundRecord> rounds) {
        StringBuilder line = new StringBuilder();
        for (RoundRecord round : rounds) {
            switch (round.outcome) {
                case PERFECT:
                    line.append(SQUARE_GREEN);
                    break;
                case HINT:
                    line.append(SQUARE_YELLOW);
                    break;
                default:
                    line.append(SQUARE_RED);
                    break;
            }
        }
        return line.toString();
    }
}
