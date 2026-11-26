package me.sectiontn.ondes;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.sectiontn.ondes.player.RadioPlayer;

/** Pauses the radio when the app leaves the screen and resumes it when the app comes back. */
public class OndesApp extends Application implements Application.ActivityLifecycleCallbacks {

    // Activities between onStart and onStop. Zero means the app is in the background
    private int startedActivities = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        startedActivities++;
        if (startedActivities == 1) {
            RadioPlayer.get(this).onAppForeground();
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        startedActivities--;
        // A rotation stops and restarts the activity, and the radio keeps playing through it
        if (startedActivities == 0 && !activity.isChangingConfigurations()) {
            RadioPlayer.get(this).onAppBackground();
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
