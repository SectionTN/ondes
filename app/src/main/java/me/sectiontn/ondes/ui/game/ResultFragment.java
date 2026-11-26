package me.sectiontn.ondes.ui.game;

import android.content.Context;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.game.GameRound;
import me.sectiontn.ondes.game.ScoreCalculator;
import me.sectiontn.ondes.player.RadioPlayer;
import me.sectiontn.ondes.ui.common.EqualizerView;
import me.sectiontn.ondes.ui.common.Motion;

/** After an answer: right or wrong, the points, and the station that was playing. */
public class ResultFragment extends Fragment {

    private static final int MAX_TAGS = 3;
    // The waves start once the outcome has risen into place: while it fades in, Android would cut them at its edges
    private static final long WAVE_DELAY_MS = 300;
    private static final long WAVE_GAP_MS = 220;
    private static final long SHAKE_DELAY_MS = 200;

    private GameViewModel game;
    private boolean saved = false;
    // Set by the first tap on Next, so a fast double tap cannot skip a round
    private boolean leaving = false;

    public ResultFragment() {
        super(R.layout.fragment_result);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        game = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
        GameRound round = game.current();
        if (round == null || !round.finished) {
            return;
        }

        View topBar = view.findViewById(R.id.top_bar);
        GameTopBar.bind(topBar, game.roundNumber(), GameViewModel.ROUND_COUNT, game.score(), game.outcomes(), false);
        topBar.findViewById(R.id.close_button).setOnClickListener(v -> ((GameActivity) requireActivity()).confirmQuit());

        // A screen brought back by Android (savedInstanceState set) shows as it was, without the entrance
        boolean animate = savedInstanceState == null;
        if (animate) {
            GameTopBar.showOutcome(topBar, game.roundNumber(), round.points, game.score(), round.outcome);
            Motion.riseInChildren(view.findViewById(R.id.result_content), 1);
        }
        bindOutcome(view, round, animate);
        Station station = round.station();
        if (station != null) {
            bindStation(view, station, game.catalog().nameOf(round.answer.code));
        }

        MaterialButton next = view.findViewById(R.id.next_button);
        next.setText(game.roundNumber() < GameViewModel.ROUND_COUNT ? R.string.action_next_round : R.string.action_see_results);
        next.setOnClickListener(v -> {
            if (leaving) {
                return;
            }
            leaving = true;
            game.next();
        });

        MaterialButton save = view.findViewById(R.id.save_button);
        if (station == null) {
            save.setVisibility(View.GONE);
            return;
        }
        game.favoriteIds().observe(getViewLifecycleOwner(), ids -> {
            saved = ids.contains(station.uuid);
            bindSave(save, station);
        });
        save.setOnClickListener(v -> game.setSaved(station, !saved));
    }

    private void bindOutcome(View view, GameRound round, boolean animate) {
        boolean right = round.isCorrect();
        int textColor = ContextCompat.getColor(requireContext(), right ? R.color.correct : R.color.wrong_text);

        ImageView circle = view.findViewById(R.id.result_circle);
        circle.setBackgroundResource(right ? R.drawable.bg_result_circle : R.drawable.bg_result_circle_wrong);
        circle.setImageResource(right ? R.drawable.ic_check : R.drawable.ic_close);
        ImageViewCompat.setImageTintList(circle, ContextCompat.getColorStateList(requireContext(),
                right ? R.color.correct : R.color.wrong));
        circle.setContentDescription(getString(right ? R.string.outcome_correct : R.string.outcome_wrong));
        if (animate) {
            Motion.pop(circle, Motion.POP_FROM_SMALL, 0);
            if (right) {
                // Two radio waves leave the circle once it has landed, one after the other
                Motion.wave(view.findViewById(R.id.result_wave_1), WAVE_DELAY_MS);
                Motion.wave(view.findViewById(R.id.result_wave_2), WAVE_DELAY_MS + WAVE_GAP_MS);
            } else {
                Motion.shake(circle, SHAKE_DELAY_MS);
            }
        }

        // The country to find, whatever the player answered
        TextView country = view.findViewById(R.id.result_country);
        country.setText(game.catalog().nameOf(round.answer));
        ViewCompat.setAccessibilityHeading(country, true);

        TextView points = view.findViewById(R.id.result_points);
        points.setTextColor(textColor);
        if (right && animate) {
            // The label reads its string from the context: getString() would crash once this screen is gone
            Context context = requireContext();
            Motion.count(points, 0, round.points, Motion.COUNT_MS, value -> context.getString(R.string.points_signed, value));
        } else {
            points.setText(right ? getString(R.string.points_signed, round.points) : getString(R.string.points_zero));
        }

        TextView detail = view.findViewById(R.id.result_detail);
        if (right) {
            detail.setText(breakdown(round));
            detail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        } else {
            // A wrong pick names the guess, a timeout has none
            detail.setText(round.guess != null ? getString(R.string.you_said, game.catalog().nameOf(round.guess)) : getString(R.string.times_up));
            detail.setTextColor(textColor);
        }
    }

