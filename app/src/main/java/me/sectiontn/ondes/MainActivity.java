package me.sectiontn.ondes;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import me.sectiontn.ondes.ui.common.SystemBars;
import me.sectiontn.ondes.ui.explore.ExploreFragment;
import me.sectiontn.ondes.ui.home.HomeFragment;
import me.sectiontn.ondes.ui.passport.PassportFragment;

/** Hosts the three tabs (Play, Explore, Passport) above the bottom navigation bar. */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    // Back on Explore or Passport returns to Play first, and Back on Play leaves the app
    private final OnBackPressedCallback backToPlay = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            bottomNav.setSelectedItemId(R.id.nav_play);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.enableEdgeToEdge(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            // The bottom nav pads itself for the navigation bar, so only the bottom inset goes on
            return insets.inset(bars.left, bars.top, bars.right, 0);
        });

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId(), true);
            return true;
        });
        // Tapping the open tab again keeps it as it is
        bottomNav.setOnItemReselectedListener(item -> { });
        getOnBackPressedDispatcher().addCallback(this, backToPlay);

        // After a rotation or process death, the FragmentManager restores the open tab itself
        if (savedInstanceState == null) {
            showTab(R.id.nav_play, false);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // The bottom nav gets its selected tab back here, after onCreate
        backToPlay.setEnabled(bottomNav.getSelectedItemId() != R.id.nav_play);
    }

    /** Opens a tab as if the user tapped it, like the Explore tile on Home does. */
    public void selectTab(@IdRes int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    private void showTab(@IdRes int itemId, boolean animate) {
        backToPlay.setEnabled(itemId != R.id.nav_play);
        Fragment tab;
        if (itemId == R.id.nav_explore) {
            tab = new ExploreFragment();
        } else if (itemId == R.id.nav_passport) {
            tab = new PassportFragment();
        } else {
            tab = new HomeFragment();
        }
        FragmentTransaction swap = getSupportFragmentManager().beginTransaction().setReorderingAllowed(true);
        if (animate) {
            // A tapped tab fades in as it rises. The first tab at launch is just there
            swap.setCustomAnimations(R.animator.tab_enter, R.animator.tab_exit);
        }
        swap.replace(R.id.tab_container, tab).commit();
    }
}
