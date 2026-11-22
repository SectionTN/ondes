package me.sectiontn.ondes.ui.home;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import java.text.NumberFormat;

import me.sectiontn.ondes.MainActivity;
import me.sectiontn.ondes.R;
import me.sectiontn.ondes.sample.SampleData;
import me.sectiontn.ondes.ui.game.GameActivity;

/** The Play tab: today's daily challenge, free play and a shortcut to Explore. */
public class HomeFragment extends Fragment {

    private static final int[] SLOT_IDS = {R.id.slot_1, R.id.slot_2, R.id.slot_3, R.id.slot_4, R.id.slot_5};

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView streak = view.findViewById(R.id.streak_text);
        streak.setText(getString(R.string.streak, SampleData.STREAK_DAYS));

        TextView today = view.findViewById(R.id.today_label);
        today.setText(SampleData.TODAY_LABEL);

        TextView title = view.findViewById(R.id.daily_title);
        title.setText(getString(R.string.daily_title, SampleData.DAILY_NUMBER));
        ViewCompat.setAccessibilityHeading(title, true);

        TextView timeLeft = view.findViewById(R.id.time_left);
        timeLeft.setText(getString(R.string.time_left, SampleData.HOURS_LEFT, SampleData.MINUTES_LEFT));

        NumberFormat numbers = NumberFormat.getIntegerInstance();
        for (int i = 0; i < SLOT_IDS.length; i++) {
            TextView slot = view.findViewById(SLOT_IDS[i]);
            slot.setText(numbers.format(i + 1));
        }

        view.findViewById(R.id.play_button).setOnClickListener(v -> startGame(GameActivity.MODE_DAILY));
        view.findViewById(R.id.free_play_tile).setOnClickListener(v -> startGame(GameActivity.MODE_FREE_PLAY));
        view.findViewById(R.id.explore_tile).setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_explore));
    }

    private void startGame(String mode) {
        startActivity(GameActivity.newIntent(requireContext(), mode));
    }
}
