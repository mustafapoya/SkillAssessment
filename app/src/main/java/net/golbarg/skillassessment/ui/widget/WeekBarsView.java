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

import net.golbarg.skillassessment.util.UiUtils;

/**
 * Seven vertical bars, oldest day first and today last. Today's bar uses the primary colour, the
 * others a quieter tone; empty days show a short stub so the week still reads as seven slots.
 */
public class WeekBarsView extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int primary;
    private final int secondary;
    private final int empty;
    private final int labelColor;
    private final int todayLabelColor;
    private int[] counts = new int[7];
    private String[] labels = new String[7];
    private float progress = 1f;

    public WeekBarsView(Context context) {
        this(context, null);
    }

    public WeekBarsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        primary = UiUtils.color(context, androidx.appcompat.R.attr.colorPrimary);
        secondary = UiUtils.color(context, com.google.android.material.R.attr.colorSecondary);
        empty = UiUtils.color(context, com.google.android.material.R.attr.colorSurfaceContainerHighest);
        labelColor = UiUtils.color(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
        todayLabelColor = UiUtils.color(context, com.google.android.material.R.attr.colorOnSurface);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(UiUtils.dp(context, 12));
        value.setTextAlign(Paint.Align.CENTER);
        value.setTextSize(UiUtils.dp(context, 11));
        value.setFakeBoldText(true);
    }

    /** @param counts seven values, oldest first; @param labels matching short day names. */
    public void setData(int[] counts, String[] labels, boolean animate) {
        this.counts = counts;
        this.labels = labels;
        StringBuilder cd = new StringBuilder();
        for (int i = 0; i < counts.length; i++) cd.append(labels[i]).append(' ').append(counts[i]).append(i < counts.length - 1 ? ", " : "");
        setContentDescription(cd);
        if (animate) {
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(700);
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = resolveSize(UiUtils.dp(getContext(), 140), heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = counts.length;
        if (n == 0) return;
        float labelArea = UiUtils.dp(getContext(), 22);
        float valueArea = UiUtils.dp(getContext(), 16);
        float chartTop = getPaddingTop() + valueArea;
        float chartBottom = getHeight() - getPaddingBottom() - labelArea;
        float chartHeight = chartBottom - chartTop;
        float slot = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) n;
        float barWidth = Math.min(slot * 0.55f, UiUtils.dp(getContext(), 28));
        float radius = barWidth / 2f;
        float stub = UiUtils.dp(getContext(), 4);

        int max = 1;
        for (int c : counts) max = Math.max(max, c);

        for (int i = 0; i < n; i++) {
            boolean today = i == n - 1;
            float cx = getPaddingLeft() + slot * i + slot / 2f;
            float h = counts[i] == 0 ? stub : Math.max(barWidth, chartHeight * counts[i] / max * progress);
            rect.set(cx - barWidth / 2f, chartBottom - h, cx + barWidth / 2f, chartBottom);
            bar.setColor(counts[i] == 0 ? empty : today ? primary : secondary);
            if (counts[i] > 0 && !today) bar.setAlpha(150);
            canvas.drawRoundRect(rect, radius, radius, bar);
            bar.setAlpha(255);

            if (counts[i] > 0 && progress > 0.95f) {
                value.setColor(today ? primary : labelColor);
                canvas.drawText(String.valueOf(counts[i]), cx, rect.top - UiUtils.dp(getContext(), 5), value);
            }
            label.setColor(today ? todayLabelColor : labelColor);
            label.setFakeBoldText(today);
            canvas.drawText(labels[i] == null ? "" : labels[i], cx, getHeight() - getPaddingBottom() - UiUtils.dp(getContext(), 4), label);
        }
    }
}
