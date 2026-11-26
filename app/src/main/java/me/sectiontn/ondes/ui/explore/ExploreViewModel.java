package me.sectiontn.ondes.ui.explore;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.data.countries.CountryCatalog;
import me.sectiontn.ondes.data.repo.StationRepository;
import me.sectiontn.ondes.player.RadioPlayer;

/** Explore: Teleport to a random live station, replay recent ones, keep favorites. */
public class ExploreViewModel extends AndroidViewModel {

    /** Dead streams in a row before Teleport gives up and says so. */
    private static final int MAX_FAILURES = 3;

    private final StationRepository stations;
    private final RadioPlayer player;
    private final CountryCatalog catalog;

    private final MutableLiveData<Boolean> teleporting = new MutableLiveData<>(false);
    /** The station of the mini player, null until something played. */
    private final MutableLiveData<Station> station = new MutableLiveData<>();
    /** A message to show once, then cleared. */
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    @Nullable
    private String genre = null;
    // Stations Teleport may still try, and the one loading now
    private final List<Station> queue = new ArrayList<>();
    @Nullable
    private Station pending;
    private int failures = 0;
    // The player generation when the latest Teleport started, so an older search's answer leaves its spinner alone
    private int teleportGeneration = -1;

    private final Observer<RadioPlayer.State> playerObserver = this::onPlayerState;

    public ExploreViewModel(@NonNull Application application) {
        super(application);
        stations = StationRepository.get(application);
        player = RadioPlayer.get(application);
        catalog = CountryCatalog.get(application);
        player.state().observeForever(playerObserver);
    }

    public LiveData<Boolean> teleporting() {
        return teleporting;
    }

    public LiveData<Station> station() {
        return station;
    }

    public LiveData<Integer> message() {
        return message;
    }

    public void messageShown() {
        message.setValue(null);
    }

    public LiveData<RadioPlayer.State> playerState() {
        return player.state();
    }

    public LiveData<List<Station>> recent() {
        return stations.recent();
    }

    public LiveData<List<Station>> favorites() {
        return stations.favorites();
    }

    public LiveData<Set<String>> favoriteIds() {
        return stations.favoriteIds();
    }

    /** "Porto Alegre, Brazil", or only the country when the station has no city. */
    public String placeOf(Station s) {
        String country = catalog.nameOf(s.countryCode);
        return s.state.isEmpty() ? country : s.state + ", " + country;
    }

    /** True when the mini player's station is the one in the player. */
    public boolean isPlayerOn(@Nullable Station s) {
        return player.isCurrent(s);
    }

    /** The genre chip: a lowercase tag such as "jazz", or null for All. */
    public void setGenre(@Nullable String tag) {
        genre = tag;
    }

    /** Jumps to a random working station of the chosen genre. */
    public void teleport() {
        if (Boolean.TRUE.equals(teleporting.getValue())) {
            return;
        }
        teleporting.setValue(true);
        int generation = player.generation();
        teleportGeneration = generation;
        stations.stationsWithTag(genre, new StationRepository.Callback<List<Station>>() {
            @Override
            public void onResult(List<Station> found) {
                // A game started or the user left the tab while the list loaded, so this answer is dropped
                if (player.generation() != generation) {
                    // After a return to the tab, a newer Teleport may own the spinner
                    if (generation == teleportGeneration) {
                        teleporting.setValue(false);
                    }
                    return;
                }
                queue.clear();
                queue.addAll(found);
                // Radio Browser caches its random order for minutes, so the shuffle happens here
                Collections.shuffle(queue);
                Station now = station.getValue();
                if (now != null && queue.size() > 1) {
                    queue.removeIf(s -> s.uuid.equals(now.uuid));
                }
                failures = 0;
                playNextInQueue();
            }

            @Override
            public void onError(IOException error) {
                if (generation != teleportGeneration) {
                    return;
                }
                teleporting.setValue(false);
                if (player.generation() == generation) {
                    message.setValue(R.string.error_directory);
                }
            }
        });
    }

    /** A tap on a Recent or Saved row. */
    public void play(Station s) {
        queue.clear();
        failures = MAX_FAILURES - 1;
        load(s);
    }

    /** The mini player button. */
    public void togglePlayback() {
        Station current = station.getValue();
        if (current == null) {
            return;
        }
        RadioPlayer.State state = player.state().getValue();
        if (player.isCurrent(current) && (state == RadioPlayer.State.PLAYING || state == RadioPlayer.State.LOADING)) {
            player.pause();
        } else if (player.isCurrent(current)) {
            player.resume();
        } else {
            // A game used the player since, so this station starts again
            load(current);
        }
    }

    /** Stops Explore's sound or Teleport when the user leaves the tab. The mini player keeps the station. */
    public void leave() {
        boolean busy = pending != null || Boolean.TRUE.equals(teleporting.getValue());
        if (busy || player.isCurrent(station.getValue())) {
            queue.clear();
            pending = null;
            teleporting.setValue(false);
            // stop() also changes the player's generation, so a Teleport search still loading drops its result
            player.stop();
        }
    }

    public void toggleFavorite(Station s, boolean favorite) {
        stations.setFavorite(s, favorite);
    }

    private void playNextInQueue() {
        if (queue.isEmpty()) {
            fail(R.string.error_streams);
            return;
        }
        load(queue.remove(0));
    }

    private void load(Station s) {
        pending = s;
        player.play(s);
    }

    private void onPlayerState(RadioPlayer.State state) {
        if (pending == null) {
            return;
        }
        if (!player.isCurrent(pending)) {
            // A game took the player before this station started: stop waiting for it
            pending = null;
            teleporting.setValue(false);
            return;
        }
        if (state == RadioPlayer.State.PLAYING) {
            Station played = pending;
            pending = null;
            failures = 0;
            teleporting.setValue(false);
            station.setValue(played);
            // The user chose to play it, so it counts as a click for Radio Browser
            stations.countClick(played.uuid);
            stations.recordPlay(played);
        } else if (state == RadioPlayer.State.FAILED) {
            failures++;
            if (failures >= MAX_FAILURES) {
                fail(R.string.error_streams);
            } else {
                playNextInQueue();
            }
        }
    }

    private void fail(@StringRes int text) {
        pending = null;
        teleporting.setValue(false);
        message.setValue(text);
    }

    @Override
    protected void onCleared() {
        player.state().removeObserver(playerObserver);
    }
}
