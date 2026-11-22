package me.sectiontn.ondes.ui.explore;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.sample.SampleData;

/** The Explore tab: shake or tap Teleport to jump to a random live station. */
public class ExploreFragment extends Fragment {

    private boolean playing = true;

    public ExploreFragment() {
        super(R.layout.fragment_explore);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.explore_title), true);

        RecyclerView recent = view.findViewById(R.id.recent_list);
        recent.setLayoutManager(new LinearLayoutManager(requireContext()));
        recent.setAdapter(new StationAdapter(SampleData.recentStations()));

        SampleData.Station station = SampleData.NOW_PLAYING;
        TextView name = view.findViewById(R.id.mini_player_name);
        name.setText(station.name);
        TextView place = view.findViewById(R.id.mini_player_place);
        place.setText(station.place);

        MaterialButton playButton = view.findViewById(R.id.mini_player_button);
        bindPlayButton(playButton, station);
        playButton.setOnClickListener(v -> {
            playing = !playing;
            bindPlayButton(playButton, station);
        });
    }

    private void bindPlayButton(MaterialButton button, SampleData.Station station) {
        button.setIconResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        int label = playing ? R.string.pause_station : R.string.play_station;
        button.setContentDescription(getString(label, station.name));
    }
}
