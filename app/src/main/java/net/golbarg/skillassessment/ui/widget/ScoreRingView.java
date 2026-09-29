package net.golbarg.skillassessment.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.util.UiUtils;

/** A donut chart with correct / wrong / skipped segments that animates in. */
public class ScoreRingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segment = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final int[] colors = new int[3];
    private final float[] values = new float[3];
    private final float strokeWidth;
    private float progress = 1f;

    public ScoreRingView(Context context) {
        this(context, null);
    }

    public ScoreRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        strokeWidth = UiUtils.dp(context, 14);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(strokeWidth);
        track.setColor(UiUtils.color(context, com.google.android.material.R.attr.colorSurfaceContainerHighest));
        segment.setStyle(Paint.Style.STROKE);
        segment.setStrokeWidth(strokeWidth);
        segment.setStrokeCap(Paint.Cap.ROUND);
        colors[0] = ContextCompat.getColor(context, R.color.correct);
        colors[1] = ContextCompat.getColor(context, R.color.wrong);
        colors[2] = ContextCompat.getColor(context, R.color.skipped);
    }

    public void setValues(int correct, int wrong, int skipped, boolean animate) {
        values[0] = correct;
        values[1] = wrong;
        values[2] = skipped;
        if (animate) {
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(900);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a -> {
                progress = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            progress = 1f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = strokeWidth / 2f + 1;
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawArc(bounds, 0, 360, false, track);

        float total = values[0] + values[1] + values[2];
        if (total <= 0) return;
        // Leave a small visual gap between segments; round caps extend each end by half the stroke.
        float radius = bounds.width() / 2f;
        float capDegrees = (float) Math.toDegrees(strokeWidth / 2f / radius);
        int nonZero = 0;
        for (float v : values) if (v > 0) nonZero++;
        float gap = nonZero > 1 ? capDegrees * 2 + 4 : 0;

        float start = -90f;
        float available = 360f * progress - gap * nonZero;
        for (int i = 0; i < 3; i++) {
            if (values[i] <= 0) continue;
            float sweep = Math.max(0.1f, available * values[i] / total);
            segment.setColor(colors[i]);
            canvas.drawArc(bounds, start + gap / 2f, sweep, false, segment);
            start += sweep + gap;
        }
    }
}