    /** "500 base · -50 hint · -30 time", leaving out the parts that are zero. */
    private String breakdown(GameRound round) {
        StringBuilder text = new StringBuilder(getString(R.string.breakdown_base, ScoreCalculator.BASE_POINTS));
        String separator = getString(R.string.breakdown_separator);
        int hintCost = ScoreCalculator.hintCost(round.hintsOpen);
        int timePenalty = ScoreCalculator.timePenalty(round.playedMs);
        if (hintCost > 0) {
            text.append(separator).append(getString(R.string.breakdown_hint, hintCost));
        }
        if (timePenalty > 0) {
            text.append(separator).append(getString(R.string.breakdown_time, timePenalty));
        }
        return text.toString();
    }

    private void bindStation(View view, Station station, String countryName) {
        ((TextView) view.findViewById(R.id.station_logo)).setText(initials(station.name));
        ImageView favicon = view.findViewById(R.id.station_favicon);
        float radius = getResources().getDimension(R.dimen.radius_station_logo);
        favicon.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        favicon.setClipToOutline(true);
        if (!station.favicon.isEmpty()) {
            // A favicon that fails to load leaves the initials visible underneath
            Glide.with(this).load(station.favicon).into(favicon);
        }

        ((TextView) view.findViewById(R.id.station_name)).setText(station.name);
        TextView place = view.findViewById(R.id.station_place);
        place.setText(station.state.isEmpty() ? countryName : getString(R.string.station_place, station.state, countryName));

        ViewGroup tags = view.findViewById(R.id.station_tags);
        tags.removeAllViews();
        tags.setVisibility(station.tags.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < Math.min(MAX_TAGS, station.tags.size()); i++) {
            TextView tag = (TextView) inflater.inflate(R.layout.item_tag, tags, false);
            tag.setText(station.tags.get(i));
            tags.addView(tag);
        }

        TextView status = view.findViewById(R.id.station_status);
        MaterialButton play = view.findViewById(R.id.station_play_button);
        EqualizerView equalizer = view.findViewById(R.id.station_equalizer);
        game.playerState().observe(getViewLifecycleOwner(), state -> {
            boolean live = state == RadioPlayer.State.PLAYING || state == RadioPlayer.State.LOADING;
            status.setText(live ? R.string.status_live : R.string.status_paused);
            play.setIconResource(live ? R.drawable.ic_pause : R.drawable.ic_play);
            play.setContentDescription(getString(live ? R.string.pause_station : R.string.play_station, station.name));
            equalizer.setPlaying(state == RadioPlayer.State.PLAYING);
        });
        play.setOnClickListener(v -> game.togglePlayback());
    }

    private void bindSave(MaterialButton save, Station station) {
        save.setText(saved ? R.string.action_saved : R.string.action_save);
        save.setContentDescription(getString(saved ? R.string.remove_from_favorites : R.string.save_to_favorites, station.name));
    }

    /** Two letters for the logo tile when there is no favicon: "Rádio Gaúcha" gives "RG". */
    static String initials(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String[] words = trimmed.split("\\s+");
        String letters = words.length >= 2
                ? words[0].substring(0, 1) + words[1].substring(0, 1)
                : trimmed.substring(0, Math.min(2, trimmed.length()));
        return letters.toUpperCase(Locale.getDefault());
    }
}
