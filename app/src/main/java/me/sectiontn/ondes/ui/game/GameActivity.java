package me.sectiontn.ondes.ui.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.ui.common.SystemBars;

/** Runs one game of five rounds, without the bottom navigation bar. */
public class GameActivity extends AppCompatActivity {

    private static final String EXTRA_MODE = "me.sectiontn.ondes.extra.MODE";
    public static final String MODE_DAILY = "daily";
    public static final String MODE_FREE_PLAY = "free_play";

    public static Intent newIntent(Context context, String mode) {
        return new Intent(context, GameActivity.class).putExtra(EXTRA_MODE, mode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.enableEdgeToEdge(this);
        setContentView(R.layout.activity_game);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.game_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        boolean daily = MODE_DAILY.equals(getIntent().getStringExtra(EXTRA_MODE));
        TextView modeLabel = findViewById(R.id.mode_label);
        modeLabel.setText(daily ? R.string.mode_daily : R.string.mode_free_play);

        findViewById(R.id.close_button).setOnClickListener(v -> finish());
    }
}
