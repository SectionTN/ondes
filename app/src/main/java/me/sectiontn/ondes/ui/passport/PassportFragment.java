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
import me.sectiontn.ondes.sample.SampleData;

/** The Passport tab: a stamp for every country guessed right at least once. */
public class PassportFragment extends Fragment {

    private static final int GRID_COLUMNS = 4;

    private RecyclerView grid;

    public PassportFragment() {
        super(R.layout.fragment_passport);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.passport_title), true);
        NumberFormat numbers = NumberFormat.getIntegerInstance();

        TextView count = view.findViewById(R.id.stamp_count);
        count.setText(numbers.format(SampleData.STAMP_COUNT));
        TextView total = view.findViewById(R.id.stamp_total);
        total.setText(getString(R.string.passport_total, SampleData.TOTAL_COUNTRIES));

        LinearProgressIndicator progress = view.findViewById(R.id.stamp_progress);
        progress.setMax(SampleData.TOTAL_COUNTRIES);
        progress.setProgress(SampleData.STAMP_COUNT);

        LinearLayout counts = view.findViewById(R.id.continent_counts);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = getResources().getDimensionPixelSize(R.dimen.gap_continent_counts);
        for (int i = 0; i < SampleData.CONTINENTS.length; i++) {
            View column = inflater.inflate(R.layout.item_continent_count, counts, false);
            ((TextView) column.findViewById(R.id.continent_count)).setText(numbers.format(SampleData.CONTINENT_COUNTS[i]));
            ((TextView) column.findViewById(R.id.continent_name)).setText(SampleData.CONTINENTS[i]);
            if (i > 0) {
                ((LinearLayout.LayoutParams) column.getLayoutParams()).setMarginStart(gap);
            }
            counts.addView(column);
        }

        grid = view.findViewById(R.id.stamp_grid);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMNS));

        ChipGroup chips = view.findViewById(R.id.continent_chips);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> showStamps(checkedIds.get(0)));
        showStamps(chips.getCheckedChipId());
    }

    private void showStamps(@IdRes int chipId) {
        String continent = continentFor(chipId);
        List<SampleData.Stamp> shown = new ArrayList<>();
        for (SampleData.Stamp stamp : SampleData.stamps()) {
            if (continent == null || continent.equals(stamp.continent)) {
                shown.add(stamp);
            }
        }
        // A filtered view ends with locked cells up to the end of its last row, or one full row
        int locked = continent == null ? 0 : GRID_COLUMNS - shown.size() % GRID_COLUMNS;
        grid.setAdapter(new StampAdapter(shown, locked));
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
