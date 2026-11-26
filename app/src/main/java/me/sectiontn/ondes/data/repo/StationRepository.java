package me.sectiontn.ondes.data.repo;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.data.api.ApiClient;
import me.sectiontn.ondes.data.api.StationDto;

/** Fetches stations from Radio Browser on a background thread and hands them back on the main thread. */
public final class StationRepository {

    private static final String TAG = "StationRepository";
    /** How many stations Explore fetches for one Teleport. */
    private static final int TELEPORT_LIMIT = 100;

    private static StationRepository instance;

    private final ExecutorService network = Executors.newFixedThreadPool(2);
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final ApiClient api = ApiClient.get();

    /** Gets the answer of a request. Both methods run on the main thread. */
    public interface Callback<T> {
        void onResult(T value);

        void onError(IOException error);
    }

    private StationRepository() {
    }

    public static synchronized StationRepository get() {
        if (instance == null) {
            instance = new StationRepository();
        }
        return instance;
    }

    /** The country's most voted working stations, always in the same order. */
    public void topStations(String countryCode, int limit, Callback<List<Station>> callback) {
        load(() -> api.execute(a -> a.byCountry(countryCode, "votes", true, limit)), callback);
    }

    /** Working stations with this genre tag, or of any genre when the tag is null. */
    public void stationsWithTag(@Nullable String tag, Callback<List<Station>> callback) {
        load(() -> api.execute(a -> a.byTag(tag, "random", TELEPORT_LIMIT)), callback);
    }

    /** Tells Radio Browser the user chose to play this station. Failures do not matter to the user. */
    public void countClick(String uuid) {
        network.execute(() -> {
            try {
                api.execute(a -> a.click(uuid));
            } catch (IOException e) {
                Log.w(TAG, "Click count failed for " + uuid, e);
            }
        });
    }

    private interface Request {
        List<StationDto> run() throws IOException;
    }

    private void load(Request request, Callback<List<Station>> callback) {
        network.execute(() -> {
            try {
                List<StationDto> answer = request.run();
                List<Station> stations = new ArrayList<>();
                if (answer != null) {
                    for (StationDto dto : answer) {
                        Station station = Station.fromDto(dto);
                        if (station != null) {
                            stations.add(station);
                        }
                    }
                }
                mainThread.post(() -> callback.onResult(stations));
            } catch (IOException | RuntimeException e) {
                IOException error = e instanceof IOException ? (IOException) e : new IOException(e);
                Log.w(TAG, "Station request failed", error);
                mainThread.post(() -> callback.onError(error));
            }
        });
    }
}
