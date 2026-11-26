package me.sectiontn.ondes.data.repo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.data.api.ApiClient;
import me.sectiontn.ondes.data.api.StationDto;
import me.sectiontn.ondes.data.db.AppDatabase;
import me.sectiontn.ondes.data.db.StationDao;
import me.sectiontn.ondes.data.db.StationEntity;

/**
 * Stations from two sources: Radio Browser for live ones, the database for recent and saved ones.
 * Network and disk work run on background threads, answers come back on the main thread.
 */
public final class StationRepository {

    private static final String TAG = "StationRepository";
    /** How many stations Explore fetches for one Teleport. */
    private static final int TELEPORT_LIMIT = 100;

    private static StationRepository instance;

    private final ExecutorService network = Executors.newFixedThreadPool(2);
    private final ExecutorService disk = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final ApiClient api = ApiClient.get();
    private final StationDao dao;

    /** Gets the answer of a request. Both methods run on the main thread. */
    public interface Callback<T> {
        void onResult(T value);

        void onError(IOException error);
    }

    private StationRepository(Context context) {
        dao = AppDatabase.get(context).stations();
    }

    public static synchronized StationRepository get(Context context) {
        if (instance == null) {
            instance = new StationRepository(context);
        }
        return instance;
    }

    // Radio Browser

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
            } catch (IOException | RuntimeException e) {
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

    // Database

    /** The last 20 stations played in Explore, newest first. */
    public LiveData<List<Station>> recent() {
        return Transformations.map(dao.recent(), StationRepository::toStations);
    }

    public LiveData<List<Station>> favorites() {
        return Transformations.map(dao.favorites(), StationRepository::toStations);
    }

    public LiveData<Set<String>> favoriteIds() {
        return Transformations.map(dao.favoriteIds(), HashSet::new);
    }

    /** Puts the station at the top of Recent, keeping its favorite heart. */
    public void recordPlay(Station station) {
        disk.execute(() -> {
            StationEntity existing = dao.find(station.uuid);
            StationEntity entity = StationEntity.from(station);
            entity.isFavorite = existing != null && existing.isFavorite;
            entity.lastPlayedAt = System.currentTimeMillis();
            dao.save(entity);
            dao.pruneRecent();
        });
    }

    /** Saves or unsaves the station. A station only saved, never played in Explore, is then deleted. */
    public void setFavorite(Station station, boolean favorite) {
        disk.execute(() -> {
            StationEntity entity = dao.find(station.uuid);
            if (entity == null) {
                if (!favorite) {
                    return;
                }
                entity = StationEntity.from(station);
            }
            entity.isFavorite = favorite;
            if (!favorite && entity.lastPlayedAt == 0) {
                dao.delete(entity);
            } else {
                dao.save(entity);
            }
        });
    }

    private static List<Station> toStations(List<StationEntity> entities) {
        List<Station> stations = new ArrayList<>();
        for (StationEntity entity : entities) {
            stations.add(entity.toStation());
        }
        return stations;
    }
}
