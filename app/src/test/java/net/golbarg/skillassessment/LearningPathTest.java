package net.golbarg.skillassessment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.InterviewPack;
import net.golbarg.skillassessment.models.LearningPath;
import net.golbarg.skillassessment.util.CategoryNames;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LearningPathTest {

    private static List<Integer> ids(int count) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) ids.add(i);
        return ids;
    }

    private static Set<Integer> range(int from, int to) {
        Set<Integer> set = new HashSet<>();
        for (int i = from; i <= to; i++) set.add(i);
        return set;
    }

    @Test
    public void splitsIntoLevelsInQuestionOrder() {
        List<LearningPath.Level> levels = LearningPath.build(ids(45), Collections.emptySet());
        assertEquals(3, levels.size());
        assertEquals(Integer.valueOf(1), levels.get(0).questionIds.get(0));
        assertEquals(Integer.valueOf(31), levels.get(2).questionIds.get(0));
        for (LearningPath.Level level : levels) assertEquals(LearningPath.LEVEL_SIZE, level.size());
    }

    @Test
    public void shortTailJoinsThePreviousLevel() {
        // 32 questions: 15 + 17, not 15 + 15 + 2.
        List<LearningPath.Level> levels = LearningPath.build(ids(32), Collections.emptySet());
        assertEquals(2, levels.size());
        assertEquals(17, levels.get(1).size());
    }

    @Test
    public void aLongEnoughTailKeepsItsOwnLevel() {
        List<LearningPath.Level> levels = LearningPath.build(ids(36), Collections.emptySet());
        assertEquals(3, levels.size());
        assertEquals(6, levels.get(2).size());
    }

    @Test
    public void onlyTheFirstLevelIsOpenAtTheStart() {
        List<LearningPath.Level> levels = LearningPath.build(ids(45), Collections.emptySet());
        assertTrue(levels.get(0).open);
        assertFalse(levels.get(1).open);
        assertFalse(levels.get(2).open);
    }

    @Test
    public void masteringSeventyPercentOpensTheNextLevel() {
        // 11 of 15 is 73%: level 2 opens. Level 2 itself is untouched, so level 3 stays shut.
        List<LearningPath.Level> levels = LearningPath.build(ids(45), range(1, 11));
        assertTrue(levels.get(0).isComplete());
        assertTrue(levels.get(1).open);
        assertFalse(levels.get(2).open);
    }

    @Test
    public void justBelowTheThresholdKeepsTheNextLevelShut() {
        // 10 of 15 is 67%.
        List<LearningPath.Level> levels = LearningPath.build(ids(45), range(1, 10));
        assertFalse(levels.get(0).isComplete());
        assertFalse(levels.get(1).open);
    }

    @Test
    public void progressInALockedLevelDoesNotOpenLaterLevels() {
        // Level 2 fully mastered (e.g. through mixed tests) while level 1 is not: level 3 stays shut.
        List<LearningPath.Level> levels = LearningPath.build(ids(45), range(16, 30));
        assertFalse(levels.get(1).open);
        assertEquals(100, levels.get(1).masteryPercent());
        assertFalse(levels.get(2).open);
    }

    @Test
    public void emptyTopicHasNoLevels() {
        assertTrue(LearningPath.build(new ArrayList<>(), Collections.emptySet()).isEmpty());
    }

    @Test
    public void interviewPacksHaveUniqueNegativeIdsAndSlugs() {
        Set<Integer> ids = new HashSet<>();
        Set<String> slugs = new HashSet<>();
        for (InterviewPack pack : InterviewPack.values()) {
            assertTrue(pack.id < Category.SPRINT);
            assertTrue(ids.add(pack.id));
            assertTrue(slugs.add(pack.slug));
            assertFalse(pack.topics.isEmpty());
            assertEquals(pack, InterviewPack.fromId(pack.id));
            assertEquals(pack, InterviewPack.fromSlug(pack.slug));
        }
        assertNull(InterviewPack.fromId(Category.MIXED));
    }

    @Test
    public void pseudoTopicsResolveForTheNewModes() {
        Category sprint = Category.pseudo(Category.SPRINT, 0);
        assertEquals("speed-round", sprint.getSlug());
        assertEquals("Speed round", sprint.getDisplayName());
        Category pack = Category.pseudo(InterviewPack.ANDROID.id, 0);
        assertEquals(InterviewPack.ANDROID.slug, pack.getSlug());
        assertEquals("Android developer", pack.getDisplayName());
        assertNotNull(CategoryNames.monogram(pack.getSlug()));
    }
}
