package me.sectiontn.ondes.ui.common;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.view.ViewCompat;

/** The small animations the screens share. With "Remove animations" on, Android ends each one as it starts. */
public final class Motion {

    /** Fast start and soft landing, for anything that enters the screen. */
    public static final Interpolator ENTER = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
    /** How long a number takes to count up. */
    public static final long COUNT_MS = 600;
    /** Start size of a view that pops in from small. */
    public static final float POP_FROM_SMALL = 0.6f;

    private static final long RISE_MS = 280;
    private static final float RISE_DP = 16;
    private static final long STAGGER_MS = 50;
    private static final long FADE_MS = 150;
    private static final long POP_MS = 220;
    private static final long PULSE_MS = 240;
    private static final float PULSE_SCALE = 1.2f;
    private static final long SHAKE_MS = 400;
    private static final float SHAKE_DP = 8;
    private static final long WIGGLE_MS = 450;
    private static final long WAVE_MS = 800;
    private static final float WAVE_SCALE = 2.4f;
    private static final float WAVE_ALPHA = 0.6f;
    private static final long TINT_MS = 300;
    private static final long FLIP_MS = 320;
    private static final float FLIP_CAMERA_DP = 8000;
    private static final long BLINK_MS = 900;
    private static final float BLINK_ALPHA = 0.25f;
    private static final long SPIN_MS = 6000;

    private Motion() {
    }

    /** How a counting number is written, such as "+420 pts". It must not call the fragment, which may be gone. */
    public interface Label {
        String of(int value);
    }

    /** False when the system has animations off. Loops and waits check it, the other animations need not. */
    public static boolean enabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8 and later answer directly, and count Battery Saver as well as "Remove animations"
            return ValueAnimator.areAnimatorsEnabled();
        }
        return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }

    /** Fades the view in while it rises into place. */
    public static void riseIn(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(RISE_DP * density(view));
        view.animate().alpha(1f).translationY(0f)
                .setStartDelay(delayMs).setDuration(RISE_MS).setInterpolator(ENTER).start();
    }

    /** Rises the visible children in one after another, starting at the child {@code first}. */
    public static void riseInChildren(ViewGroup parent, int first) {
        long delay = 0;
        for (int i = first; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            // Spacers are invisible views and stay out of it
            if (child.getVisibility() == View.VISIBLE) {
                riseIn(child, delay);
                delay += STAGGER_MS;
            }
        }
    }

    public static void fadeIn(View view) {
        view.setAlpha(0f);
        view.animate().alpha(1f).setStartDelay(0).setDuration(FADE_MS).setInterpolator(new LinearInterpolator()).start();
    }

    /** Fades out the children from {@code first} on, after a wait. */
    public static void fadeOutChildren(ViewGroup parent, int first, long delayMs) {
        for (int i = first; i < parent.getChildCount(); i++) {
            parent.getChildAt(i).animate().alpha(0f)
                    .setStartDelay(delayMs).setDuration(FADE_MS).setInterpolator(new LinearInterpolator()).start();
        }
    }

    /** Brings the view in from another size and lets it settle a little past its own, like a stamp or a bubble. */
    public static void pop(View view, float fromScale, long delayMs) {
        view.setAlpha(0f);
        view.setScaleX(fromScale);
        view.setScaleY(fromScale);
        view.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(delayMs).setDuration(POP_MS).setInterpolator(new OvershootInterpolator()).start();
    }

    /** A quick bump in size, for a heart that fills or a clock that runs out. */
    public static void pulse(View view) {
        if (!enabled(view.getContext())) {
            return;
        }
        view.animate().cancel();
        view.setScaleX(PULSE_SCALE);
        view.setScaleY(PULSE_SCALE);
        view.animate().scaleX(1f).scaleY(1f)
                .setStartDelay(0).setDuration(PULSE_MS).setInterpolator(new OvershootInterpolator()).start();
    }

    /** Shakes the view left and right: a wrong answer. */
    public static void shake(View view, long delayMs) {
        float d = SHAKE_DP * density(view);
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -d, d, -d / 2, d / 2, 0f);
        shake.setStartDelay(delayMs);
        shake.setDuration(SHAKE_MS);
        shake.start();
    }

    /** Rocks the view around its center, like a phone being shaken. */
    public static void wiggle(View view) {
        ObjectAnimator wiggle = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, -14f, 12f, -8f, 6f, 0f);
        wiggle.setDuration(WIGGLE_MS);
        wiggle.start();
    }

    /** Grows a ring and fades it away, like a radio wave leaving the view it sits behind. */
    public static void wave(View ring, long delayMs) {
        ring.setScaleX(1f);
        ring.setScaleY(1f);
        ring.setAlpha(WAVE_ALPHA);
        ring.animate().alpha(0f).scaleX(WAVE_SCALE).scaleY(WAVE_SCALE)
                .setStartDelay(delayMs).setDuration(WAVE_MS).setInterpolator(new DecelerateInterpolator()).start();
    }

    /** Turns the view over into place, like a card flipped face up. */
    public static void flipIn(View view) {
        // A far camera keeps the edges from stretching during the turn
        view.setCameraDistance(FLIP_CAMERA_DP * density(view));
        view.setRotationX(-90f);
        view.animate().rotationX(0f)
                .setStartDelay(0).setDuration(FLIP_MS).setInterpolator(new OvershootInterpolator()).start();
    }

    /** Counts the number in the text view from one value to another. The animator is returned so a new count can cancel it. */
    public static ValueAnimator count(TextView text, int from, int to, long durationMs, Label label) {
        ValueAnimator count = ValueAnimator.ofInt(from, to).setDuration(durationMs);
        count.setInterpolator(new DecelerateInterpolator());
        count.addUpdateListener(a -> text.setText(label.of((int) a.getAnimatedValue())));
        count.start();
        return count;
    }

    /** Fades the tint of the view's background from one color to another. */
    public static void tint(View view, @ColorInt int from, @ColorInt int to) {
        ValueAnimator tint = ValueAnimator.ofArgb(from, to).setDuration(TINT_MS);
        tint.addUpdateListener(a -> ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf((int) a.getAnimatedValue())));
        tint.start();
    }

    /** A blink that goes on until it is cancelled. The caller starts it, and only when {@link #enabled} says so. */
    public static ObjectAnimator blink(View view) {
        ObjectAnimator blink = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, BLINK_ALPHA);
        blink.setDuration(BLINK_MS);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        return blink;
    }

    /** A slow turn that goes on until it is cancelled. The caller starts it, and only when {@link #enabled} says so. */
    public static ObjectAnimator spin(View view) {
        ObjectAnimator spin = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 360f);
        spin.setDuration(SPIN_MS);
        spin.setInterpolator(new LinearInterpolator());
        spin.setRepeatCount(ValueAnimator.INFINITE);
        return spin;
    }

    private static float density(View view) {
        return view.getResources().getDisplayMetrics().density;
    }
}
