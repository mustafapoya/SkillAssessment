package net.golbarg.skillassessment.ui.widget;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.snackbar.Snackbar;

import net.golbarg.skillassessment.BuildConfig;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ItemQuestionCardBinding;
import net.golbarg.skillassessment.models.AnswerResponseType;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.ResultItem;
import net.golbarg.skillassessment.ui.question.OptionView;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.util.AppLinks;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.ContentRenderer;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.List;
import java.util.Set;

/** Fills {@code item_question_card} for review and saved-question lists. */
public final class QuestionCardBinder {
    private QuestionCardBinder() {
    }

    public static void bindReview(ItemQuestionCardBinding b, ResultItem item, int position, String slug, boolean isBookmarked, View.OnClickListener onToggleBookmark) {
        Context context = b.getRoot().getContext();
        Question question = item.getQuestion();
        b.txtOverline.setText(context.getString(R.string.question_label, position + 1));

        int color;
        int label;
        int icon;
        if (item.getOutcome() == AnswerResponseType.CORRECT) {
            color = ContextCompat.getColor(context, R.color.correct);
            label = R.string.correct;
            icon = R.drawable.ic_check_circle;
        } else if (item.getOutcome() == AnswerResponseType.WRONG) {
            color = ContextCompat.getColor(context, R.color.wrong);
            label = R.string.wrong;
            icon = R.drawable.ic_cancel;
        } else {
            color = ContextCompat.getColor(context, R.color.skipped);
            label = R.string.skipped;
            icon = R.drawable.ic_skip_circle;
        }
        b.txtStatus.setVisibility(View.VISIBLE);
        b.txtStatus.setText(label);
        b.txtStatus.setTextColor(color);
        b.txtStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(b.txtStatus, ColorStateList.valueOf(color));

        b.btnMore.setOnClickListener(v -> showMenu(v, question, slug));
        bindBookmark(b, isBookmarked, onToggleBookmark);

        ContentRenderer.render(b.questionContent, question.getTitle(), slug, ContentRenderer.Style.BODY, null);

        b.answers.removeAllViews();
        Set<Integer> correct = question.getCorrectPositions();
        List<Integer> selected = item.getSelectedPositions();
        for (int i = 0; i < question.getAnswers().size(); i++) {
            boolean isCorrect = correct.contains(i);
            boolean isSelected = selected.contains(i);
            if (!isCorrect && !isSelected) continue;
            OptionView.State state = isCorrect ? (isSelected ? OptionView.State.CORRECT : OptionView.State.MISSED) : OptionView.State.WRONG;
            addAnswer(b.answers, i, question.getAnswers().get(i).getTitle(), slug, state);
        }
        bindExplanation(b, question, slug, true);
    }

    public static void bindSaved(ItemQuestionCardBinding b, Question question, String categoryName, String slug, View.OnClickListener onRemove) {
        Context context = b.getRoot().getContext();
        b.txtOverline.setText(categoryName);
        b.txtStatus.setVisibility(View.GONE);
        b.btnMore.setOnClickListener(v -> showMenu(v, question, slug));
        b.btnAction.setVisibility(View.VISIBLE);
        b.btnAction.setIconResource(R.drawable.ic_bookmark);
        b.btnAction.setIconTint(ColorStateList.valueOf(UiUtils.color(context, androidx.appcompat.R.attr.colorPrimary)));
        b.btnAction.setContentDescription(context.getString(R.string.bookmark_remove));
        b.btnAction.setOnClickListener(onRemove);

        ContentRenderer.render(b.questionContent, question.getTitle(), slug, ContentRenderer.Style.BODY, null);
        b.answers.removeAllViews();
        for (Integer i : question.getCorrectPositions()) {
            addAnswer(b.answers, i, question.getAnswers().get(i).getTitle(), slug, OptionView.State.CORRECT);
        }
        bindExplanation(b, question, slug, true);
    }

