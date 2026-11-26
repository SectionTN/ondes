package me.sectiontn.ondes.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import me.sectiontn.ondes.R;

/** The FM dial under the timer: ticks, a needle and the 88 to 108 labels. Pure decoration. */
public class DialStripView extends View {

    private static final String[] LABELS = {"88", "92", "96", "100", "104", "108"};

    // Sizes in dp, from the design
    private static final float STRIP_HEIGHT = 30;
    private static final float CORNER_RADIUS = 10;
    private static final float MINOR_STEP = 8;
    private static final float MINOR_WIDTH = 1;
    private static final float MINOR_HEIGHT = 10;
    private static final float MAJOR_STEP = 40;
    private static final float MAJOR_WIDTH = 2;
    private static final float MAJOR_HEIGHT = 18;
    private static final float NEEDLE_WIDTH = 2;
    private static final float LABEL_GAP = 6;
    private static final float LABEL_INSET = 2;
    private static final float LABEL_SIZE_SP = 11;
    // The needle travels between these two points of the strip, 0 being the left edge and 1 the right
    private static final float NEEDLE_MIN = 0.12f;
    private static final float NEEDLE_MAX = 0.88f;
    // One swing of the search, from the middle to both ends and back
    private static final long SCAN_MS = 3200;
    private static final long TUNE_MS = 500;
    private static final float NOT_TUNED = -1f;

    private float needlePosition = 0.5f;
    // Where tuneTo last sent the needle, so tuning to the same station again does nothing
    private float tunedSpot = NOT_TUNED;
    @Nullable
    private ValueAnimator needleMove;
    private boolean scanning = false;

    private final float dp;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minorTick = new Paint();
    private final Paint majorTick = new Paint();
    private final Paint needle = new Paint();
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF strip = new RectF();
    private final Path stripShape = new Path();

    public DialStripView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        fill.setColor(ContextCompat.getColor(context, R.color.bg_deep));
        border.setColor(ContextCompat.getColor(context, R.color.divider));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp);
        minorTick.setColor(ContextCompat.getColor(context, R.color.dash));
        majorTick.setColor(ContextCompat.getColor(context, R.color.tick_major));
        needle.setColor(ContextCompat.getColor(context, R.color.accent));
        label.setColor(ContextCompat.getColor(context, R.color.text_faint));
        label.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE_SP,
                getResources().getDisplayMetrics()));
        label.setTypeface(ResourcesCompat.getFont(context, R.font.jetbrains_mono_medium));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Swings the needle from side to side, like a hand searching the band, until tuneTo or stopScan is called. */
    public void scan() {
        if (scanning || !Motion.enabled(getContext())) {
            return;
        }
        scanning = true;
        // A sine wave around the middle of the strip, entered at the angle where the needle is now, so it never jumps
        float swing = (NEEDLE_MAX - NEEDLE_MIN) / 2f;
        float from = (float) Math.asin(Math.max(-1f, Math.min(1f, (needlePosition - 0.5f) / swing)));
        ValueAnimator scan = ValueAnimator.ofFloat(from, from + (float) (2 * Math.PI));
        scan.setDuration(SCAN_MS);
        scan.setInterpolator(new LinearInterpolator());
        scan.setRepeatCount(ValueAnimator.INFINITE);
        scan.addUpdateListener(a -> moveNeedle(0.5f + swing * (float) Math.sin((float) a.getAnimatedValue())));
        startMove(scan);
    }

    /** Stops the needle on the station's own spot of the dial: the same station always lands on the same one. */
    public void tuneTo(String stationId) {
        float spot = NEEDLE_MIN + (NEEDLE_MAX - NEEDLE_MIN) * Math.floorMod(stationId.hashCode(), 101) / 100f;
        if (!scanning && spot == tunedSpot) {
            return;
        }
        scanning = false;
        tunedSpot = spot;
        ValueAnimator tune = ValueAnimator.ofFloat(needlePosition, spot);
        tune.setDuration(TUNE_MS);
        // It goes a little past the spot and comes back, like a needle settling
        tune.setInterpolator(new OvershootInterpolator());
        tune.addUpdateListener(a -> moveNeedle((float) a.getAnimatedValue()));
        startMove(tune);
    }

    /** Leaves the needle where it is when the search ends without a station, on an error or a pause. */
    public void stopScan() {
        if (scanning && needleMove != null) {
            scanning = false;
            tunedSpot = NOT_TUNED;
            needleMove.cancel();
        }
    }

    private void startMove(ValueAnimator move) {
        if (needleMove != null) {
            needleMove.cancel();
        }
        needleMove = move;
        move.start();
    }

    private void moveNeedle(float position) {
        needlePosition = position;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // An endless sweep would otherwise go on after the screen is gone
        if (needleMove != null) {
            needleMove.cancel();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics metrics = label.getFontMetrics();
        float height = (STRIP_HEIGHT + LABEL_GAP) * dp + metrics.descent - metrics.ascent;
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), Math.round(height));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float stripBottom = STRIP_HEIGHT * dp;

        // Rounded strip, with the ticks clipped to its corners
        float half = dp / 2f;
        strip.set(half, half, width - half, stripBottom - half);
        float radius = CORNER_RADIUS * dp;
        canvas.drawRoundRect(strip, radius, radius, fill);
        stripShape.reset();
        stripShape.addRoundRect(strip, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(stripShape);
        for (float x = dp; x < width; x += MINOR_STEP * dp) {
            canvas.drawRect(x, stripBottom - MINOR_HEIGHT * dp, x + MINOR_WIDTH * dp, stripBottom, minorTick);
        }
        for (float x = dp; x < width; x += MAJOR_STEP * dp) {
            canvas.drawRect(x, stripBottom - MAJOR_HEIGHT * dp, x + MAJOR_WIDTH * dp, stripBottom, majorTick);
        }
        float needleX = width * needlePosition;
        canvas.drawRect(needleX - NEEDLE_WIDTH * dp / 2f, 0, needleX + NEEDLE_WIDTH * dp / 2f, stripBottom, needle);
        canvas.restore();
        canvas.drawRoundRect(strip, radius, radius, border);

        // Labels spread edge to edge: first one on the left, last one on the right
        float textWidth = 0;
        for (String text : LABELS) {
            textWidth += label.measureText(text);
        }
        float inset = LABEL_INSET * dp;
        float gap = (width - 2 * inset - textWidth) / (LABELS.length - 1);
        float baseline = stripBottom + LABEL_GAP * dp - label.getFontMetrics().ascent;
        float x = inset;
        for (String text : LABELS) {
            canvas.drawText(text, x, baseline, label);
            x += label.measureText(text) + gap;
        }
    }
}
