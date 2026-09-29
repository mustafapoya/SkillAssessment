package net.golbarg.skillassessment.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Runs blocking work on a background thread and delivers the result on the main thread. */
public final class Async {
    private static final String TAG = "Async";
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    /** One thread, so quick successive writes (e.g. bookmark, then undo) apply in order. */
    private static final ExecutorService WRITES = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        /** @param result the value, or null if the task threw. */
        void onResult(@Nullable T result);
    }

    private Async() {
    }

    /** The callback is dropped if {@code owner} has been destroyed by the time the work completes. */
    public static <T> void run(@Nullable LifecycleOwner owner, Callable<T> task, Callback<T> callback) {
        IO.execute(() -> {
            T value = null;
            try {
                value = task.call();
            } catch (Exception e) {
                Log.e(TAG, "Background task failed", e);
            }
            final T result = value;
            MAIN.post(() -> {
                if (owner == null || owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
                    callback.onResult(result);
                }
            });
        });
    }

    public static void io(Runnable runnable) {
        IO.execute(() -> runSafely(runnable));
    }

    /** Fire-and-forget writes that must not overtake each other; they run in submission order. */
    public static void write(Runnable runnable) {
        WRITES.execute(() -> runSafely(runnable));
    }

    private static void runSafely(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            Log.e(TAG, "Background task failed", e);
        }
    }
}
