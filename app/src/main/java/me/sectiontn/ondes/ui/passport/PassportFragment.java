package me.sectiontn.ondes.ui.passport;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.countries.Country;
import me.sectiontn.ondes.data.countries.CountryCatalog;
import me.sectiontn.ondes.data.db.StampEntity;
import me.sectiontn.ondes.data.repo.ProgressRepository;
import me.sectiontn.ondes.ui.common.Motion;
import me.sectiontn.ondes.ui.common.UiFormat;

/** The Passport tab: a stamp for every country guessed right at least once. Works offline. */
public class PassportFragment extends Fragment {

    private static final int GRID_COLUMNS = 4;
    private static final String PREFS = "passport";
    private static final String KEY_SEEN_AT = "seen_at";

    // Stamps with a later date than this are new on this visit. Read from the preferences, then set past every date
    private long newSince;
    private CountryCatalog catalog;
    private RecyclerView grid;
    private TextView emptyText;
    private ChipGroup chips;
    private List<StampEntity> stamps = new ArrayList<>();

    public PassportFragment() {
        super(R.layout.fragment_passport);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        catalog = CountryCatalog.get(requireContext());
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.passport_title), true);

        TextView total = view.findViewById(R.id.stamp_total);
        total.setText(getString(R.string.passport_total, catalog.size()));
        LinearProgressIndicator progress = view.findViewById(R.id.stamp_progress);
        progress.setMax(catalog.size());

        grid = view.findViewById(R.id.stamp_grid);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMNS));
        emptyText = view.findViewById(R.id.stamps_empty);

        chips = view.findViewById(R.id.continent_chips);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            showStamps();
            Motion.fadeIn(grid);
        });

        // Stamps earned after the last visit are new: they land on the page, and the count climbs to include them
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        newSince = prefs.getLong(KEY_SEEN_AT, 0);
        ProgressRepository.get(requireContext()).stamps().observe(getViewLifecycleOwner(), saved -> {
            stamps = saved;
            int newStamps = 0;
            for (StampEntity stamp : stamps) {
                if (stamp.firstDate > newSince) {
                    newStamps++;
                }
            }
            bindCounts(view, newStamps);
            showStamps();
            // Shown once: from now on every stamp on the page counts as seen
            newSince = Long.MAX_VALUE;
            prefs.edit().putLong(KEY_SEEN_AT, System.currentTimeMillis()).apply();
        });
    }

    private void bindCounts(View view, int newStamps) {
        NumberFormat numbers = UiFormat.numbers(requireContext());
        TextView total = view.findViewById(R.id.stamp_count);
        total.setText(numbers.format(stamps.size()));
        LinearProgressIndicator progress = view.findViewById(R.id.stamp_progress);
        if (newStamps > 0) {
            Motion.count(total, stamps.size() - newStamps, stamps.size(), Motion.COUNT_MS, numbers::format);
            progress.setProgress(stamps.size() - newStamps);
            progress.setProgressCompat(stamps.size(), true);
        } else {
            progress.setProgress(stamps.size());
        }

        LinearLayout counts = view.findViewById(R.id.continent_counts);
        counts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = getResources().getDimensionPixelSize(R.dimen.gap_continent_counts);
        for (int i = 0; i < CountryCatalog.CONTINENTS.length; i++) {
            String continent = CountryCatalog.CONTINENTS[i];
            int count = 0;
            for (StampEntity stamp : stamps) {
                Country country = catalog.find(stamp.countryCode);
                if (country != null && continent.equals(country.continent)) {
                    count++;
                }
            }
            View column = inflater.inflate(R.layout.item_continent_count, counts, false);
            ((TextView) column.findViewById(R.id.continent_count)).setText(numbers.format(count));
            ((TextView) column.findViewById(R.id.continent_name)).setText(catalog.continentName(continent));
            if (i > 0) {
                ((LinearLayout.LayoutParams) column.getLayoutParams()).setMarginStart(gap);
            }
            counts.addView(column);
        }
    }

    private void showStamps() {
        String continent = continentFor(chips.getCheckedChipId());
        List<StampEntity> shown = new ArrayList<>();
        for (StampEntity stamp : stamps) {
            Country country = catalog.find(stamp.countryCode);
            if (continent == null || country != null && continent.equals(country.continent)) {
                shown.add(stamp);
            }
        }
        // No stamp yet: one row of locked cells. A filtered view fills its last row with locked cells
        int locked;
        if (stamps.isEmpty()) {
            locked = GRID_COLUMNS;
        } else if (continent == null) {
            locked = 0;
        } else {
            locked = GRID_COLUMNS - shown.size() % GRID_COLUMNS;
        }
        emptyText.setVisibility(stamps.isEmpty() ? View.VISIBLE : View.GONE);
        grid.setAdapter(new StampAdapter(shown, locked, catalog, newSince));
    }

    /** The continent a chip filters on, or null for All. */
    @Nullable
    private static String continentFor(@IdRes int chipId) {
        if (chipId == R.id.continent_africa) return "Africa";
        if (chipId == R.id.continent_americas) return "Americas";
        if (chipId == R.id.continent_asia) return "Asia";
        if (chipId == R.id.continent_europe) return "Europe";
        if (chipId == R.id.continent_oceania) return "Oceania";
        return null;
    }
}
