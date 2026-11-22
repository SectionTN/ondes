package me.sectiontn.ondes.ui.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.game.Outcome;
import me.sectiontn.ondes.sample.SampleData;

/** After an answer: right or wrong, the points, and the station that was playing. */
public class ResultFragment extends Fragment {

    private static final String ARG_ROUND = "round";
    private static final int BASE_POINTS = 500;
    private static final int MAX_TAGS = 3;

    private boolean playing = true;
    private boolean saved = false;

    public ResultFragment() {
        super(R.layout.fragment_result);
    }

    public static ResultFragment newInstance(int roundNumber) {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ROUND, roundNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        int roundNumber = requireArguments().getInt(ARG_ROUND);
        SampleData.Round round = SampleData.ROUNDS.get(roundNumber - 1);

        List<Outcome> outcomes = new ArrayList<>();
        for (int i = 0; i < roundNumber; i++) {
            outcomes.add(SampleData.ROUNDS.get(i).outcome);
        }
        View topBar = view.findViewById(R.id.top_bar);
        int score = SampleData.scoreBefore(roundNumber) + round.points;
        GameTopBar.bind(topBar, roundNumber, SampleData.ROUND_COUNT, score, outcomes, false);
        topBar.findViewById(R.id.close_button).setOnClickListener(v -> requireActivity().finish());

        bindOutcome(view, round);
        bindStation(view, round);

        MaterialButton next = view.findViewById(R.id.next_button);
        next.setText(roundNumber < SampleData.ROUND_COUNT ? R.string.action_next_round : R.string.action_see_results);
        next.setOnClickListener(v -> ((GameActivity) requireActivity()).showNextRound());

        MaterialButton save = view.findViewById(R.id.save_button);
        save.setOnClickListener(v -> {
            saved = !saved;
            bindSave(save);
        });
    }

    private void bindOutcome(View view, SampleData.Round round) {
        boolean right = round.outcome != Outcome.WRONG;
        int textColor = ContextCompat.getColor(requireContext(), right ? R.color.correct : R.color.wrong_text);

        ImageView circle = view.findViewById(R.id.result_circle);
        circle.setBackgroundResource(right ? R.drawable.bg_result_circle : R.drawable.bg_result_circle_wrong);
        circle.setImageResource(right ? R.drawable.ic_check : R.drawable.ic_close);
        ImageViewCompat.setImageTintList(circle, ContextCompat.getColorStateList(requireContext(),
                right ? R.color.correct : R.color.wrong));
        circle.setContentDescription(getString(right ? R.string.outcome_correct : R.string.outcome_wrong));

        TextView country = view.findViewById(R.id.result_country);
        country.setText(round.country);
        ViewCompat.setAccessibilityHeading(country, true);

        TextView points = view.findViewById(R.id.result_points);
        points.setText(right ? getString(R.string.points_signed, round.points) : getString(R.string.points_zero));
        points.setTextColor(textColor);

        TextView detail = view.findViewById(R.id.result_detail);
        if (right) {
            detail.setText(breakdown(round));
            detail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        } else {
            // A wrong pick names the guess, a timeout has none
            detail.setText(round.guess != null ? getString(R.string.you_said, round.guess) : getString(R.string.times_up));
            detail.setTextColor(textColor);
        }
    }

    /** "500 base · -50 hint · -30 time", leaving out the parts that are zero. */
    private String breakdown(SampleData.Round round) {
        StringBuilder text = new StringBuilder(getString(R.string.breakdown_base, BASE_POINTS));
        String separator = getString(R.string.breakdown_separator);
        if (round.hintCost > 0) {
            text.append(separator).append(getString(R.string.breakdown_hint, round.hintCost));
        }
        if (round.timePenalty > 0) {
            text.append(separator).append(getString(R.string.breakdown_time, round.timePenalty));
        }
        return text.toString();
    }

    private void bindStation(View view, SampleData.Round round) {
        ((TextView) view.findViewById(R.id.station_logo)).setText(initials(round.stationName));
        ((TextView) view.findViewById(R.id.station_name)).setText(round.stationName);
        ((TextView) view.findViewById(R.id.station_place)).setText(getString(R.string.station_place, round.city, round.country));

        LinearLayout tags = view.findViewById(R.id.station_tags);
        tags.setVisibility(round.tags.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < Math.min(MAX_TAGS, round.tags.size()); i++) {
            TextView tag = (TextView) inflater.inflate(R.layout.item_tag, tags, false);
            tag.setText(round.tags.get(i));
            tags.addView(tag);
        }

        TextView status = view.findViewById(R.id.station_status);
        MaterialButton play = view.findViewById(R.id.station_play_button);
        bindPlayback(status, play, round.stationName);
        play.setOnClickListener(v -> {
            playing = !playing;
            bindPlayback(status, play, round.stationName);
        });
    }

    private void bindPlayback(TextView status, MaterialButton play, String stationName) {
        status.setText(playing ? R.string.status_live : R.string.status_paused);
        play.setIconResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        play.setContentDescription(getString(playing ? R.string.pause_station : R.string.play_station, stationName));
    }

    private void bindSave(MaterialButton save) {
        save.setText(saved ? R.string.action_saved : R.string.action_save);
        save.setIconResource(saved ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        save.setIconTint(ContextCompat.getColorStateList(requireContext(),
                saved ? R.color.accent : R.color.button_outlined_content));
    }

    /** Two letters for the logo tile when there is no favicon: "Rádio Gaúcha" gives "RG". */
    static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        String letters = words.length >= 2
                ? words[0].substring(0, 1) + words[1].substring(0, 1)
                : name.trim().substring(0, Math.min(2, name.trim().length()));
        return letters.toUpperCase(Locale.getDefault());
    }
}
