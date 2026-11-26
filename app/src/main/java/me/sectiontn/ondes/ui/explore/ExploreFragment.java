package me.sectiontn.ondes.ui.explore;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.google.android.material.snackbar.Snackbar;


import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.player.RadioPlayer;
import me.sectiontn.ondes.ui.common.EqualizerView;
import me.sectiontn.ondes.ui.common.ShakeDetector;

/** The Explore tab: shake or tap Teleport to jump to a random live station. */
public class ExploreFragment extends Fragment implements StationAdapter.Listener {

    private static final long MINI_PLAYER_SLIDE_MS = 200;

    private ExploreViewModel explore;
    private ShakeDetector shakeDetector;
    @Nullable
    private SensorManager sensorManager;
    @Nullable
    private Sensor accelerometer;
    private MaterialButton teleportButton;
    private View miniPlayer;

    public ExploreFragment() {
        super(R.layout.fragment_explore);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Scoped to the activity, so the mini player keeps its station when the user switches tabs
        explore = new ViewModelProvider(requireActivity()).get(ExploreViewModel.class);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.explore_title), true);

        teleportButton = view.findViewById(R.id.teleport_button);
        teleportButton.setOnClickListener(v -> explore.teleport());
        explore.teleporting().observe(getViewLifecycleOwner(), this::bindTeleporting);

        ChipGroup genres = view.findViewById(R.id.genre_chips);
        genres.setOnCheckedStateChangeListener((group, checkedIds) -> explore.setGenre(tagFor(checkedIds.get(0))));
        explore.setGenre(tagFor(genres.getCheckedChipId()));

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        shakeDetector = new ShakeDetector(() -> explore.teleport());

        bindLists(view);
        bindMiniPlayer(view);

        explore.message().observe(getViewLifecycleOwner(), text -> {
            if (text == null) {
                return;
            }
            Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG);
            if (miniPlayer.getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(miniPlayer);
            }
            snackbar.show();
            explore.messageShown();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // The shake listens only while Explore is on screen
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Another tab replaces this one: the sound stops, since only Explore has a button to stop it
        if (!requireActivity().isChangingConfigurations()) {
            explore.leave();
        }
    }

    private void bindTeleporting(boolean busy) {
        teleportButton.setEnabled(!busy);
        if (busy) {
            CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(requireContext(), null, 0,
                    com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
            teleportButton.setIcon(IndeterminateDrawable.createCircularDrawable(requireContext(), spec));
        } else {
            teleportButton.setIconResource(R.drawable.ic_shuffle);
        }
    }

    private void bindLists(View view) {
        StationAdapter saved = new StationAdapter(this);
        RecyclerView savedList = view.findViewById(R.id.saved_list);
        savedList.setLayoutManager(new LinearLayoutManager(requireContext()));
        savedList.setAdapter(saved);
        View savedLabel = view.findViewById(R.id.saved_label);
        explore.favorites().observe(getViewLifecycleOwner(), stations -> {
            saved.setStations(stations);
            int visibility = stations.isEmpty() ? View.GONE : View.VISIBLE;
            savedLabel.setVisibility(visibility);
            savedList.setVisibility(visibility);
        });

        StationAdapter recent = new StationAdapter(this);
        RecyclerView recentList = view.findViewById(R.id.recent_list);
        recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentList.setAdapter(recent);
        View recentEmpty = view.findViewById(R.id.recent_empty);
        explore.recent().observe(getViewLifecycleOwner(), stations -> {
            recent.setStations(stations);
            recentEmpty.setVisibility(stations.isEmpty() ? View.VISIBLE : View.GONE);
        });

        explore.favoriteIds().observe(getViewLifecycleOwner(), ids -> {
            saved.setFavoriteIds(ids);
            recent.setFavoriteIds(ids);
        });
    }

    private void bindMiniPlayer(View view) {
        miniPlayer = view.findViewById(R.id.mini_player);
        TextView name = view.findViewById(R.id.mini_player_name);
        TextView place = view.findViewById(R.id.mini_player_place);
        MaterialButton button = view.findViewById(R.id.mini_player_button);
        EqualizerView equalizer = view.findViewById(R.id.mini_player_equalizer);
        button.setOnClickListener(v -> explore.togglePlayback());

        explore.station().observe(getViewLifecycleOwner(), station -> {
            if (station == null) {
                return;
            }
            if (miniPlayer.getVisibility() != View.VISIBLE) {
                slideUp(miniPlayer);
            }
            name.setText(station.name);
            place.setText(placeOf(station));
            bindPlayButton(button, equalizer, station, explore.playerState().getValue());
        });
        explore.playerState().observe(getViewLifecycleOwner(), state -> {
            Station station = explore.station().getValue();
            if (station != null) {
                bindPlayButton(button, equalizer, station, state);
            }
        });
    }

    private void bindPlayButton(MaterialButton button, EqualizerView equalizer, Station station, RadioPlayer.State state) {
        boolean on = explore.isPlayerOn(station)
                && (state == RadioPlayer.State.PLAYING || state == RadioPlayer.State.LOADING);
        button.setIconResource(on ? R.drawable.ic_pause : R.drawable.ic_play);
        button.setContentDescription(getString(on ? R.string.pause_station : R.string.play_station, station.name));
        equalizer.setPlaying(on && state == RadioPlayer.State.PLAYING);
    }

    // The first station slides the mini player up from below
    private static void slideUp(View panel) {
        panel.setVisibility(View.VISIBLE);
        panel.post(() -> {
            panel.setTranslationY(panel.getHeight());
            panel.animate().translationY(0).setDuration(MINI_PLAYER_SLIDE_MS).start();
        });
    }

    /** The Radio Browser tag of a genre chip, or null for All. */
    @Nullable
    private static String tagFor(@IdRes int chipId) {
        if (chipId == R.id.genre_news) return "news";
        if (chipId == R.id.genre_music) return "music";
        if (chipId == R.id.genre_jazz) return "jazz";
        if (chipId == R.id.genre_talk) return "talk";
        if (chipId == R.id.genre_sports) return "sports";
        return null;
    }

    // StationAdapter.Listener

    @Override
    public void onPlay(Station station) {
        explore.play(station);
    }

    @Override
    public void onFavorite(Station station, boolean favorite) {
        explore.toggleFavorite(station, favorite);
    }

    @Override
    public String placeOf(Station station) {
        return explore.placeOf(station);
    }
}
