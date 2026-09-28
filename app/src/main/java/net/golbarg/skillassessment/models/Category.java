package net.golbarg.skillassessment.models;

import net.golbarg.skillassessment.util.CategoryNames;

/** A topic from the bundled catalog, enriched with the user's local state. */
public class Category {
    /** Pseudo topics used by special test modes; they are never stored in the categories table. */
    public static final int MIXED = -1;
    public static final int DAILY = -2;
    public static final int REVIEW = -3;

    private final int id;
    private final String slug;
    private final int numberOfQuestion;
    private boolean unlocked;
    private int attempts;
    private int bestScore = -1;
    private int mastered;

    public Category(int id, String slug, int numberOfQuestion) {
        this.id = id;
        this.slug = slug;
        this.numberOfQuestion = numberOfQuestion;
    }

    /** A synthetic topic for mixed, daily and review tests. */
    public static Category pseudo(int id, int questionCount) {
        String slug = id == DAILY ? "daily-challenge" : id == REVIEW ? "mistakes-review" : "mixed-practice";
        Category category = new Category(id, slug, questionCount);
        category.setUnlocked(true);
        return category;
    }

    public boolean isPseudo() { return id < 0; }

    public int getId() { return id; }

    /** Raw identifier such as "node.js" or "c#"; also the folder name of the question images. */
    public String getSlug() { return slug; }

    public String getDisplayName() { return CategoryNames.displayName(slug); }

    public int getNumberOfQuestion() { return numberOfQuestion; }

    public boolean isUnlocked() { return unlocked; }

    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public int getAttempts() { return attempts; }

    public void setAttempts(int attempts) { this.attempts = attempts; }

    /** Best score in percent, or -1 when the topic has never been attempted. */
    public int getBestScore() { return bestScore; }

    public void setBestScore(int bestScore) { this.bestScore = bestScore; }

    /** Distinct questions of this topic the user has answered correctly at least once. */
    public int getMastered() { return mastered; }

    public void setMastered(int mastered) { this.mastered = mastered; }

    public int getMasteryPercent() {
        return numberOfQuestion == 0 ? 0 : Math.min(100, Math.round(mastered * 100f / numberOfQuestion));
    }
}
