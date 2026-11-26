package me.sectiontn.ondes.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import me.sectiontn.ondes.R;

/** Rounded accent bars that bounce while a station plays. Decorative, so screen readers skip it. */
public class EqualizerView extends View {

    // Height of each bar as a share of the view height, taken from the design mock
    private static final float[] LEVELS = {0.46f, 0.79f, 0.63f, 1f, 0.54f, 0.88f, 0.71f, 0.42f};
    // Where each bar starts in its cycle, so they never move together
    private static final float[] OFFSETS = {0f, 0.35f, 0.7f, 0.15f, 0.55f, 0.85f, 0.25f, 0.6f};
    private static final long CYCLE_MS = 1100;
    private static final float MIN_SCALE = 0.35f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private final int barCount;
    private final float barWidth;
    private final float barGap;
    private boolean playing = false;
    private boolean animated = false;

    public EqualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EqualizerView);
        barCount = Math.min(a.getInt(R.styleable.EqualizerView_barCount, 3), LEVELS.length);
        barGap = a.getDimension(R.styleable.EqualizerView_barGap, getResources().getDimension(R.dimen.equalizer_bar_gap));
        barWidth = a.getDimension(R.styleable.EqualizerView_barWidth, getResources().getDimension(R.dimen.equalizer_bar_width));
        a.recycle();
        paint.setColor(ContextCompat.getColor(context, R.color.accent));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Starts or freezes the bars. */
    public void setPlaying(boolean playing) {
        if (this.playing != playing) {
            this.playing = playing;
            animated = playing && animationsEnabled();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = Math.round(barCount * barWidth + (barCount - 1) * barGap);
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), getDefaultSize(0, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean animate = animated;
        float time = (SystemClock.uptimeMillis() % CYCLE_MS) / (float) CYCLE_MS;
        float height = getHeight();
        float radius = barWidth / 2f;
        for (int i = 0; i < barCount; i++) {
            float scale = 1f;
            if (animate) {
                // A cosine wave between 0.35 and 1 eases in and out at both ends
                double wave = 0.5 - 0.5 * Math.cos(2 * Math.PI * (time + OFFSETS[i]));
                scale = MIN_SCALE + (1f - MIN_SCALE) * (float) wave;
            }
            float left = i * (barWidth + barGap);
            // Bars grow from the bottom edge
            bar.set(left, height - LEVELS[i] * scale * height, left + barWidth, height);
            canvas.drawRoundRect(bar, radius, radius, paint);
        }
        if (animate) {
            postInvalidateOnAnimation();
        }
    }

    // "Remove animations" in the accessibility settings sets the animator scale to 0
    private boolean animationsEnabled() {
        return Settings.Global.getFloat(getContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }
}
