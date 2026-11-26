package me.sectiontn.ondes.ui.home;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import me.sectiontn.ondes.MainActivity;
import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.db.DailyResultEntity;
import me.sectiontn.ondes.data.repo.ProgressRepository;
import me.sectiontn.ondes.game.DailySeed;
import me.sectiontn.ondes.game.Outcome;
import me.sectiontn.ondes.ui.common.UiFormat;
import me.sectiontn.ondes.ui.game.GameActivity;
import me.sectiontn.ondes.ui.game.OutcomeStyle;

/** The Play tab: today's daily challenge, free play and a shortcut to Explore. Works offline. */
public class HomeFragment extends Fragment {

    private static final int[] SLOT_IDS = {R.id.slot_1, R.id.slot_2, R.id.slot_3, R.id.slot_4, R.id.slot_5};
    private static final long CLOCK_REFRESH_MS = 30_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshClock = new Runnable() {
        @Override
        public void run() {
            bindTimeLeft();
            handler.postDelayed(this, CLOCK_REFRESH_MS);
        }
    };

    private TextView timeLeft;
    @Nullable
    private DailyResultEntity todayResult;
    private int today;
    @Nullable
    private LiveData<DailyResultEntity> todayLive;
    // True from the tap that opens a game until Home is back, so a double tap opens one game, not two
    private boolean launching = false;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ProgressRepository progress = ProgressRepository.get(requireContext());

        View streakPill = view.findViewById(R.id.streak_pill);
        TextView streak = view.findViewById(R.id.streak_text);
        progress.streak().observe(getViewLifecycleOwner(), days -> {
            // No streak, no pill
            streakPill.setVisibility(days > 0 ? View.VISIBLE : View.GONE);
            streak.setText(getResources().getQuantityString(R.plurals.streak, days, days));
        });

        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.daily_title), true);
        timeLeft = view.findViewById(R.id.time_left);
        bindToday(view);

        view.findViewById(R.id.play_button).setOnClickListener(v -> startGame(GameActivity.MODE_DAILY));
        view.findViewById(R.id.results_button).setOnClickListener(v ->
                launch(GameActivity.newResultsIntent(requireContext(), today)));
        view.findViewById(R.id.free_play_tile).setOnClickListener(v -> startGame(GameActivity.MODE_FREE_PLAY));
        view.findViewById(R.id.explore_tile).setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_explore));
    }

    @Override
    public void onResume() {
        super.onResume();
        launching = false;
        // Past midnight the tab shows the new day's daily
        View view = getView();
        if (view != null && DailySeed.today() != today) {
            bindToday(view);
        }
        handler.post(refreshClock);
    }

    /** The date, the daily number and its saved result, for the current day. */
    private void bindToday(View view) {
        today = DailySeed.today();
        TextView todayLabel = view.findViewById(R.id.today_label);
        todayLabel.setText(UiFormat.date(requireContext(), "EEE d MMM", new Date()));
        TextView title = view.findViewById(R.id.daily_title);
        title.setText(getString(R.string.daily_title, today));

        if (todayLive != null) {
            todayLive.removeObservers(getViewLifecycleOwner());
        }
        todayLive = ProgressRepository.get(requireContext()).dailyResult(today);
        todayLive.observe(getViewLifecycleOwner(), result -> {
            todayResult = result;
            bindDaily(view, result);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshClock);
    }

    /** Empty numbered slots and Play, or the day's colored squares and See results once it is done. */
    private void bindDaily(View view, @Nullable DailyResultEntity result) {
        boolean done = result != null;
        NumberFormat numbers = UiFormat.numbers(requireContext());
        StringBuilder outcomes = new StringBuilder();
        for (int i = 0; i < SLOT_IDS.length; i++) {
            TextView slot = view.findViewById(SLOT_IDS[i]);
            if (done && i < result.squares.length()) {
                Outcome outcome = outcomeOf(result.squares.charAt(i));
                slot.setText(null);
                slot.setBackground(outcomeSquare(outcome));
                if (outcomes.length() > 0) {
                    outcomes.append(", ");
                }
                outcomes.append(getString(OutcomeStyle.description(outcome)));
            } else {
                slot.setText(numbers.format(i + 1));
                slot.setBackgroundResource(R.drawable.bg_dashed_rounded);
            }
        }
        // Once played, TalkBack reads the row as one line: "Correct, Wrong, Correct with a hint..."
        View slots = view.findViewById(R.id.daily_slots);
        slots.setContentDescription(done ? outcomes : null);
        slots.setImportantForAccessibility(done ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        view.findViewById(R.id.play_button).setVisibility(done ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.results_button).setVisibility(done ? View.VISIBLE : View.GONE);
        bindTimeLeft();
    }

    private void bindTimeLeft() {
        if (timeLeft == null) {
            return;
        }
        if (todayResult != null) {
            timeLeft.setText(getString(R.string.daily_done, UiFormat.numbers(requireContext()).format(todayResult.score)));
            return;
        }
        long left = DailySeed.millisUntilMidnight(Calendar.getInstance());
        timeLeft.setText(getString(R.string.time_left, (int) TimeUnit.MILLISECONDS.toHours(left),
                (int) (TimeUnit.MILLISECONDS.toMinutes(left) % 60)));
    }

    // A filled square in the outcome color with its icon in the middle
    private Drawable outcomeSquare(Outcome outcome) {
        Drawable square = DrawableCompat.wrap(ContextCompat.getDrawable(requireContext(), R.drawable.bg_outcome_square).mutate());
        DrawableCompat.setTint(square, ContextCompat.getColor(requireContext(), OutcomeStyle.color(outcome)));
        Drawable icon = DrawableCompat.wrap(ContextCompat.getDrawable(requireContext(), OutcomeStyle.icon(outcome)).mutate());
        DrawableCompat.setTint(icon, ContextCompat.getColor(requireContext(), R.color.on_accent));
        LayerDrawable layers = new LayerDrawable(new Drawable[]{square, icon});
        int size = getResources().getDimensionPixelSize(R.dimen.daily_slot_icon);
        layers.setLayerSize(1, size, size);
        layers.setLayerGravity(1, Gravity.CENTER);
        return layers;
    }

    private static Outcome outcomeOf(char letter) {
        switch (letter) {
            case 'G':
                return Outcome.PERFECT;
            case 'Y':
                return Outcome.HINT;
            default:
                return Outcome.WRONG;
        }
    }

    private void startGame(String mode) {
        launch(GameActivity.newIntent(requireContext(), mode));
    }

    private void launch(Intent intent) {
        if (launching) {
            return;
        }
        launching = true;
        startActivity(intent);
    }
}
