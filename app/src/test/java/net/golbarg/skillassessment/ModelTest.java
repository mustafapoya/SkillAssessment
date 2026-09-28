package net.golbarg.skillassessment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.golbarg.skillassessment.models.AnswerResponseType;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionAnswer;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.util.CategoryNames;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class ModelTest {

    private static Question question(boolean... correct) {
        Question q = new Question(1, 1, 1, "Q", 1);
        for (int i = 0; i < correct.length; i++) q.getAnswers().add(new QuestionAnswer(1, i + 1, "A" + i, correct[i]));
        return q;
    }

    @Test
    public void correctPositionsFollowDisplayOrder() {
        Question q = question(false, false, true, false);
        assertEquals(new HashSet<>(Arrays.asList(2)), q.getCorrectPositions());
        assertEquals(1, q.getRequiredSelections());
    }

    @Test
    public void multiAnswerQuestionsRequireEveryCorrectOption() {
        Question q = question(true, false, true, false);
        assertEquals(2, q.getRequiredSelections());
        assertTrue(q.getCorrectPositions().equals(new HashSet<>(Arrays.asList(0, 2))));
    }

    @Test
    public void scoreIsBasedOnQuestionsAsked() {
        QuestionResult r = new QuestionResult(-1, 1, 0, 0, 0, 0, 0);
        r.record(AnswerResponseType.CORRECT);
        r.record(AnswerResponseType.CORRECT);
        r.record(AnswerResponseType.WRONG);
        r.record(AnswerResponseType.NO_ANSWER);
        assertEquals(4, r.getTotal());
        assertEquals(50, r.getScorePercent());
    }

    @Test
    public void emptyResultScoresZero() {
        assertEquals(0, new QuestionResult(-1, 1, 0, 0, 0, 0, 0).getScorePercent());
    }

    @Test
    public void displayNames() {
        assertEquals("C#", CategoryNames.displayName("c#"));
        assertEquals("Node.js", CategoryNames.displayName("node.js"));
        assertEquals("Machine Learning", CategoryNames.displayName("machine-learning"));
        assertEquals("Ruby on Rails", CategoryNames.displayName("ruby-on-rails"));
        assertEquals("Google Cloud Platform", CategoryNames.displayName("google-cloud-platform"));
        assertEquals("Daily challenge", Category.pseudo(Category.DAILY, 5).getDisplayName());
        assertEquals("Mistakes review", Category.pseudo(Category.REVIEW, 5).getDisplayName());
        assertEquals("Mixed practice", Category.pseudo(Category.MIXED, 5).getDisplayName());
    }

    @Test
    public void masteryPercent() {
        Category c = new Category(1, "python", 80);
        c.setMastered(20);
        assertEquals(25, c.getMasteryPercent());
        c.setMastered(200);
        assertEquals(100, c.getMasteryPercent());
    }

    @Test
    public void monograms() {
        assertEquals("C#", CategoryNames.monogram("c#"));
        assertEquals("Py", CategoryNames.monogram("python"));
        assertEquals("ML", CategoryNames.monogram("machine-learning"));
        assertEquals("AWS", CategoryNames.monogram("aws"));
    }
}
