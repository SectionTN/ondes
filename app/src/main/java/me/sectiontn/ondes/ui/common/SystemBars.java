package me.sectiontn.ondes.ui.common;

import android.graphics.Color;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;

/** Edge-to-edge setup shared by every activity. */
public final class SystemBars {

    private SystemBars() {
    }

    /** Draws behind the status and navigation bars, with light icons on every phone since the app is always dark. */
    public static void enableEdgeToEdge(ComponentActivity activity) {
        EdgeToEdge.enable(activity, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
    }
}
