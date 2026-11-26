package me.sectiontn.ondes.ui.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.game.GameRound;
import me.sectiontn.ondes.ui.common.Motion;
import me.sectiontn.ondes.ui.common.SystemBars;

/** Runs one game of five rounds, without the bottom navigation bar. */
public class GameActivity extends AppCompatActivity {

    private static final String EXTRA_MODE = "me.sectiontn.ondes.extra.MODE";
    private static final String EXTRA_DAY = "me.sectiontn.ondes.extra.DAY";
    public static final String MODE_DAILY = "daily";
    public static final String MODE_FREE_PLAY = "free_play";
    /** Opens the Summary of a daily already played. */
    public static final String MODE_DAILY_RESULTS = "daily_results";

    private GameViewModel game;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static Intent newIntent(Context context, String mode) {
        return new Intent(context, GameActivity.class).putExtra(EXTRA_MODE, mode);
    }

    public static Intent newResultsIntent(Context context, int dayNumber) {
        return newIntent(context, MODE_DAILY_RESULTS).putExtra(EXTRA_DAY, dayNumber);
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

        game = new ViewModelProvider(this).get(GameViewModel.class);
        if (!game.isStarted()) {
            // After process death Android brings back the old screen but not the game: start over
            Fragment restored = getSupportFragmentManager().findFragmentById(R.id.game_container);
            if (restored != null) {
                getSupportFragmentManager().beginTransaction().remove(restored).commitNow();
            }
            String mode = getIntent().getStringExtra(EXTRA_MODE);
            if (MODE_DAILY_RESULTS.equals(mode)) {
                game.showSavedDaily(getIntent().getIntExtra(EXTRA_DAY, 0));
            } else {
                game.start(MODE_DAILY.equals(mode));
            }
        }

        game.screen().observe(this, this::show);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmQuit();
            }
        });
    }

    /** The close button and Back: asks first while the game is still going. */
    void confirmQuit() {
        if (!game.isInProgress()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quit_title)
                .setMessage(R.string.quit_message)
                .setNegativeButton(R.string.quit_cancel, null)
                .setPositiveButton(R.string.quit_confirm, (dialog, which) -> finish())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void show(GameViewModel.Screen screen) {
        Fragment next;
        switch (screen) {
            case RESULT:
                next = new ResultFragment();
                break;
            case SUMMARY:
                next = new SummaryFragment();
                break;
            default:
                next = new RoundFragment();
                break;
        }
        handler.removeCallbacksAndMessages(null);
        // A picked answer first shows green or red on its button, then the Result opens. The score is already final
        GameRound round = game.current();
        Fragment shown = getSupportFragmentManager().findFragmentById(R.id.game_container);
        boolean reveal = screen == GameViewModel.Screen.RESULT && shown instanceof RoundFragment
                && round != null && round.guess != null && Motion.enabled(this);
        if (reveal) {
            handler.postDelayed(() -> swap(next), RoundFragment.REVEAL_MS);
        } else {
            swap(next);
        }
    }

    // Swaps the screen without a back stack: Back always leaves the game
    private void swap(Fragment next) {
        // The delayed swap can run after the app went to the background, where a plain commit() throws
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.game_container, next)
                .commitAllowingStateLoss();
    }
}
