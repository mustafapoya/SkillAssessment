package net.golbarg.skillassessment.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Splits a topic into short levels, in the topic's own question order. The first level is always
 * open; each further level opens once enough of the previous one has been answered correctly.
 * The content has no difficulty ratings, so levels are sized chunks rather than difficulty tiers.
 */
public final class LearningPath {
    /** Questions per level. */
    public static final int LEVEL_SIZE = 15;
    /** A shorter tail than this is merged into the level before it. */
    static final int MIN_LAST_LEVEL = 6;
    /** Share of a level, in percent, to master before the next level opens. */
    public static final int UNLOCK_PERCENT = 70;

    public static final class Level {
        /** 0-based position in the path. */
        public final int index;
        public final List<Integer> questionIds;
        public final int mastered;
        public final boolean open;

        Level(int index, List<Integer> questionIds, int mastered, boolean open) {
            this.index = index;
            this.questionIds = Collections.unmodifiableList(questionIds);
            this.mastered = mastered;
            this.open = open;
        }

        public int size() { return questionIds.size(); }

        public int masteryPercent() {
            return questionIds.isEmpty() ? 0 : Math.round(mastered * 100f / questionIds.size());
        }

        public boolean isComplete() { return masteryPercent() >= UNLOCK_PERCENT; }
    }

    private LearningPath() {
    }

    /**
     * @param questionIds the topic's questions in display order
     * @param mastered    ids answered correctly at least once
     */
    public static List<Level> build(List<Integer> questionIds, Set<Integer> mastered) {
        List<List<Integer>> chunks = new ArrayList<>();
        for (int start = 0; start < questionIds.size(); start += LEVEL_SIZE) {
            chunks.add(new ArrayList<>(questionIds.subList(start, Math.min(questionIds.size(), start + LEVEL_SIZE))));
        }
        int last = chunks.size() - 1;
        if (last > 0 && chunks.get(last).size() < MIN_LAST_LEVEL) {
            chunks.get(last - 1).addAll(chunks.remove(last));
        }

        List<Level> levels = new ArrayList<>();
        boolean open = true;
        for (int i = 0; i < chunks.size(); i++) {
            List<Integer> ids = chunks.get(i);
            int count = 0;
            for (Integer id : ids) if (mastered.contains(id)) count++;
            Level level = new Level(i, ids, count, open);
            levels.add(level);
            open = open && level.isComplete();
        }
        return levels;
    }
}
