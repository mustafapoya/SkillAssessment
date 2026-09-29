package net.golbarg.skillassessment.models;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A multiple-choice question; answers are kept in display order. */
public class Question {
    private final int id;
    private final int categoryId;
    private final int number;
    private final String title;
    private final int numberOfCorrectAnswer;
    private final List<QuestionAnswer> answers = new ArrayList<>();
    @Nullable private String explanation;

    public Question(int id, int categoryId, int number, String title, int numberOfCorrectAnswer) {
        this.id = id;
        this.categoryId = categoryId;
        this.number = number;
        this.title = title;
        this.numberOfCorrectAnswer = numberOfCorrectAnswer;
    }

    public int getId() { return id; }

    public int getCategoryId() { return categoryId; }

    public int getNumber() { return number; }

    public String getTitle() { return title; }

    public List<QuestionAnswer> getAnswers() { return answers; }

    /** Why the correct answer is correct; null until the content provides one. */
    @Nullable
    public String getExplanation() { return explanation; }

    public void setExplanation(@Nullable String explanation) {
        this.explanation = explanation == null || explanation.trim().isEmpty() ? null : explanation.trim();
    }

    /** Positions (0-based, in display order) of the correct answers. */
    public Set<Integer> getCorrectPositions() {
        Set<Integer> result = new LinkedHashSet<>();
        for (int i = 0; i < answers.size(); i++) {
            if (answers.get(i).isCorrect()) result.add(i);
        }
        return result;
    }

    /** How many answers the user has to pick. Falls back to the metadata if no answer is flagged. */
    public int getRequiredSelections() {
        int actual = getCorrectPositions().size();
        return Math.max(1, actual > 0 ? actual : numberOfCorrectAnswer);
    }
}
