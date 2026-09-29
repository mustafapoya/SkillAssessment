package net.golbarg.skillassessment.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ItemTopicProgressBinding;
import net.golbarg.skillassessment.databinding.SheetInterviewPacksBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.InterviewPack;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.ui.widget.AppBottomSheet;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.CategoryNames;

import java.util.ArrayList;
import java.util.List;

/** Lists the interview packs with how many of their topics are unlocked, and starts one as an exam. */
public class InterviewPacksSheet extends AppBottomSheet {
    public static final String TAG = "InterviewPacksSheet";

    private static final class PackState {
        InterviewPack pack;
        List<String> unlocked;
        int best;
    }

    private SheetInterviewPacksBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetInterviewPacksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        QuizRepository repository = QuizRepository.get(requireContext());
        Async.run(getViewLifecycleOwner(), () -> {
            List<PackState> states = new ArrayList<>();
            for (InterviewPack pack : InterviewPack.values()) {
                PackState s = new PackState();
                s.pack = pack;
                s.unlocked = repository.getUnlockedPackTopics(pack);
                s.best = repository.getBestScore(pack.id);
                states.add(s);
            }
            return states;
        }, states -> {
            if (binding == null || states == null) return;
            for (PackState s : states) addRow(s);
        });
    }

    private void addRow(PackState s) {
        ItemTopicProgressBinding row = ItemTopicProgressBinding.inflate(getLayoutInflater(), binding.listPacks, false);
        CategoryNames.styleBadge(row.txtBadge, s.pack.slug, 12);
        String title = getString(s.pack.title);
        row.txtTitle.setText(title);
        int total = s.pack.topics.size();
        row.progress.setProgressCompat(s.unlocked.size() * 100 / total, false);
        row.txtSubtitle.setText(getString(R.string.pack_topics_unlocked, s.unlocked.size(), total));
        row.txtPercent.setText(s.best >= 0 ? getString(R.string.best_score, s.best) : "");
        boolean available = !s.unlocked.isEmpty();
        row.getRoot().setAlpha(available ? 1f : 0.6f);
        row.getRoot().setContentDescription(title + ", " + row.txtSubtitle.getText());
        row.getRoot().setOnClickListener(v -> {
            if (!available) {
                List<String> names = new ArrayList<>();
                for (String slug : s.pack.topics) names.add(CategoryNames.displayName(slug));
                Toast.makeText(requireContext(), getString(R.string.pack_locked, TextUtils.join(", ", names)), Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(QuestionActivity.packIntent(requireContext(), s.pack));
            dismissAllowingStateLoss();
        });
        binding.listPacks.addView(row.getRoot());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
