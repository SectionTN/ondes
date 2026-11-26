package me.sectiontn.ondes.ui.common;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/** Calls back when the phone is shaken hard: more than 2.7 g, at most once per second. */
public class ShakeDetector implements SensorEventListener {

    private static final float SHAKE_THRESHOLD_G = 2.7f;
    private static final long DEBOUNCE_MS = 1000;

    public interface Listener {
        void onShake();
    }

    private final Listener listener;
    private long lastShakeAt = 0;

    public ShakeDetector(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // The accelerometer gives m/s2 on three axes. At rest the total is about 1 g, from gravity
        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
        double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);
        long now = SystemClock.elapsedRealtime();
        if (gForce > SHAKE_THRESHOLD_G && now - lastShakeAt > DEBOUNCE_MS) {
            lastShakeAt = now;
            listener.onShake();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
