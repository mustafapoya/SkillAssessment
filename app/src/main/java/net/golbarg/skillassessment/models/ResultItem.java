package net.golbarg.skillassessment.models;

import java.util.ArrayList;
import java.util.List;

/** How the user answered one question of a finished test. */
public class ResultItem {
    private final int questionId;
    private final List<Integer> selectedPositions;
    private final AnswerResponseType outcome;
    private Question question;

    public ResultItem(int questionId, List<Integer> selectedPositions, AnswerResponseType outcome) {
        this.questionId = questionId;
        this.selectedPositions = new ArrayList<>(selectedPositions);
        this.outcome = outcome;
    }

    public int getQuestionId() { return questionId; }

    public List<Integer> getSelectedPositions() { return selectedPositions; }

    public AnswerResponseType getOutcome() { return outcome; }

    public Question getQuestion() { return question; }

    public void setQuestion(Question question) { this.question = question; }
}
