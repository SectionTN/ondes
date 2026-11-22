package me.sectiontn.ondes;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import me.sectiontn.ondes.ui.explore.ExploreFragment;
import me.sectiontn.ondes.ui.home.HomeFragment;
import me.sectiontn.ondes.ui.passport.PassportFragment;

/** Hosts the three tabs (Play, Explore, Passport) above the bottom navigation bar. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Light status bar icons on every phone, since the app is always dark
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            // The bottom nav pads itself for the navigation bar, so only the bottom inset goes on
            return insets.inset(bars.left, bars.top, bars.right, 0);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        // Tapping the open tab again keeps it as it is
        bottomNav.setOnItemReselectedListener(item -> { });

        // After a rotation or process death, the FragmentManager restores the open tab itself
        if (savedInstanceState == null) {
            showTab(R.id.nav_play);
        }
    }

    private void showTab(@IdRes int itemId) {
        Fragment tab;
        if (itemId == R.id.nav_explore) {
            tab = new ExploreFragment();
        } else if (itemId == R.id.nav_passport) {
            tab = new PassportFragment();
        } else {
            tab = new HomeFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.tab_container, tab)
                .commit();
    }
}
