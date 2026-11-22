package me.sectiontn.ondes.ui.game;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.text.NumberFormat;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.sample.SampleData;

/** End of a game: the total score, the five rounds, and Share (daily) or Play again (free play). */
public class SummaryFragment extends Fragment {

    private static final String ARG_DAILY = "daily";

    public SummaryFragment() {
        super(R.layout.fragment_summary);
    }

    public static SummaryFragment newInstance(boolean daily) {
        SummaryFragment fragment = new SummaryFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_DAILY, daily);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        boolean daily = requireArguments().getBoolean(ARG_DAILY);
        NumberFormat numbers = NumberFormat.getIntegerInstance();
        int score = SampleData.scoreBefore(SampleData.ROUND_COUNT + 1);
        String scoreText = numbers.format(score);

        view.findViewById(R.id.close_button).setOnClickListener(v -> requireActivity().finish());
        TextView eyebrow = view.findViewById(R.id.summary_eyebrow);
        eyebrow.setText(daily ? getString(R.string.daily_title, SampleData.DAILY_NUMBER) : getString(R.string.mode_free_play));

        TextView scoreView = view.findViewById(R.id.summary_score);
        scoreView.setText(scoreText);
        ViewCompat.setAccessibilityHeading(scoreView, true);
        TextView max = view.findViewById(R.id.summary_max);
        max.setText(getString(R.string.summary_max, numbers.format(SampleData.MAX_POINTS)));

        // Streak and sharing belong to the daily only
        view.findViewById(R.id.summary_streak).setVisibility(daily ? View.VISIBLE : View.GONE);
        TextView streak = view.findViewById(R.id.summary_streak_text);
        streak.setText(getString(R.string.streak, SampleData.STREAK_DAYS));

        RecyclerView list = view.findViewById(R.id.rounds_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new RoundAdapter(SampleData.ROUNDS));
        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL);
        divider.setDividerColor(ContextCompat.getColor(requireContext(), R.color.border));
        divider.setLastItemDecorated(false);
        list.addItemDecoration(divider);

        view.findViewById(R.id.share_preview).setVisibility(daily ? View.VISIBLE : View.GONE);
        TextView shareLine = view.findViewById(R.id.share_line);
        shareLine.setText(getString(R.string.share_title, SampleData.DAILY_NUMBER, scoreText));
        ViewGroup squares = view.findViewById(R.id.share_squares);
        StringBuilder squaresDescription = new StringBuilder();
        for (int i = 0; i < squares.getChildCount(); i++) {
            SampleData.Round round = SampleData.ROUNDS.get(i);
            ViewCompat.setBackgroundTintList(squares.getChildAt(i),
                    ContextCompat.getColorStateList(requireContext(), OutcomeStyle.color(round.outcome)));
            if (i > 0) {
                squaresDescription.append(", ");
            }
            squaresDescription.append(getString(OutcomeStyle.description(round.outcome)));
        }
        squares.setContentDescription(squaresDescription);

        MaterialButton action = view.findViewById(R.id.summary_action);
        action.setText(daily ? R.string.action_share : R.string.action_play_again);
        action.setIconResource(daily ? R.drawable.ic_share : R.drawable.ic_shuffle);

        TextView nextDaily = view.findViewById(R.id.next_daily);
        nextDaily.setVisibility(daily ? View.VISIBLE : View.GONE);
        nextDaily.setText(getString(R.string.next_daily, SampleData.HOURS_LEFT, SampleData.MINUTES_LEFT));
    }
}
