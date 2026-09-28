package net.golbarg.skillassessment.ui.widget;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.Locale;

/** Circular countdown: the arc drains clockwise and shifts green → amber → red as time runs out. */
public class TimerRingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final ArgbEvaluator evaluator = new ArgbEvaluator();
    private final int colorOk;
    private final int colorWarn;
    private final int colorDanger;
    private float fraction = 1f;
    private String label = "";

    public TimerRingView(Context context) {
        this(context, null);
    }

    public TimerRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float stroke = UiUtils.dp(context, 3.5f);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(UiUtils.color(context, com.google.android.material.R.attr.colorSurfaceContainerHighest));
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setTextSize(UiUtils.dp(context, 14));
        colorOk = UiUtils.color(context, androidx.appcompat.R.attr.colorPrimary);
        colorWarn = ContextCompat.getColor(context, R.color.coin);
        colorDanger = ContextCompat.getColor(context, R.color.wrong);
    }

    /** @param remainingMs time left; @param totalMs the full duration. */
    public void setTime(long remainingMs, long totalMs) {
        fraction = totalMs <= 0 ? 0 : Math.max(0f, Math.min(1f, remainingMs / (float) totalMs));
        long seconds = (remainingMs + 999) / 1000;
        // Exams run for many minutes: show m:ss there, plain seconds for a single question.
        label = seconds >= 60 ? (seconds / 60) + ":" + String.format(Locale.ROOT, "%02d", seconds % 60) : String.valueOf(seconds);
        text.setTextSize(UiUtils.dp(getContext(), label.length() > 3 ? 11 : 14));
        setContentDescription(seconds + " s");
        invalidate();
    }

    private int currentColor() {
        if (fraction > 0.5f) return colorOk;
        if (fraction > 0.2f) return (int) evaluator.evaluate((0.5f - fraction) / 0.3f, colorOk, colorWarn);
        return (int) evaluator.evaluate(Math.min(1f, (0.2f - fraction) / 0.1f), colorWarn, colorDanger);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = track.getStrokeWidth() / 2f + 1;
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawArc(bounds, 0, 360, false, track);
        int color = currentColor();
        arc.setColor(color);
        if (fraction > 0) canvas.drawArc(bounds, -90, 360f * fraction, false, arc);
        text.setColor(fraction <= 0.2f ? color : UiUtils.color(getContext(), com.google.android.material.R.attr.colorOnSurface));
        float y = getHeight() / 2f - (text.descent() + text.ascent()) / 2f;
        canvas.drawText(label, getWidth() / 2f, y, text);
    }
}
