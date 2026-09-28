package net.golbarg.skillassessment.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** A lightweight, non-interactive confetti burst drawn on top of the result screen. */
public class ConfettiView extends View {
    private static final int[] COLORS = {0xFF7ED321, 0xFF57CC02, 0xFFFFC940, 0xFF4FC3F7, 0xFFFF7A59, 0xFFB388FF};
    private static final long DURATION_MS = 2800;

    private static final class Piece {
        float x, y, vx, vy, rotation, spin, width, height, sway, phase;
        int color;
        boolean circle;
    }

    private final List<Piece> pieces = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private ValueAnimator animator;
    private float elapsed;

    public ConfettiView(Context context) {
        this(context, null);
    }

    public ConfettiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** @param count number of pieces, e.g. 80 for a good score and 150 for an excellent one. */
    public void burst(int count) {
        post(() -> start(count));
    }

    private void start(int count) {
        if (getWidth() == 0) return;
        float density = getResources().getDisplayMetrics().density;
        pieces.clear();
        for (int i = 0; i < count; i++) {
            Piece p = new Piece();
            p.x = getWidth() * (0.5f + (random.nextFloat() - 0.5f) * 0.3f);
            p.y = getHeight() * 0.28f;
            double angle = Math.toRadians(-90 + (random.nextFloat() - 0.5f) * 140);
            float speed = (900 + random.nextFloat() * 900) * density / 3f;
            p.vx = (float) (Math.cos(angle) * speed);
            p.vy = (float) (Math.sin(angle) * speed);
            p.rotation = random.nextFloat() * 360;
            p.spin = (random.nextFloat() - 0.5f) * 720;
            p.width = (5 + random.nextFloat() * 5) * density;
            p.height = p.width * (0.4f + random.nextFloat() * 0.6f);
            p.sway = (10 + random.nextFloat() * 20) * density;
            p.phase = random.nextFloat() * 6.28f;
            p.color = COLORS[random.nextInt(COLORS.length)];
            p.circle = random.nextInt(4) == 0;
            pieces.add(p);
        }
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, DURATION_MS / 1000f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            elapsed = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pieces.isEmpty() || animator == null || !animator.isRunning()) return;
        float t = elapsed;
        float gravity = 1400 * getResources().getDisplayMetrics().density / 3f;
        float fadeStart = DURATION_MS / 1000f * 0.7f;
        int alpha = t < fadeStart ? 255 : (int) (255 * Math.max(0, 1 - (t - fadeStart) / (DURATION_MS / 1000f - fadeStart)));
        for (Piece p : pieces) {
            // Air drag slows the burst so pieces flutter down instead of falling like stones.
            float drag = (float) Math.exp(-1.6 * t);
            float x = p.x + p.vx * (1 - drag) / 1.6f + (float) Math.sin(p.phase + t * 5) * p.sway * Math.min(1, t);
            float y = p.y + p.vy * (1 - drag) / 1.6f + 0.5f * gravity * t * t * 0.22f;
            paint.setColor(p.color);
            paint.setAlpha(alpha);
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(p.rotation + p.spin * t);
            if (p.circle) {
                canvas.drawCircle(0, 0, p.height / 2f, paint);
            } else {
                // Squash horizontally over time to fake a 3D flip.
                canvas.scale((float) Math.abs(Math.cos(p.phase + t * 8)) * 0.8f + 0.2f, 1f);
                canvas.drawRect(-p.width / 2f, -p.height / 2f, p.width / 2f, p.height / 2f, paint);
            }
            canvas.restore();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }
}
