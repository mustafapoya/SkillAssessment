package net.golbarg.skillassessment.ui.home;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.databinding.SheetTestSetupBinding;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.ui.widget.AppBottomSheet;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.UiUtils;

/** Lets the user pick test length, timer and shuffle before starting a topic. */
public class TestSetupSheet extends AppBottomSheet {
    public static final String TAG = "TestSetupSheet";
    private static final int QUICK = 10;
    private static final int STANDARD = 25;

    private static final String ARG_ID = "id";
    private static final String ARG_SLUG = "slug";
    private static final String ARG_COUNT = "count";
    private static final String ARG_ATTEMPTS = "attempts";
    private static final String ARG_BEST = "best";

    private SheetTestSetupBinding binding;

    public static TestSetupSheet newInstance(Category category) {
        Bundle args = new Bundle();
        args.putInt(ARG_ID, category.getId());
        args.putString(ARG_SLUG, category.getSlug());
        args.putInt(ARG_COUNT, category.getNumberOfQuestion());
        args.putInt(ARG_ATTEMPTS, category.getAttempts());
        args.putInt(ARG_BEST, category.getBestScore());
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
        boolean dark = UiUtils.isNightMode(requireContext());

        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(UiUtils.dp(requireContext(), 16));
        badge.setColor(CategoryNames.badgeBackground(slug, dark));
        binding.txtBadge.setBackground(badge);
        binding.txtBadge.setTextColor(CategoryNames.badgeForeground(slug, dark));
        binding.txtBadge.setText(CategoryNames.monogram(slug));
        binding.txtTitle.setText(CategoryNames.displayName(slug));

        String questions = getResources().getQuantityString(R.plurals.question_count, count, count);
        if (categoryId < 0) {
            binding.txtSubtitle.setText(questions + " · " + getString(R.string.mixed_practice_desc));
        } else if (attempts > 0 && best >= 0) {
            String tests = getResources().getQuantityString(R.plurals.tests_taken, attempts, attempts);
            binding.txtSubtitle.setText(questions + " · " + getString(R.string.attempts_best, tests, best));
        } else {
            binding.txtSubtitle.setText(questions + " · " + getString(R.string.not_attempted));
        }

        binding.btnLenQuick.setText(getString(R.string.length_quick) + "\n" + QUICK);
        binding.btnLenStandard.setText(getString(R.string.length_standard) + "\n" + STANDARD);
        binding.btnLenFull.setText(getString(R.string.length_full) + "\n" + count);
        binding.btnLenQuick.setVisibility(count > QUICK ? View.VISIBLE : View.GONE);
        binding.btnLenStandard.setVisibility(count > STANDARD ? View.VISIBLE : View.GONE);

        int preferred = Prefs.getTestLength(requireContext());
        if (preferred == QUICK && count > QUICK) binding.toggleLength.check(R.id.btn_len_quick);
        else if (preferred == STANDARD && count > STANDARD) binding.toggleLength.check(R.id.btn_len_standard);
        else binding.toggleLength.check(R.id.btn_len_full);

        binding.switchTimer.setChecked(Prefs.isTimerEnabled(requireContext()));
        binding.switchShuffle.setChecked(Prefs.isShuffleEnabled(requireContext()));
        // Mixed tests are always drawn at random.
        if (categoryId < 0) ((View) binding.switchShuffle.getParent()).setVisibility(View.GONE);

        binding.toggleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) bindMode(checkedId == R.id.btn_mode_exam);
        });
        bindMode(false);

        binding.btnStart.setOnClickListener(v -> {
            int checked = binding.toggleLength.getCheckedButtonId();
            int length = checked == R.id.btn_len_quick ? QUICK : checked == R.id.btn_len_standard ? STANDARD : 0;
            boolean timer = binding.switchTimer.isChecked();
            boolean shuffle = binding.switchShuffle.isChecked();
            boolean exam = binding.toggleMode.getCheckedButtonId() == R.id.btn_mode_exam;
            Prefs.saveTestSetup(requireContext(), length, timer, shuffle);
            binding.btnStart.setEnabled(false);
            AdManager.showInterstitial(requireActivity(), AdManager.Interstitial.TEST_START, () -> {
                if (!isAdded()) return;
                startActivity(QuestionActivity.intent(requireContext(), categoryId, length, timer, shuffle, exam));
                dismissAllowingStateLoss();
            });
        });
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
