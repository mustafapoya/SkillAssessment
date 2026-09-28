package net.golbarg.skillassessment.ui.widget;

import android.graphics.Color;
import android.os.Build;
import android.view.Window;

import androidx.core.view.WindowCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet whose surface runs behind the navigation bar. A dialog has its own window, and
 * Android would otherwise draw a darker contrast scrim behind 3-button navigation.
 */
public abstract class AppBottomSheet extends BottomSheetDialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null) return;
        Window window = getDialog().getWindow();
        if (window == null) return;
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
    }
}
