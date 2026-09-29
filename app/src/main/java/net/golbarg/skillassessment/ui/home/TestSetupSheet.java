package net.golbarg.skillassessment.ui.home;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.SheetTestSetupBinding;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.ui.path.PathActivity;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.ui.study.StudyActivity;
import net.golbarg.skillassessment.ui.widget.AppBottomSheet;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.Prefs;

/** Lets the user pick test length, timer and shuffle before starting a topic. */
public class TestSetupSheet extends AppBottomSheet {
    public static final String TAG = "TestSetupSheet";
    private static final int EXPRESS = 5;
    private static final int QUICK = 10;
    private static final int STANDARD = 25;

    private static final String ARG_ID = "id";
    private static final String ARG_SLUG = "slug";
    private static final String ARG_COUNT = "count";
    private static final String ARG_ATTEMPTS = "attempts";
    private static final String ARG_BEST = "best";
    private static final String ARG_MASTERED = "mastered";

    private SheetTestSetupBinding binding;

    public static TestSetupSheet newInstance(Category category) {
        Bundle args = new Bundle();
        args.putInt(ARG_ID, category.getId());
        args.putString(ARG_SLUG, category.getSlug());
        args.putInt(ARG_COUNT, category.getNumberOfQuestion());
        args.putInt(ARG_ATTEMPTS, category.getAttempts());
        args.putInt(ARG_BEST, category.getBestScore());
        args.putInt(ARG_MASTERED, category.getMastered());
        TestSetupSheet sheet = new TestSetupSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetTestSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        int categoryId = args.getInt(ARG_ID);
        String slug = args.getString(ARG_SLUG, "");
        int count = args.getInt(ARG_COUNT);
        int attempts = args.getInt(ARG_ATTEMPTS);
        int best = args.getInt(ARG_BEST, -1);
        int mastered = args.getInt(ARG_MASTERED, 0);

        CategoryNames.styleBadge(binding.txtBadge, slug, 16);
        binding.txtTitle.setText(CategoryNames.displayName(slug));

        String questions = getResources().getQuantityString(R.plurals.question_count, count, count);
        if (categoryId < 0) {
            String desc = categoryId == Category.BOOKMARKED ? getString(R.string.practice_saved_desc, count)
                    : categoryId == Category.DAILY ? getString(R.string.daily_challenge)
                    : categoryId == Category.REVIEW ? getString(R.string.mistakes_review)
                    : getString(R.string.mixed_practice_desc);
            binding.txtSubtitle.setText(getString(R.string.joined_dot, questions, desc));
            binding.layoutMastery.setVisibility(View.GONE);
        } else if (attempts > 0 && best >= 0) {
            String tests = getResources().getQuantityString(R.plurals.tests_taken, attempts, attempts);
            binding.txtSubtitle.setText(getString(R.string.joined_dot, questions, getString(R.string.attempts_best, tests, best)));
            int percent = count > 0 ? Math.round(mastered * 100f / count) : 0;
            binding.layoutMastery.setVisibility(View.VISIBLE);
            binding.txtMastery.setText(getString(R.string.mastery_level, mastered, count, percent));
            binding.progressMastery.setProgressCompat(percent, false);
        } else {
            binding.txtSubtitle.setText(getString(R.string.joined_dot, questions, getString(R.string.not_attempted)));
            binding.layoutMastery.setVisibility(View.GONE);
        }

        lengthLabel(binding.btnLenExpress, R.string.length_express, EXPRESS);
        lengthLabel(binding.btnLenQuick, R.string.length_quick, QUICK);
        lengthLabel(binding.btnLenStandard, R.string.length_standard, STANDARD);
        lengthLabel(binding.btnLenFull, R.string.length_full, count);

        binding.btnLenExpress.setVisibility(count > EXPRESS ? View.VISIBLE : View.GONE);
        binding.btnLenQuick.setVisibility(count > QUICK ? View.VISIBLE : View.GONE);
        binding.btnLenStandard.setVisibility(count > STANDARD ? View.VISIBLE : View.GONE);

        int preferred = Prefs.getTestLength(requireContext());
        if (preferred == EXPRESS && count > EXPRESS) binding.toggleLength.check(R.id.btn_len_express);
        else if (preferred == QUICK && count > QUICK) binding.toggleLength.check(R.id.btn_len_quick);
        else if (preferred == STANDARD && count > STANDARD) binding.toggleLength.check(R.id.btn_len_standard);
        else binding.toggleLength.check(R.id.btn_len_full);

        binding.switchTimer.setChecked(Prefs.isTimerEnabled(requireContext()));
        binding.switchShuffle.setChecked(Prefs.isShuffleEnabled(requireContext()));
        // Mixed, daily, review and saved-question tests pick their own order.
        if (categoryId < 0) ((View) binding.switchShuffle.getParent()).setVisibility(View.GONE);

        binding.toggleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) bindMode(checkedId == R.id.btn_mode_exam);
        });
        bindMode(false);

        binding.btnStart.setOnClickListener(v -> {
            int checked = binding.toggleLength.getCheckedButtonId();
            int length = checked == R.id.btn_len_express ? EXPRESS : checked == R.id.btn_len_quick ? QUICK : checked == R.id.btn_len_standard ? STANDARD : 0;
            boolean timer = binding.switchTimer.isChecked();
            boolean shuffle = binding.switchShuffle.isChecked();
            boolean exam = binding.toggleMode.getCheckedButtonId() == R.id.btn_mode_exam;
            Prefs.saveTestSetup(requireContext(), length, timer, shuffle);
            // "Full" means every question shown on the button. A pseudo topic would read 0 as its
            // own default length (15 for mixed practice), so it gets the count explicitly.
            int requested = length == 0 && categoryId < 0 ? count : length;
            // The switch is hidden for pseudo topics; saved questions are always drawn at random.
            startActivity(QuestionActivity.intent(requireContext(), categoryId, requested, timer, shuffle || categoryId < 0, exam));
            dismissAllowingStateLoss();
        });

        binding.layoutTopicTools.setVisibility(categoryId >= 0 ? View.VISIBLE : View.GONE);
        binding.btnPath.setContentDescription(getString(R.string.learning_path) + ". " + getString(R.string.learning_path_desc));
        binding.btnPath.setOnClickListener(v -> {
            startActivity(PathActivity.intent(requireContext(), categoryId));
            dismissAllowingStateLoss();
        });
        binding.btnStudy.setContentDescription(getString(R.string.study_mode) + ". " + getString(R.string.study_mode_desc));
        binding.btnStudy.setOnClickListener(v -> {
            startActivity(StudyActivity.intent(requireContext(), categoryId));
            dismissAllowingStateLoss();
        });
    }

    /** Two-line label: a small caption over a bold question count, so four options fit side by side. */
    private void lengthLabel(Button button, int label, int questions) {
        String caption = getString(label);
        SpannableStringBuilder text = new SpannableStringBuilder(caption).append('\n');
        int start = text.length();
        text.append(String.valueOf(questions));
        text.setSpan(new RelativeSizeSpan(0.8f), 0, caption.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new RelativeSizeSpan(1.2f), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setText(text);
        button.setContentDescription(caption + ", " + getResources().getQuantityString(R.plurals.question_count, questions, questions));
    }

    private void bindMode(boolean exam) {
        binding.txtModeDesc.setText(exam ? R.string.mode_exam_desc : R.string.mode_practice_desc);
        binding.txtTimerDesc.setText(exam ? R.string.exam_timer_desc : R.string.timer_option_desc);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
