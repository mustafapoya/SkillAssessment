package net.golbarg.skillassessment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.golbarg.skillassessment.databinding.ActivitySplashBinding;
import net.golbarg.skillassessment.ui.intro.IntroActivity;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.UiUtils;

/**
 * Splash activity: displays the branded splash screen on cold start
 * without showing the OS launcher icon, then routes to onboarding or main.
 */
public class SplashScreenActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1800;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        UiUtils.enableEdgeToEdge(this);
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Display version from build config
        binding.txtSplashVersion.setText(getString(R.string.splash_version, BuildConfig.VERSION_NAME));

        // Entrance animation for center brand
        binding.layoutCenterBrand.setAlpha(0f);
        binding.layoutCenterBrand.setScaleX(0.85f);
        binding.layoutCenterBrand.setScaleY(0.85f);
        binding.layoutCenterBrand.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Subtle fade-in for bottom indicator and version
        binding.layoutSplashBottom.setAlpha(0f);
        binding.layoutSplashBottom.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(250)
                .start();

        // Delay then proceed to main or intro
        handler.postDelayed(this::proceedToNextScreen, SPLASH_DURATION_MS);
    }

    private void proceedToNextScreen() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Class<?> target = Prefs.isIntroSeen(this) ? MainActivity.class : IntroActivity.class;
        Intent intent = new Intent(this, target);
        startActivity(intent);
        fadeToNextScreen();
        finish();
    }

    /** Deprecated since API 34 in favour of overrideActivityTransition, but still honoured. */
    @SuppressWarnings("deprecation")
    private void fadeToNextScreen() {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
