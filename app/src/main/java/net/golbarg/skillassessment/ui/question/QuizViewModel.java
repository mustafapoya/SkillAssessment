package net.golbarg.skillassessment.ui.question;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.AnswerResponseType;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.InterviewPack;
import net.golbarg.skillassessment.models.LearningPath;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.models.ResultItem;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.ProgressTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds a running test so it survives configuration changes.
 *
 * <p>Practice mode reveals the answer after every question and times each question separately.
 * Exam mode records answers silently, runs one timer for the whole test and reveals everything on
 * the result screen. A speed round works like an exam against a fixed one-minute clock: when it
 * runs out the round simply ends, and only the answered questions count.
 */
public class QuizViewModel extends AndroidViewModel {
    public static final long TIME_PER_QUESTION_MS = 60_000L;
    private static final long TICK_MS = 250L;
    private static final int DEFAULT_MIXED_LENGTH = 15;
    private static final int MAX_REVIEW_LENGTH = 20;
    public static final long SPRINT_MS = 60_000L;
    /** More questions than anyone can answer in a minute. */
    private static final int SPRINT_POOL = 150;
    /** Questions in the free preview of a locked topic. */
    public static final int PREVIEW_LENGTH = 5;
    /** Result id reported for a finished preview, which is never saved. */
    public static final long PREVIEW_RESULT_ID = -2;

    public enum Phase { LOADING, ANSWERING, REVEALED, EMPTY }

    /** Why the current question was revealed. */
    public enum Reveal { NONE, CORRECT, WRONG, SKIPPED, TIMEOUT }

    /** Everything the result screen needs after the test is saved. */
    public static final class FinishInfo {
        public final long resultId;
        public final ProgressTracker.Outcome outcome;

        FinishInfo(long resultId, ProgressTracker.Outcome outcome) {
            this.resultId = resultId;
            this.outcome = outcome;
        }
    }

    private final QuizRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Integer> state = new MutableLiveData<>();
    private final MutableLiveData<Long> timeLeft = new MutableLiveData<>();
    private final MutableLiveData<FinishInfo> finished = new MutableLiveData<>();

    private boolean started;
    private Category category;
    private List<Question> questions = Collections.emptyList();
    private Map<Integer, String> slugs = new HashMap<>();
    private final Set<Integer> bookmarked = new HashSet<>();
    private final List<ResultItem> answers = new ArrayList<>();
    private QuestionResult result;
    private boolean exam;
    private boolean sprint;
    /** The clock ended the test (exam or speed round). */
    private boolean timedOut;
    /** Learning-path level being practised, or -1. */
    private int level = -1;
    /** Only the topic's questions still in the mistakes queue. */
    private boolean focus;
    @Nullable private AnswerResponseType lastOutcome;

    private int index;
    private int streak;
    private int bestStreak;
    private Phase phase = Phase.LOADING;
    private Reveal reveal = Reveal.NONE;
    private final LinkedHashSet<Integer> selected = new LinkedHashSet<>();

    private boolean timerEnabled;
    private long timerTotalMs;
    private long remainingMs;
    private long deadline;
    private boolean timerRunning;
    private boolean pausedTimer;

    private long activeSince;
    private long activeAccumulated;
    private boolean saving;
    private int version;

