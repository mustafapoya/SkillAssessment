package net.golbarg.skillassessment.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

import net.golbarg.skillassessment.R;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sound effects and haptics, both honouring the user's settings. */
public final class Feedback {

    public enum Sound {
        TAP(R.raw.sfx_tap, 1f),
        CORRECT(R.raw.sfx_correct, 1f),
        WRONG(R.raw.sfx_wrong, 0.9f),
        TICK(R.raw.sfx_tick, 0.8f),
        TIMEOUT(R.raw.sfx_timeout, 0.9f),
        COMPLETE(R.raw.sfx_complete, 1f),
        COIN(R.raw.sfx_coin, 1f),
        UNLOCK(R.raw.sfx_unlock, 1f);

        final int res;
        final float volume;

        Sound(int res, float volume) {
            this.res = res;
            this.volume = volume;
        }
    }

    public enum Haptic { LIGHT, SUCCESS, ERROR }

    private static SoundPool pool;
    private static final Map<Sound, Integer> ids = new EnumMap<>(Sound.class);
    /** Sample id → loaded; filled in by SoundPool's loader thread. */
    private static final Map<Integer, Boolean> ready = new ConcurrentHashMap<>();
    private static Context appContext;

    private Feedback() {
    }

    /** Loads all sounds; call once from the Application. */
    public static synchronized void init(Context context) {
        if (pool != null) return;
        appContext = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build();
        pool.setOnLoadCompleteListener((soundPool, sampleId, status) -> ready.put(sampleId, status == 0));
        for (Sound sound : Sound.values()) {
            ids.put(sound, pool.load(appContext, sound.res, 1));
        }
    }

    public static void play(Sound sound) {
        if (pool == null || appContext == null || !Prefs.isSoundEnabled(appContext)) return;
        Integer id = ids.get(sound);
        if (id == null || !Boolean.TRUE.equals(ready.get(id))) return;
        pool.play(id, sound.volume, sound.volume, 1, 0, 1f);
    }

    public static void haptic(View view, Haptic type) {
        if (view == null || !Prefs.isVibrationEnabled(view.getContext())) return;
        int constant;
        switch (type) {
            case SUCCESS:
                constant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY;
                break;
            case ERROR:
                constant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS;
                break;
            default:
                constant = HapticFeedbackConstants.CLOCK_TICK;
                break;
        }
        view.performHapticFeedback(constant);
    }
}
