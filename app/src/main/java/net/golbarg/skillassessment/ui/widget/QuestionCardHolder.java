package net.golbarg.skillassessment.ui.widget;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.skillassessment.databinding.ItemQuestionCardBinding;

/** View holder for {@code item_question_card}, shared by every list of question cards. */
public final class QuestionCardHolder extends RecyclerView.ViewHolder {
    public final ItemQuestionCardBinding binding;

    private QuestionCardHolder(ItemQuestionCardBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public static QuestionCardHolder create(ViewGroup parent) {
        return new QuestionCardHolder(ItemQuestionCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }
}
