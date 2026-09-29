package net.golbarg.skillassessment.models;

public class QuestionResult {
    private long id;
    private final int categoryId;
    private int correctAnswer;
    private int wrongAnswer;
    private int noAnswer;
    private long createdAt;
    private long durationMs;
    private boolean exam;

    public QuestionResult(long id, int categoryId, int correctAnswer, int wrongAnswer, int noAnswer, long createdAt, long durationMs) {
        this.id = id;
        this.categoryId = categoryId;
        this.correctAnswer = correctAnswer;
        this.wrongAnswer = wrongAnswer;
        this.noAnswer = noAnswer;
        this.createdAt = createdAt;
        this.durationMs = durationMs;
    }

    public long getId() { return id; }

    /** Taken in exam mode (one timer, no feedback); only exams can earn a certificate. */
    public boolean isExam() { return exam; }

    public void setExam(boolean exam) { this.exam = exam; }

    public void setId(long id) { this.id = id; }

    public int getCategoryId() { return categoryId; }

    public int getCorrectAnswer() { return correctAnswer; }

    public int getWrongAnswer() { return wrongAnswer; }

    public int getNoAnswer() { return noAnswer; }

    /** 0 for results recorded before version 2 of the app. */
    public long getCreatedAt() { return createdAt; }

    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getDurationMs() { return durationMs; }

    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public void record(AnswerResponseType type) {
        switch (type) {
            case CORRECT: correctAnswer++; break;
            case WRONG: wrongAnswer++; break;
            default: noAnswer++; break;
        }
    }

    public int getTotal() {
        return correctAnswer + wrongAnswer + noAnswer;
    }

    public int getScorePercent() {
        int total = getTotal();
        return total == 0 ? 0 : Math.round(correctAnswer * 100f / total);
    }
}
