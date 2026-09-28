package net.golbarg.skillassessment.ui.intro;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import net.golbarg.skillassessment.MainActivity;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ActivityIntroBinding;
import net.golbarg.skillassessment.databinding.ItemIntroPageBinding;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.UiUtils;

public class IntroActivity extends AppCompatActivity {

    private static final class Page {
        @StringRes final int title;
        @StringRes final int description;
        @DrawableRes final int icon;

        Page(int title, int description, int icon) {
            this.title = title;
            this.description = description;
            this.icon = icon;
        }
    }

    private static final Page[] PAGES = {
            new Page(R.string.intro_1_title, R.string.intro_1_desc, 0),
            new Page(R.string.intro_2_title, R.string.intro_2_desc, R.drawable.ic_bolt),
            new Page(R.string.intro_3_title, R.string.intro_3_desc, R.drawable.ic_check_circle),
            new Page(R.string.intro_4_title, R.string.intro_4_desc, R.drawable.ic_insights),
    };

    private ActivityIntroBinding binding;
    private String enteredName = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        UiUtils.enableEdgeToEdge(this);
        super.onCreate(savedInstanceState);
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarPadding(binding.root, true, true);

        binding.pager.setAdapter(new PageAdapter());
        new TabLayoutMediator(binding.dots, binding.pager, (tab, position) -> {
            tab.view.setClickable(false);
            tab.setContentDescription((position + 1) + " / " + PAGES.length);
        }).attach();

        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                boolean last = position == PAGES.length - 1;
                binding.btnNext.setText(last ? R.string.get_started : R.string.next);
                binding.btnSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            }
        });

        binding.btnSkip.setOnClickListener(v -> binding.pager.setCurrentItem(PAGES.length - 1));
        binding.btnNext.setOnClickListener(v -> {
            int current = binding.pager.getCurrentItem();
            if (current < PAGES.length - 1) binding.pager.setCurrentItem(current + 1);
            else finishIntro();
        });
    }

    private void finishIntro() {
        if (!enteredName.trim().isEmpty()) Prefs.setUserName(this, enteredName);
        Prefs.setIntroSeen(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private class PageAdapter extends RecyclerView.Adapter<PageHolder> {
        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PageHolder(ItemIntroPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            Page page = PAGES[position];
            ItemIntroPageBinding b = holder.binding;
            b.txtTitle.setText(page.title);
            b.txtDescription.setText(page.description);
            if (page.icon == 0) {
                b.imgIcon.setVisibility(View.GONE);
                b.imgLogo.setVisibility(View.VISIBLE);
                b.imgLogo.setImageResource(R.mipmap.ic_launcher_round);
            } else {
                b.imgLogo.setVisibility(View.GONE);
                b.imgIcon.setVisibility(View.VISIBLE);
                b.imgIcon.setImageResource(page.icon);
            }
            boolean last = position == PAGES.length - 1;
            b.layoutName.setVisibility(last ? View.VISIBLE : View.GONE);
            if (last) {
                b.editName.setText(enteredName);
                b.editName.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                    @Override public void afterTextChanged(Editable s) { enteredName = s.toString(); }
                });
            }
        }

        @Override
        public int getItemCount() {
            return PAGES.length;
        }
    }

    private static class PageHolder extends RecyclerView.ViewHolder {
        final ItemIntroPageBinding binding;

        PageHolder(ItemIntroPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