    private boolean preview;
    private int startLength;
    private boolean startShuffle;
    private boolean hintUsed;
    private boolean abandoned;
    /** Options removed by the 50/50 hint on the current question. */
    private final Set<Integer> hidden = new HashSet<>();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning) return;
            remainingMs = Math.max(0, deadline - SystemClock.elapsedRealtime());
            timeLeft.setValue(remainingMs);
            if (remainingMs == 0) {
                timerRunning = false;
                if (exam) onExamTimeUp();
                else if (sprint) onSprintTimeUp();
                else revealWith(AnswerResponseType.NO_ANSWER, Reveal.TIMEOUT);
            } else {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    public QuizViewModel(@NonNull Application application) {
        super(application);
        repository = QuizRepository.get(application);
    }

    public LiveData<Integer> getState() { return state; }

    public LiveData<Long> getTimeLeft() { return timeLeft; }

    /** Emits once the test is complete and saved. */
    public LiveData<FinishInfo> getFinished() { return finished; }

    /**
     * @param categoryId a topic id, or {@link Category#MIXED}, {@link Category#DAILY}, {@link Category#REVIEW}
     *                   or {@link Category#BOOKMARKED}
     *                   {@link Category#SPRINT} or an {@link InterviewPack} id
     * @param length     number of questions; 0 means all (or the mode's default)
     * @param levelIndex a learning-path level of the topic, or -1 for the whole topic
     * @param focusMode  only the topic's questions still waiting in the mistakes queue
     */
    public void start(int categoryId, int length, boolean timer, boolean shuffle, boolean examMode, boolean previewMode,
                      int levelIndex, boolean focusMode) {
        if (started) return;
        started = true;
        sprint = categoryId == Category.SPRINT && !previewMode;
        timerEnabled = timer || sprint;
        exam = examMode && !previewMode && !sprint;
        preview = previewMode;
        level = categoryId >= 0 ? levelIndex : -1;
        focus = categoryId >= 0 && focusMode;
        startLength = length;
        startShuffle = shuffle;
        Async.run(null, () -> {
            List<Question> loaded;
            if (preview) {
                loaded = repository.getPreviewQuestions(categoryId, PREVIEW_LENGTH);
                slugs = repository.getSlugs();
                category = repository.getCategory(categoryId);
                return loaded;
            }
            InterviewPack pack = InterviewPack.fromId(categoryId);
            if (level >= 0) {
                loaded = repository.getLevelQuestions(categoryId, level);
                if (shuffle) Collections.shuffle(loaded);
            } else if (focus) {
                loaded = repository.getMissedQuestions(categoryId, length > 0 ? length : MAX_REVIEW_LENGTH);
            } else if (pack != null) {
                loaded = repository.getPackQuestions(pack, length > 0 ? length : InterviewPack.LENGTH);
            } else switch (categoryId) {
                case Category.SPRINT:
                    loaded = repository.getMixedQuestions(SPRINT_POOL, null);
                    break;
                case Category.DAILY:
                    loaded = repository.getMixedQuestions(ProgressTracker.DAILY_QUESTIONS, ProgressTracker.dailySeed());
                    break;
                case Category.MIXED:
                    loaded = repository.getMixedQuestions(length > 0 ? length : DEFAULT_MIXED_LENGTH, null);
                    break;
                case Category.REVIEW:
                    loaded = repository.getReviewQuestions(length > 0 ? length : MAX_REVIEW_LENGTH);
                    break;
                case Category.BOOKMARKED:
                    loaded = repository.getBookmarkedQuestions(length > 0 ? length : 0);
                    if (shuffle) Collections.shuffle(loaded);
                    break;
                default:
                    loaded = repository.getQuestions(categoryId);
                    if (shuffle) Collections.shuffle(loaded);
                    break;
            }
            List<Question> usable = new ArrayList<>();
            for (Question q : loaded) if (q.getAnswers().size() >= 2) usable.add(q);
            if (length > 0 && usable.size() > length) usable = new ArrayList<>(usable.subList(0, length));
            bookmarked.addAll(repository.getBookmarkedQuestionIds());
            slugs = repository.getSlugs();
            category = categoryId < 0 ? Category.pseudo(categoryId, usable.size()) : repository.getCategory(categoryId);
            return usable;
        }, loaded -> {
            questions = loaded == null ? Collections.emptyList() : loaded;
            if (category == null || questions.isEmpty()) {
                phase = Phase.EMPTY;
                publish();
                return;
            }
            result = new QuestionResult(-1, category.getId(), 0, 0, 0, System.currentTimeMillis(), 0);
            index = 0;
            activeSince = SystemClock.elapsedRealtime();
            timerTotalMs = sprint ? SPRINT_MS : exam ? TIME_PER_QUESTION_MS * questions.size() : TIME_PER_QUESTION_MS;
            beginQuestion();
            if (timerEnabled && isContinuous()) startTimer(timerTotalMs);
        });
    }

    /** Continues the test saved in {@link QuizSession}; shows the empty state if it can't be restored. */
    public void resume() {
        if (started) return;
        started = true;
        QuizSession session = QuizSession.load(getApplication());
        if (session == null) {
            phase = Phase.EMPTY;
            publish();
            return;
        }
        timerEnabled = session.timer;
        exam = session.exam;
        startLength = session.length;
        startShuffle = session.shuffle;
        level = session.level;
        focus = session.focus;
        hintUsed = session.hintUsed;
        Async.run(null, () -> {
            List<Question> loaded = repository.getQuestionsInOrder(session.questionIds);
            bookmarked.addAll(repository.getBookmarkedQuestionIds());
            slugs = repository.getSlugs();
            category = session.categoryId < 0 ? Category.pseudo(session.categoryId, loaded.size()) : repository.getCategory(session.categoryId);
            return loaded;
        }, loaded -> {
            // A topic removed or reset since then can't be continued faithfully.
            if (loaded == null || category == null || loaded.size() != session.questionIds.size()) {
                QuizSession.clear(getApplication());
                phase = Phase.EMPTY;
                publish();
                return;
            }
            questions = loaded;
            result = new QuestionResult(-1, category.getId(), 0, 0, 0, session.createdAt, 0);
            for (ResultItem item : session.answers) {
                result.record(item.getOutcome());
                answers.add(item);
            }
            streak = session.streak;
            bestStreak = session.bestStreak;
            activeAccumulated = session.activeMs;
            activeSince = SystemClock.elapsedRealtime();
            index = answers.size();
            timerTotalMs = exam ? TIME_PER_QUESTION_MS * questions.size() : TIME_PER_QUESTION_MS;
            beginQuestion();
            if (timerEnabled && exam) startTimer(Math.max(1000, session.examRemainingMs));
        });
    }

    /** Stores the test so it can be continued later. Previews and finished tests aren't kept. */
    private void saveSession() {
        // A speed round is over in a minute; there is nothing worth continuing later.
        if (preview || sprint || abandoned || saving || result == null || questions.isEmpty()) return;
        if (phase != Phase.ANSWERING && phase != Phase.REVEALED) return;
        if (answers.size() >= questions.size()) return;
        QuizSession s = new QuizSession();
        s.categoryId = category.getId();
        s.length = startLength;
        s.timer = timerEnabled;
        s.shuffle = startShuffle;
        s.exam = exam;
        s.level = level;
        s.focus = focus;
        for (Question q : questions) s.questionIds.add(q.getId());
        s.answers.addAll(answers);
        s.streak = streak;
        s.bestStreak = bestStreak;
        s.activeMs = activeAccumulated;
        s.createdAt = result.getCreatedAt();
        s.examRemainingMs = exam ? remainingMs : 0;
        s.hintUsed = hintUsed;
        s.save(getApplication());
    }

    /** The user quit on purpose: nothing to continue later. */
    public void abandon() {
        abandoned = true;
        QuizSession.clear(getApplication());
    }

    // region Getters used for rendering

    @Nullable public Category getCategory() { return category; }

    /** The real topic of a question; differs from {@link #getCategory()} in mixed tests. */
    public String getSlug(Question question) {
        String slug = slugs.get(question.getCategoryId());
        return slug != null ? slug : category == null ? "" : category.getSlug();
    }

    public Phase getPhase() { return phase; }

    public Reveal getReveal() { return reveal; }

    public int getIndex() { return index; }

    public int getCount() { return questions.size(); }

    @Nullable
    public Question getCurrentQuestion() {
        return index < questions.size() ? questions.get(index) : null;
    }

    public Set<Integer> getSelected() { return Collections.unmodifiableSet(selected); }

    public boolean isTimerEnabled() { return timerEnabled; }

    public long getTimerTotalMs() { return timerTotalMs; }

    public boolean isExam() { return exam; }

    public boolean isSprint() { return sprint; }

    /** Exams and speed rounds move on without revealing answers, against one clock. */
    public boolean isContinuous() { return exam || sprint; }

    public boolean isTimedOut() { return timedOut; }

    /** Learning-path level being practised, or -1. */
    public int getLevel() { return level; }

    public boolean isFocus() { return focus; }

    /** How the most recent question was answered; null before the first answer. */
    @Nullable public AnswerResponseType getLastOutcome() { return lastOutcome; }

    public boolean isLastQuestion() { return index >= questions.size() - 1; }

    public boolean isCurrentBookmarked() {
        Question q = getCurrentQuestion();
        return q != null && bookmarked.contains(q.getId());
    }

    public int getAnsweredCount() { return answers.size(); }

    public boolean isPreview() { return preview; }

    /** Setup this test was started with, so "Try again" can repeat it (also after resuming). */
    public int getStartLength() { return startLength; }

    public boolean isStartShuffle() { return startShuffle; }

    /** Correct answers so far; used for the preview summary, which has no result screen. */
    public int getCorrectCount() { return result == null ? 0 : result.getCorrectAnswer(); }

    /** Options removed by the 50/50 hint on the current question. */
    public Set<Integer> getHidden() { return Collections.unmodifiableSet(hidden); }

    /** One hint per practice test, on a question with at least two wrong options. */
    public boolean canUseHint() {
        Question q = getCurrentQuestion();
        if (q == null || isContinuous() || preview || hintUsed || phase != Phase.ANSWERING) return false;
        return q.getAnswers().size() - q.getCorrectPositions().size() >= 2;
    }

    public boolean isHintUsed() { return hintUsed; }

    /** Consecutive correct answers ending with the current question. */
    public int getStreak() { return streak; }

    public int getBestStreak() { return bestStreak; }

    // endregion

    // region Actions

    public void toggleOption(int position) {
        Question q = getCurrentQuestion();
        if (phase != Phase.ANSWERING || q == null || hidden.contains(position)) return;
        int required = q.getRequiredSelections();
        if (required == 1) {
            selected.clear();
            selected.add(position);
        } else if (!selected.remove(position)) {
            if (selected.size() >= required) {
                Integer oldest = selected.iterator().next();
                selected.remove(oldest);
            }
            selected.add(position);
        }
        publish();
    }

    public void check() {
        Question q = getCurrentQuestion();
        if (phase != Phase.ANSWERING || q == null) return;
        if (selected.isEmpty()) {
            skip();
            return;
        }
        boolean correct = selected.equals(q.getCorrectPositions());
        revealWith(correct ? AnswerResponseType.CORRECT : AnswerResponseType.WRONG, correct ? Reveal.CORRECT : Reveal.WRONG);
    }

    public void skip() {
        if (phase != Phase.ANSWERING) return;
        selected.clear();
        revealWith(AnswerResponseType.NO_ANSWER, Reveal.SKIPPED);
    }

    public void next() {
        if (phase != Phase.REVEALED) return;
        advance();
    }

    /** 50/50: removes half of the wrong options (rounded up) from the current question. */
    public void useHint() {
        if (!canUseHint()) return;
        Question q = getCurrentQuestion();
        Set<Integer> correct = q.getCorrectPositions();
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < q.getAnswers().size(); i++) if (!correct.contains(i)) wrong.add(i);
        Collections.shuffle(wrong);
        int remove = (wrong.size() + 1) / 2;
        hidden.addAll(wrong.subList(0, remove));
        selected.removeAll(hidden);
        hintUsed = true;
        publish();
    }

    /** Saves the questions answered so far and ends the test. */
    public void finish() {
        if (saving || result == null) return;
        saving = true;
        stopTimer();
        pauseActiveClock();
        QuizSession.clear(getApplication());
        if (preview) {
            // Previews are a taste of a locked topic: nothing is recorded.
            finished.setValue(new FinishInfo(PREVIEW_RESULT_ID, new ProgressTracker.Outcome()));
            return;
        }
        if (answers.isEmpty()) {
            finished.setValue(new FinishInfo(-1, new ProgressTracker.Outcome()));
            return;
        }
        result.setDurationMs(activeAccumulated);
        result.setExam(exam);
        List<ResultItem> items = new ArrayList<>(answers);
        int best = bestStreak;
        boolean isExam = exam;
        int levelIndex = level;
        int topicId = category.getId();
        Async.run(null, () -> {
            boolean nextWasOpen = levelIndex >= 0 && isLevelOpen(topicId, levelIndex + 1);
            long id = repository.saveResult(result, items);
            ProgressTracker.Outcome outcome = ProgressTracker.onTestFinished(getApplication(), repository, result, isExam, best);
            if (levelIndex >= 0 && !nextWasOpen && isLevelOpen(topicId, levelIndex + 1)) {
                outcome.levelUnlocked = levelIndex + 1;
            }
            return new FinishInfo(id, outcome);
        }, info -> finished.setValue(info != null ? info : new FinishInfo(-1, new ProgressTracker.Outcome())));
    }

    private boolean isLevelOpen(int categoryId, int levelIndex) {
        List<LearningPath.Level> levels = repository.getLearningPath(categoryId);
        return levelIndex < levels.size() && levels.get(levelIndex).open;
    }

    public void toggleBookmark() {
        Question q = getCurrentQuestion();
        if (q == null) return;
        boolean nowBookmarked = !bookmarked.contains(q.getId());
        if (nowBookmarked) bookmarked.add(q.getId());
        else bookmarked.remove(q.getId());
        Async.io(() -> repository.setBookmarked(q.getId(), nowBookmarked));
        publish();
    }

    // endregion

    // region Lifecycle-driven pause

    public void onScreenHidden() {
        if (timerRunning) {
            remainingMs = Math.max(0, deadline - SystemClock.elapsedRealtime());
            stopTimer();
            pausedTimer = true;
        }
        pauseActiveClock();
        saveSession();
    }

    public void onScreenVisible() {
        if (saving || phase == Phase.LOADING || phase == Phase.EMPTY) return;
        if (activeSince == 0) activeSince = SystemClock.elapsedRealtime();
        if (pausedTimer) {
            pausedTimer = false;
            startTimer(remainingMs);
        }
    }

    private void pauseActiveClock() {
        if (activeSince != 0) {
            activeAccumulated += SystemClock.elapsedRealtime() - activeSince;
            activeSince = 0;
        }
    }

    // endregion

    private void beginQuestion() {
        phase = Phase.ANSWERING;
        reveal = Reveal.NONE;
        selected.clear();
        hidden.clear();
        publish();
        if (timerEnabled && !isContinuous()) startTimer(TIME_PER_QUESTION_MS);
    }

    private void advance() {
        if (isLastQuestion()) {
            finish();
        } else {
            index++;
            beginQuestion();
        }
    }

    private void revealWith(AnswerResponseType type, Reveal why) {
        Question q = getCurrentQuestion();
        if (q == null || phase != Phase.ANSWERING) return;
        result.record(type);
        if (type == AnswerResponseType.CORRECT) {
            streak++;
            bestStreak = Math.max(bestStreak, streak);
        } else {
            streak = 0;
        }
        answers.add(new ResultItem(q.getId(), new ArrayList<>(selected), type));
        lastOutcome = type;
        if (isContinuous()) {
            // No reveal in exams and speed rounds: move straight on, the clock keeps running.
            advance();
            return;
        }
        stopTimer();
        pausedTimer = false;
        phase = Phase.REVEALED;
        reveal = why;
        publish();
    }

    /** The exam clock ran out: every unanswered question counts as skipped. */
    private void onExamTimeUp() {
        if (saving) return;
        timedOut = true;
        // A pick on the current question that was never confirmed still counts as no answer.
        for (int i = index; i < questions.size(); i++) {
            result.record(AnswerResponseType.NO_ANSWER);
            answers.add(new ResultItem(questions.get(i).getId(), Collections.emptyList(), AnswerResponseType.NO_ANSWER));
        }
        streak = 0;
        finish();
    }

    /** The minute is over: the question on screen is dropped, not counted as skipped. */
    private void onSprintTimeUp() {
        if (saving) return;
        timedOut = true;
        finish();
    }

    private void startTimer(long millis) {
        stopTimer();
        remainingMs = millis;
        deadline = SystemClock.elapsedRealtime() + millis;
        timerRunning = true;
        timeLeft.setValue(remainingMs);
        handler.postDelayed(tick, TICK_MS);
    }

    private void stopTimer() {
        timerRunning = false;
        handler.removeCallbacks(tick);
    }

    private void publish() {
        state.setValue(++version);
    }

    @Override
    protected void onCleared() {
        stopTimer();
        super.onCleared();
    }
}
