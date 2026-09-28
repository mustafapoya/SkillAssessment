package net.golbarg.skillassessment.models;

public class QuestionAnswer {
    private final int questionId;
    private final int number;
    private final String title;
    private final boolean correct;

    public QuestionAnswer(int questionId, int number, String title, boolean correct) {
        this.questionId = questionId;
        this.number = number;
        this.title = title;
        this.correct = correct;
    }

    public int getQuestionId() { return questionId; }

    public int getNumber() { return number; }

    public String getTitle() { return title; }

    public boolean isCorrect() { return correct; }
}
