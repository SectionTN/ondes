package me.sectiontn.ondes.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.data.api.ApiClient;

/**
 * The one audio player of the app, shared by the game and Explore.
 * Screens watch {@link #state()} and call play, pause and resume. Every method runs on the main thread.
 */
public final class RadioPlayer {

    /** What the player is doing, as the screens show it. */
    public enum State {
        /** Nothing loaded. */
        IDLE,
        /** Connecting or buffering: no sound yet. */
        LOADING,
        PLAYING,
        PAUSED,
        /** The stream failed or did not start in time. The caller picks another station. */
        FAILED
    }

    private static final String TAG = "RadioPlayer";
    /** A stream that is not playing after this long counts as dead. */
    private static final long START_TIMEOUT_MS = 8000;
    private static final int HTTP_TIMEOUT_MS = 8000;

    private static RadioPlayer instance;

    private final ExoPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);
    @Nullable
    private Station station;
    // Set when the app went to the background while a station was playing
    private boolean pausedByBackground = false;

    private final Runnable startTimeout = () -> {
        Log.w(TAG, "No sound after " + START_TIMEOUT_MS + " ms: " + describe());
        fail();
    };

    @OptIn(markerClass = UnstableApi.class)
    private RadioPlayer(Context context) {
        // Many stations redirect from http to https or back, which ExoPlayer refuses unless allowed
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent(ApiClient.USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(HTTP_TIMEOUT_MS)
                .setReadTimeoutMs(HTTP_TIMEOUT_MS);
        player = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(context).setDataSourceFactory(http))
                .build();
        AudioAttributes music = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
        // true: pause when a call or another app takes the audio
        player.setAudioAttributes(music, true);
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updateState();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updateState();
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.w(TAG, "Stream failed: " + describe(), error);
                fail();
            }
        });
    }

    public static synchronized RadioPlayer get(Context context) {
        if (instance == null) {
            instance = new RadioPlayer(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<State> state() {
        return state;
    }

    @Nullable
    public Station station() {
        return station;
    }

    /** True when this station is the one loaded, playing or not. */
    public boolean isCurrent(@Nullable Station other) {
        return station != null && other != null && station.uuid.equals(other.uuid);
    }

    /** Stops what is playing and starts this station. */
    public void play(Station next) {
        station = next;
        pausedByBackground = false;
        MediaItem.Builder item = new MediaItem.Builder().setUri(next.streamUrl);
        if (next.hls) {
            item.setMimeType(MimeTypes.APPLICATION_M3U8);
        }
        player.setMediaItem(item.build());
        player.prepare();
        player.setPlayWhenReady(true);
        handler.removeCallbacks(startTimeout);
        handler.postDelayed(startTimeout, START_TIMEOUT_MS);
        state.setValue(State.LOADING);
    }

    /** Live radio cannot wait, so a pause drops the connection and {@link #resume()} reconnects. */
    public void pause() {
        handler.removeCallbacks(startTimeout);
        player.setPlayWhenReady(false);
        player.stop();
        if (station != null) {
            state.setValue(State.PAUSED);
        }
    }

    /** Reconnects to the paused station, so the sound is what it airs now. */
    public void resume() {
        if (station != null) {
            play(station);
        }
    }

    public void stop() {
        handler.removeCallbacks(startTimeout);
        player.stop();
        player.clearMediaItems();
        station = null;
        pausedByBackground = false;
        state.setValue(State.IDLE);
    }

    /** Called when the last screen of the app leaves the foreground. */
    public void onAppBackground() {
        if (player.getPlayWhenReady() && station != null) {
            pausedByBackground = true;
            pause();
        }
    }

    /** Called when a screen of the app comes back. Resumes only what the background paused. */
    public void onAppForeground() {
        if (pausedByBackground) {
            pausedByBackground = false;
            resume();
        }
    }

    private void fail() {
        handler.removeCallbacks(startTimeout);
        player.stop();
        state.setValue(State.FAILED);
    }

    private void updateState() {
        if (state.getValue() == State.FAILED && player.getPlaybackState() == Player.STATE_IDLE) {
            return;
        }
        State next;
        if (player.isPlaying()) {
            handler.removeCallbacks(startTimeout);
            next = State.PLAYING;
        } else if (!player.getPlayWhenReady()) {
            next = station == null ? State.IDLE : State.PAUSED;
        } else if (player.getPlaybackState() == Player.STATE_ENDED) {
            // A live stream that ends has gone off the air
            fail();
            return;
        } else {
            next = State.LOADING;
        }
        if (next != state.getValue()) {
            state.setValue(next);
        }
    }

    private String describe() {
        return station == null ? "no station" : station.name + " " + station.streamUrl;
    }
}
