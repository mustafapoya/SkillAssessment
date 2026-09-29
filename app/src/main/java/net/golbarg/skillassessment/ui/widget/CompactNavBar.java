package net.golbarg.skillassessment.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.widget.TextViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.ui.NavigationUI;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom navigation with the label beside the icon, shown only on the selected tab. The selected
 * tab grows to fit its label and the others share the remaining width, which keeps the bar low.
 *
 * <p>Material's BottomNavigationView can also put labels beside icons, but it caps every tab at a
 * quarter of the width, which cuts longer labels ("Progr…") on phones.
 */
public class CompactNavBar extends LinearLayout {
    private static final int PILL_HEIGHT_DP = 40;
    private static final int VERTICAL_PADDING_DP = 10;
    private static final long ANIMATION_MS = 200;

    private static final class Tab {
        final MenuItem item;
        final FrameLayout slot;
        final LinearLayout pill;
        final MaterialShapeDrawable pillBackground;
        final ImageView icon;
        final TextView label;

        Tab(MenuItem item, FrameLayout slot, LinearLayout pill, MaterialShapeDrawable pillBackground, ImageView icon, TextView label) {
            this.item = item;
            this.slot = slot;
            this.pill = pill;
            this.pillBackground = pillBackground;
            this.icon = icon;
            this.label = label;
        }
    }

    private final List<Tab> tabs = new ArrayList<>();
    private final int selectedContainer;
    private final int selectedContent;
    private final int unselectedContent;
    @Nullable private NavController navController;
    private int selected = -1;

    public CompactNavBar(@NonNull Context context) {
        this(context, null);
    }

    public CompactNavBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int vertical = UiUtils.dp(context, VERTICAL_PADDING_DP);
        int horizontal = UiUtils.dp(context, 8);
        setPadding(horizontal, vertical, horizontal, vertical);
        setBackgroundColor(UiUtils.color(context, com.google.android.material.R.attr.colorSurfaceContainer));
        selectedContainer = UiUtils.color(context, com.google.android.material.R.attr.colorPrimaryContainer);
        selectedContent = UiUtils.color(context, com.google.android.material.R.attr.colorOnPrimaryContainer);
        unselectedContent = UiUtils.color(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
        // Content stays clear of the gesture/button navigation bar, whose colour continues ours.
        UiUtils.applySystemBarPadding(this, false, true);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CompactNavBar);
        int menuRes = a.getResourceId(R.styleable.CompactNavBar_menu, 0);
        a.recycle();
        if (menuRes != 0) inflateMenu(menuRes);
    }

    /** Builds one tab per item of the menu resource (id, icon and title are used). */
    private void inflateMenu(int menuRes) {
        // PopupMenu is only used as a public way to inflate a Menu; it is never shown.
        PopupMenu parser = new PopupMenu(getContext(), this);
        parser.inflate(menuRes);
        Menu menu = parser.getMenu();
        for (int i = 0; i < menu.size(); i++) addTab(menu.getItem(i));
        applySelection();
    }

    private void addTab(MenuItem item) {
        Context context = getContext();
        int index = tabs.size();

        ImageView icon = new ImageView(context);
        icon.setImageDrawable(item.getIcon());
        icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        int iconSize = UiUtils.dp(context, 24);

        TextView label = new TextView(context);
        TextViewCompat.setTextAppearance(label, R.style.TextAppearance_App_NavLabel_Active);
        label.setText(item.getTitle());
        label.setSingleLine(true);
        label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(UiUtils.dp(context, 16), 0, UiUtils.dp(context, 16), 0);
        MaterialShapeDrawable background = new MaterialShapeDrawable(ShapeAppearanceModel.builder()
                .setAllCornerSizes(UiUtils.dp(context, PILL_HEIGHT_DP) / 2f)
                .build());
        pill.setBackground(background);
        pill.addView(icon, new LayoutParams(iconSize, iconSize));
        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.setMarginStart(UiUtils.dp(context, 8));
        pill.addView(label, labelParams);

        FrameLayout slot = new FrameLayout(context);
        slot.setClickable(true);
        slot.setFocusable(true);
        TypedValue ripple = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true);
        slot.setForeground(AppCompatResources.getDrawable(context, ripple.resourceId));
        slot.setContentDescription(item.getTitle());
        // Unselected tabs show no label, so a long press explains the icon.
        TooltipCompat.setTooltipText(slot, item.getTitle());
        slot.addView(pill, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                UiUtils.dp(context, PILL_HEIGHT_DP), Gravity.CENTER));
        slot.setOnClickListener(v -> {
            if (index == selected || navController == null) return;
            NavigationUI.onNavDestinationSelected(item, navController);
        });

        addView(slot, new LayoutParams(0, UiUtils.dp(context, PILL_HEIGHT_DP), 1f));
        tabs.add(new Tab(item, slot, pill, background, icon, label));
    }

    /** Navigates on taps and follows the controller's destination, including back presses. */
    public void setupWithNavController(@NonNull NavController controller) {
        navController = controller;
        controller.addOnDestinationChangedListener((c, destination, arguments) -> {
            for (int i = 0; i < tabs.size(); i++) {
                if (matches(destination, tabs.get(i).item.getItemId())) {
                    select(i);
                    return;
                }
            }
        });
    }

    private static boolean matches(NavDestination destination, int id) {
        for (NavDestination d = destination; d != null; d = d.getParent()) {
            if (d.getId() == id) return true;
        }
        return false;
    }

    private void select(int index) {
        if (index == selected) return;
        // The first selection happens while the screen is being set up; only later ones animate.
        if (selected >= 0 && isLaidOut()) {
            TransitionManager.beginDelayedTransition(this, new AutoTransition().setDuration(ANIMATION_MS));
        }
        selected = index;
        applySelection();
    }

    private void applySelection() {
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean on = i == selected;
            tab.label.setVisibility(on ? View.VISIBLE : View.GONE);
            tab.label.setTextColor(selectedContent);
            tab.icon.setImageTintList(ColorStateList.valueOf(on ? selectedContent : unselectedContent));
            tab.pillBackground.setFillColor(ColorStateList.valueOf(on ? selectedContainer : 0));
            tab.slot.setSelected(on);
            // The selected tab takes the width its label needs; the others share what is left.
            LayoutParams lp = (LayoutParams) tab.slot.getLayoutParams();
            lp.width = on ? LayoutParams.WRAP_CONTENT : 0;
            lp.weight = on ? 0f : 1f;
            tab.slot.setLayoutParams(lp);
        }
    }
}
