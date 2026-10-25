package net.golbarg.skillassessment.util;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.StringRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import net.golbarg.skillassessment.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class UiUtils {
    private UiUtils() {
    }

    /**
     * Draws the app behind fully transparent status and navigation bars. Android would otherwise add
     * a translucent scrim behind 3-button navigation, making it a different colour from the app's
     * own bottom surfaces (bottom navigation, action panels). setStatusBarContrastEnforced is
     * deprecated on API 35+, where it has no effect, but still matters below that.
     */
    @SuppressWarnings("deprecation")
    public static void enableEdgeToEdge(ComponentActivity activity) {
        SystemBarStyle transparent = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(activity, transparent, transparent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.getWindow().setNavigationBarContrastEnforced(false);
            activity.getWindow().setStatusBarContrastEnforced(false);
        }
    }

    /** A snackbar that floats above the bottom navigation when the screen has one. */
    public static Snackbar snackbar(View anyView, CharSequence text, int duration) {
        Snackbar snackbar = Snackbar.make(anyView, text, duration);
        View nav = anyView.getRootView().findViewById(R.id.nav_view);
        if (nav != null && nav.isShown()) snackbar.setAnchorView(nav);
        return snackbar;
    }

    public static Snackbar snackbar(View anyView, @StringRes int text, int duration) {
        return snackbar(anyView, anyView.getContext().getText(text), duration);
    }

    public static int color(View view, @AttrRes int attr) {
        return MaterialColors.getColor(view, attr);
    }

    public static int color(Context context, @AttrRes int attr) {
        return MaterialColors.getColor(context, attr, 0);
    }

    public static int dp(Context context, float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    public static boolean isNightMode(Context context) {
        int flags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return flags == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Adds the system bar (and display cutout) insets to the view's existing padding. Used with
     * edge-to-edge so content never sits under the status or navigation bar.
     */
    public static void applySystemBarPadding(View view, boolean top, boolean bottom) {
        final int left = view.getPaddingLeft();
        final int topPadding = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottomPadding = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int side = sideExtra(v);
            v.setPadding(left + bars.left + side, topPadding + (top ? bars.top : 0), right + bars.right + side,
                    bottomPadding + (bottom ? bars.bottom : 0));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /** {@link #limitContentWidth(View, int)} with the standard reading width. */
    public static void limitContentWidth(View view) {
        limitContentWidth(view, R.dimen.content_max_width);
    }

    /**
     * Keeps a screen readable on tablets and in landscape: once {@code view} is wider than
     * {@code maxWidth}, its side padding grows so the content stays that wide and centred. Because
     * it is padding, a scrolling view still scrolls across its full width. Works together with
     * {@link #applySystemBarPadding}.
     */
    public static void limitContentWidth(View view, @DimenRes int maxWidth) {
        int max = view.getResources().getDimensionPixelSize(maxWidth);
        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            // Padding changed during a layout pass isn't always laid out again (the children keep
            // their old places and get clipped), so it is applied right after this pass instead.
            if (sideExtraFor(v, max) != sideExtra(v)) v.post(() -> applySideExtra(v, max));
        });
    }

    private static int sideExtraFor(View view, int maxWidth) {
        return Math.max(0, (view.getWidth() - maxWidth) / 2);
    }

    private static void applySideExtra(View view, int maxWidth) {
        int extra = sideExtraFor(view, maxWidth);
        int applied = sideExtra(view);
        if (extra == applied) return;
        view.setTag(R.id.tag_content_side_extra, extra);
        view.setPadding(view.getPaddingLeft() - applied + extra, view.getPaddingTop(),
                view.getPaddingRight() - applied + extra, view.getPaddingBottom());
    }

    private static int sideExtra(View view) {
        Object extra = view.getTag(R.id.tag_content_side_extra);
        return extra instanceof Integer ? (Integer) extra : 0;
    }

    /** "1:05" below an hour, "1h 05m" above. */
    public static String formatDuration(long millis) {
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes >= 60) return String.format(Locale.getDefault(), "%dh %02dm", minutes / 60, minutes % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    /** "1 coin", "5 coins". */
    public static String coins(Context context, int count) {
        return context.getResources().getQuantityString(R.plurals.coin_count, count, count);
    }

    public static String formatDate(long epochMillis) {
        if (epochMillis <= 0) return "—";
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(epochMillis));
    }
}
