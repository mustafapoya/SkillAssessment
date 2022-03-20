package net.golbarg.skillassessment.models;

public class ExamResult {
    private int correct;
    private int wrong;

    public ExamResult(int correct, int wrong) {
        this.correct = correct;
        this.wrong = wrong;
    }

    public ExamResult() {
        this(0, 0);
    }

    public int getCorrect() {
        return correct;
    }

    public void setCorrect(int correct) {
        this.correct = correct;
    }

    public int getWrong() {
        return wrong;
    }

    public void setWrong(int wrong) {
        this.wrong = wrong;
    }

    @Override
    public String toString() {
        return "ExamResult{" +
                "correct=" + correct +
                ", wrong=" + wrong +
                '}';
    }
}
