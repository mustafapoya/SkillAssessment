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
 * the result screen.
 */
public class QuizViewModel extends AndroidViewModel {
    public static final long TIME_PER_QUESTION_MS = 60_000L;
    private static final long TICK_MS = 250L;
    private static final int DEFAULT_MIXED_LENGTH = 15;
    private static final int MAX_REVIEW_LENGTH = 20;

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
    private boolean examTimedOut;

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

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning) return;
            remainingMs = Math.max(0, deadline - SystemClock.elapsedRealtime());
            timeLeft.setValue(remainingMs);
            if (remainingMs == 0) {
                timerRunning = false;
                if (exam) onExamTimeUp();
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
     * @param categoryId a topic id, or {@link Category#MIXED}, {@link Category#DAILY} or {@link Category#REVIEW}
     * @param length     number of questions; 0 means all (or the mode's default)
     */
    public void start(int categoryId, int length, boolean timer, boolean shuffle, boolean examMode) {
        if (started) return;
        started = true;
        timerEnabled = timer;
        exam = examMode;
        Async.run(null, () -> {
            List<Question> loaded;
            switch (categoryId) {
                case Category.DAILY:
                    loaded = repository.getMixedQuestions(ProgressTracker.DAILY_QUESTIONS, ProgressTracker.dailySeed());
                    break;
                case Category.MIXED:
                    loaded = repository.getMixedQuestions(length > 0 ? length : DEFAULT_MIXED_LENGTH, null);
                    break;
                case Category.REVIEW:
                    loaded = repository.getReviewQuestions(length > 0 ? length : MAX_REVIEW_LENGTH);
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
            timerTotalMs = exam ? TIME_PER_QUESTION_MS * questions.size() : TIME_PER_QUESTION_MS;
            beginQuestion();
            if (timerEnabled && exam) startTimer(timerTotalMs);
        });
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

    public boolean isExamTimedOut() { return examTimedOut; }

    public boolean isLastQuestion() { return index >= questions.size() - 1; }

    public boolean isCurrentBookmarked() {
        Question q = getCurrentQuestion();
        return q != null && bookmarked.contains(q.getId());
    }

    public int getAnsweredCount() { return answers.size(); }

    /** Consecutive correct answers ending with the current question. */
    public int getStreak() { return streak; }

    public int getBestStreak() { return bestStreak; }

    // endregion

    // region Actions

    public void toggleOption(int position) {
        Question q = getCurrentQuestion();
        if (phase != Phase.ANSWERING || q == null) return;
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

    /** Saves the questions answered so far and ends the test. */
    public void finish() {
        if (saving || result == null) return;
        saving = true;
        stopTimer();
        pauseActiveClock();
        result.setDurationMs(activeAccumulated);
        List<ResultItem> items = new ArrayList<>(answers);
        int best = bestStreak;
        boolean isExam = exam;
        Async.run(null, () -> {
            long id = repository.saveResult(result, items);
            ProgressTracker.Outcome outcome = ProgressTracker.onTestFinished(getApplication(), repository, result, isExam, best);
            return new FinishInfo(id, outcome);
        }, info -> finished.setValue(info != null ? info : new FinishInfo(-1, new ProgressTracker.Outcome())));
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
        publish();
        if (timerEnabled && !exam) startTimer(TIME_PER_QUESTION_MS);
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
        if (exam) {
            // No feedback in exam mode: move straight on, the timer keeps running.
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
        examTimedOut = true;
        // A pick on the current question that was never confirmed still counts as no answer.
        for (int i = index; i < questions.size(); i++) {
            result.record(AnswerResponseType.NO_ANSWER);
            answers.add(new ResultItem(questions.get(i).getId(), Collections.emptyList(), AnswerResponseType.NO_ANSWER));
        }
        streak = 0;
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
