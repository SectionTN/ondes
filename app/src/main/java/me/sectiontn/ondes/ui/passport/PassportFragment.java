package me.sectiontn.ondes.ui.passport;

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

/** The Passport tab: a stamp for every country guessed right at least once. Works offline. */
public class PassportFragment extends Fragment {

    private static final int GRID_COLUMNS = 4;

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
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> showStamps());

        ProgressRepository.get(requireContext()).stamps().observe(getViewLifecycleOwner(), saved -> {
            stamps = saved;
            bindCounts(view);
            showStamps();
        });
    }

    private void bindCounts(View view) {
        NumberFormat numbers = NumberFormat.getIntegerInstance();
        ((TextView) view.findViewById(R.id.stamp_count)).setText(numbers.format(stamps.size()));
        ((LinearProgressIndicator) view.findViewById(R.id.stamp_progress)).setProgress(stamps.size());

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
            ((TextView) column.findViewById(R.id.continent_name)).setText(continent);
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
        grid.setAdapter(new StampAdapter(shown, locked, catalog));
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
