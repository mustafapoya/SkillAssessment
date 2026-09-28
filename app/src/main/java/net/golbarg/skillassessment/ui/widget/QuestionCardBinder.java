package net.golbarg.skillassessment.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ItemQuestionCardBinding;
import net.golbarg.skillassessment.models.AnswerResponseType;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.ResultItem;
import net.golbarg.skillassessment.ui.question.OptionView;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.util.ContentRenderer;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.List;
import java.util.Set;

/** Fills {@code item_question_card} for review and saved-question lists. */
public final class QuestionCardBinder {
    private QuestionCardBinder() {
    }

    public static void bindReview(ItemQuestionCardBinding b, ResultItem item, int position, String slug) {
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
        b.btnAction.setVisibility(View.GONE);

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
    }

    public static void bindSaved(ItemQuestionCardBinding b, Question question, String categoryName, String slug, View.OnClickListener onRemove) {
        Context context = b.getRoot().getContext();
        b.txtOverline.setText(categoryName);
        b.txtStatus.setVisibility(View.GONE);
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
    }

    private static void addAnswer(LinearLayout container, int position, String text, String slug, OptionView.State state) {
        Context context = container.getContext();
        OptionView option = new OptionView(context);
        String letter = position < QuestionActivity.LETTERS.length ? QuestionActivity.LETTERS[position] : String.valueOf(position + 1);
        option.bind(letter, text, slug);
        option.setState(state);
        option.setClickable(false);
        option.setFocusable(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (container.getChildCount() > 0) lp.topMargin = UiUtils.dp(context, 8);
        container.addView(option, lp);
    }
}
