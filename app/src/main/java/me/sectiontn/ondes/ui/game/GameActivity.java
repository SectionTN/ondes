package me.sectiontn.ondes.ui.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.sample.SampleData;
import me.sectiontn.ondes.ui.common.SystemBars;

/** Runs one game of five rounds, without the bottom navigation bar. */
public class GameActivity extends AppCompatActivity {

    private static final String EXTRA_MODE = "me.sectiontn.ondes.extra.MODE";
    private static final String STATE_ROUND = "round";
    public static final String MODE_DAILY = "daily";
    public static final String MODE_FREE_PLAY = "free_play";

    // The sample game walks through the five sample rounds
    private int roundNumber = 1;

    public static Intent newIntent(Context context, String mode) {
        return new Intent(context, GameActivity.class).putExtra(EXTRA_MODE, mode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.enableEdgeToEdge(this);
        setContentView(R.layout.activity_game);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.game_container), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        if (savedInstanceState == null) {
            show(RoundFragment.newInstance(roundNumber));
        } else {
            roundNumber = savedInstanceState.getInt(STATE_ROUND);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ROUND, roundNumber);
    }

    /** Called when the player picks an answer. */
    void showResult() {
        show(ResultFragment.newInstance(roundNumber));
    }

    /** Called by Next round on the Result screen, and by See results after the last round. */
    void showNextRound() {
        if (roundNumber < SampleData.ROUND_COUNT) {
            roundNumber++;
            show(RoundFragment.newInstance(roundNumber));
        } else {
            finish();
        }
    }

    // Swaps the screen without a back stack: Back always leaves the game
    private void show(Fragment screen) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.game_container, screen)
                .commit();
    }
}
