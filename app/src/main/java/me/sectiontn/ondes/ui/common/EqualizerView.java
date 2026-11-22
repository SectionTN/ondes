package me.sectiontn.ondes.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import me.sectiontn.ondes.R;

/** Rounded accent bars that show a station is live. Decorative, so screen readers skip it. */
public class EqualizerView extends View {

    // Height of each bar as a share of the view height, taken from the design mock
    private static final float[] LEVELS = {0.46f, 0.79f, 0.63f, 1f, 0.54f, 0.88f, 0.71f, 0.42f};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private final int barCount;
    private final float barWidth;
    private final float barGap;

    public EqualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EqualizerView);
        barCount = Math.min(a.getInt(R.styleable.EqualizerView_barCount, 3), LEVELS.length);
        barGap = a.getDimension(R.styleable.EqualizerView_barGap, getResources().getDimension(R.dimen.equalizer_bar_gap));
        a.recycle();
        barWidth = getResources().getDimension(R.dimen.equalizer_bar_width);
        paint.setColor(ContextCompat.getColor(context, R.color.accent));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = Math.round(barCount * barWidth + (barCount - 1) * barGap);
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), getDefaultSize(0, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float height = getHeight();
        float radius = barWidth / 2f;
        for (int i = 0; i < barCount; i++) {
            float left = i * (barWidth + barGap);
            // Bars grow from the bottom edge
            bar.set(left, height - LEVELS[i] * height, left + barWidth, height);
            canvas.drawRoundRect(bar, radius, radius, paint);
        }
    }
}
