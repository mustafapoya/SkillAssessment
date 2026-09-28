package net.golbarg.skillassessment.ui.question;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ViewOptionBinding;
import net.golbarg.skillassessment.util.ContentRenderer;
import net.golbarg.skillassessment.util.UiUtils;

/** A selectable answer card with a letter badge and correct/wrong reveal states. */
public class OptionView extends MaterialCardView {

    public enum State { NORMAL, SELECTED, CORRECT, WRONG, MISSED, DIMMED }

    private final ViewOptionBinding binding;
    private State state = State.NORMAL;
    private String letter = "";

    public OptionView(@NonNull Context context) {
        this(context, null);
    }

    public OptionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = ViewOptionBinding.inflate(LayoutInflater.from(context), this);
        setCardElevation(0);
        setRadius(UiUtils.dp(context, 16));
        setClickable(true);
        setFocusable(true);
        setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, R.animator.press_scale));
        applyState();
    }

    /** Horizontal "no" shake for a wrong pick. */
    public void shake() {
        float d = UiUtils.dp(getContext(), 1);
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0, -12 * d, 10 * d, -7 * d, 5 * d, -2 * d, 0);
        anim.setDuration(420);
        anim.start();
    }

    /** A small bounce to celebrate the right answer. */
    public void pulse() {
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(this,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.03f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.03f, 1f));
        anim.setDuration(360);
        anim.setInterpolator(new OvershootInterpolator());
        anim.start();
    }

    public void bind(String letter, String text, String categorySlug) {
        this.letter = letter;
        binding.txtLetter.setText(letter);
        ContentRenderer.render(binding.content, text, categorySlug, ContentRenderer.Style.BODY, this);
        setState(State.NORMAL);
    }

    public LinearLayout getContentContainer() {
        return binding.content;
    }

    public void setState(State state) {
        this.state = state;
        applyState();
    }

    public State getState() {
        return state;
    }

    private void applyState() {
        Context c = getContext();
        int stroke = UiUtils.color(this, com.google.android.material.R.attr.colorOutlineVariant);
        int background = UiUtils.color(this, com.google.android.material.R.attr.colorSurfaceContainerLowest);
        int badgeBg = UiUtils.color(this, com.google.android.material.R.attr.colorSurfaceContainerHighest);
        int badgeFg = UiUtils.color(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        int strokeWidth = UiUtils.dp(c, 1);
        int icon = 0;
        int iconTint = 0;
        float alpha = 1f;
        String stateDescription = "";

        switch (state) {
            case SELECTED:
                stroke = UiUtils.color(this, androidx.appcompat.R.attr.colorPrimary);
                background = UiUtils.color(this, com.google.android.material.R.attr.colorSurfaceContainerHigh);
                badgeBg = stroke;
                badgeFg = UiUtils.color(this, com.google.android.material.R.attr.colorOnPrimary);
                strokeWidth = UiUtils.dp(c, 2);
                stateDescription = "selected";
                break;
            case MISSED:
                // The right answer the user did not pick: outlined, not filled.
                stroke = ContextCompat.getColor(c, R.color.correct);
                badgeBg = stroke;
                badgeFg = ContextCompat.getColor(c, R.color.correct_container);
                strokeWidth = UiUtils.dp(c, 2);
                icon = R.drawable.ic_check_circle;
                iconTint = stroke;
                stateDescription = c.getString(R.string.correct);
                break;
            case CORRECT:
                stroke = ContextCompat.getColor(c, R.color.correct);
                background = ContextCompat.getColor(c, R.color.correct_container);
                badgeBg = stroke;
                badgeFg = ContextCompat.getColor(c, R.color.correct_container);
                strokeWidth = UiUtils.dp(c, 2);
                icon = R.drawable.ic_check_circle;
                iconTint = stroke;
                stateDescription = c.getString(R.string.correct);
                break;
            case WRONG:
                stroke = ContextCompat.getColor(c, R.color.wrong);
                background = ContextCompat.getColor(c, R.color.wrong_container);
                badgeBg = stroke;
                badgeFg = ContextCompat.getColor(c, R.color.wrong_container);
                strokeWidth = UiUtils.dp(c, 2);
                icon = R.drawable.ic_cancel;
                iconTint = stroke;
                stateDescription = c.getString(R.string.wrong);
                break;
            case DIMMED:
                alpha = 0.55f;
                break;
            default:
                break;
        }

        setStrokeColor(stroke);
        setStrokeWidth(strokeWidth);
        setCardBackgroundColor(background);
        binding.txtLetter.setBackgroundTintList(ColorStateList.valueOf(badgeBg));
        binding.txtLetter.setTextColor(badgeFg);
        setAlpha(alpha);
        if (icon != 0) {
            binding.imgState.setVisibility(View.VISIBLE);
            binding.imgState.setImageResource(icon);
            binding.imgState.setImageTintList(ColorStateList.valueOf(iconTint));
        } else {
            binding.imgState.setVisibility(View.GONE);
        }
        setContentDescription(c.getString(R.string.answer_label) + " " + letter + (stateDescription.isEmpty() ? "" : ", " + stateDescription));
    }
}
