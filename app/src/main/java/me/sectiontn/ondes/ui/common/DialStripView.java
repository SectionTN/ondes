package me.sectiontn.ondes.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import me.sectiontn.ondes.R;

/** The FM dial under the timer: ticks, a center needle and the 88 to 108 labels. Pure decoration. */
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
        float center = width / 2f;
        canvas.drawRect(center - NEEDLE_WIDTH * dp / 2f, 0, center + NEEDLE_WIDTH * dp / 2f, stripBottom, needle);
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