    /**
     * Flashcard for study mode: every option is listed, and the correct ones are only highlighted
     * once {@code revealed}. Tapping the card toggles the answer.
     */
    public static void bindStudy(ItemQuestionCardBinding b, Question question, String overline, String slug, boolean revealed,
                                 boolean isBookmarked, View.OnClickListener onToggleReveal, View.OnClickListener onToggleBookmark) {
        Context context = b.getRoot().getContext();
        b.txtOverline.setText(overline);
        int color = revealed ? UiUtils.color(context, com.google.android.material.R.attr.colorOnSurfaceVariant)
                : UiUtils.color(context, androidx.appcompat.R.attr.colorPrimary);
        b.txtStatus.setVisibility(View.VISIBLE);
        b.txtStatus.setText(revealed ? R.string.study_hide_answer : R.string.study_tap_to_reveal);
        b.txtStatus.setTextColor(color);
        b.txtStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(revealed ? R.drawable.ic_visibility_off : R.drawable.ic_visibility, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(b.txtStatus, ColorStateList.valueOf(color));
        b.btnMore.setOnClickListener(v -> showMenu(v, question, slug));
        bindBookmark(b, isBookmarked, onToggleBookmark);
        b.getRoot().setOnClickListener(onToggleReveal);

        ContentRenderer.render(b.questionContent, question.getTitle(), slug, ContentRenderer.Style.BODY, null);
        b.answers.removeAllViews();
        Set<Integer> correct = question.getCorrectPositions();
        for (int i = 0; i < question.getAnswers().size(); i++) {
            OptionView.State state = !revealed ? OptionView.State.NORMAL
                    : correct.contains(i) ? OptionView.State.CORRECT : OptionView.State.DIMMED;
            addAnswer(b.answers, i, question.getAnswers().get(i).getTitle(), slug, state);
        }
        bindExplanation(b, question, slug, revealed);
    }

    private static void bindExplanation(ItemQuestionCardBinding b, Question question, String slug, boolean show) {
        String explanation = question.getExplanation();
        boolean visible = show && explanation != null;
        b.explanation.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) ContentRenderer.render(b.explanationContent, explanation, slug, ContentRenderer.Style.BODY, null);
    }

    private static void bindBookmark(ItemQuestionCardBinding b, boolean isBookmarked, View.OnClickListener onToggle) {
        Context context = b.getRoot().getContext();
        if (onToggle == null) {
            b.btnAction.setVisibility(View.GONE);
            return;
        }
        b.btnAction.setVisibility(View.VISIBLE);
        b.btnAction.setIconResource(isBookmarked ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
        b.btnAction.setIconTint(ColorStateList.valueOf(isBookmarked
                ? UiUtils.color(context, androidx.appcompat.R.attr.colorPrimary)
                : UiUtils.color(context, com.google.android.material.R.attr.colorOnSurfaceVariant)));
        b.btnAction.setContentDescription(context.getString(isBookmarked ? R.string.bookmark_remove : R.string.bookmark_add));
        b.btnAction.setOnClickListener(onToggle);
    }

    /** Overflow menu shared by every question card: copy, or report a problem by email. */
    private static void showMenu(View anchor, Question question, String slug) {
        PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
        menu.getMenu().add(0, 1, 0, R.string.copy_question);
        menu.getMenu().add(0, 2, 1, R.string.report_question);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) copyQuestion(anchor.getContext(), anchor, question);
            else reportQuestion(anchor.getContext(), question, slug);
            return true;
        });
        menu.show();
    }

    private static String plainText(Question question) {
        StringBuilder sb = new StringBuilder();
        sb.append(question.getTitle()).append("\n\n");
        Set<Integer> correct = question.getCorrectPositions();
        for (int i = 0; i < question.getAnswers().size(); i++) {
            sb.append(QuestionActivity.letter(i)).append(") ").append(question.getAnswers().get(i).getTitle());
            if (correct.contains(i)) sb.append(" ✓");
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /** Opens an email to support prefilled with the question, so content mistakes get fixed. */
    public static void reportQuestion(Context context, Question question, String slug) {
        String topic = CategoryNames.displayName(slug);
        Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + AppLinks.EMAIL))
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_question_subject, topic, question.getId()))
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.report_question_body, topic, question.getId(),
                        BuildConfig.VERSION_NAME, plainText(question)));
        try {
            context.startActivity(mail);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_SHORT).show();
        }
    }

    public static void copyQuestion(Context context, View anchor, Question question) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;
        cm.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), plainText(question)));
        Feedback.haptic(anchor, Feedback.Haptic.LIGHT);
        // Android 13+ shows its own clipboard confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Snackbar.make(anchor, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
        }
    }

    private static void addAnswer(LinearLayout container, int position, String text, String slug, OptionView.State state) {
        Context context = container.getContext();
        OptionView option = new OptionView(context);
        option.bind(QuestionActivity.letter(position), text, slug);
        option.setState(state);
        option.setClickable(false);
        option.setFocusable(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (container.getChildCount() > 0) lp.topMargin = UiUtils.dp(context, 8);
        container.addView(option, lp);
    }
}
