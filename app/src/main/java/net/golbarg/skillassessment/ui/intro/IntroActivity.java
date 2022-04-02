package net.golbarg.skillassessment.ui.intro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import net.golbarg.skillassessment.MainActivity;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.models.ScreenItem;

import java.util.ArrayList;
import java.util.List;

public class IntroActivity extends AppCompatActivity {

    private ViewPager screenPager;
    IntroViewPagerAdapter introViewPagerAdapter;
    private LinearLayout mDotLayout;
    private TextView[] mDots;

    Button btnNext;
    int position = 0;
    Button btnGetStarted;
    Animation btnAnimation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //make the activity on full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // when this activity is about to be launch we need to check if its opened before or not
        /*if(restorePrefData()) {
            Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainActivity);
            finish();
        }*/


        setContentView(R.layout.activity_intro);

        //hide the action bar
//        getSupportActionBar().hide();

        btnNext = findViewById(R.id.btn_next);
        btnGetStarted = findViewById(R.id.btn_get_started);
        btnAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_animation);

        // fill list screen
        List<ScreenItem> mList = new ArrayList<>();
        mList.add(new ScreenItem("Skill Assessment", "this Application will help you practice Programming Skills", R.drawable.golbarg_logo_blue));
        mList.add(new ScreenItem("Categories", "Different categories to choose and practice", R.drawable.ic_java_original));
        mList.add(new ScreenItem("Questions", "select the correct Answer and enjoy learning by practicing", R.drawable.ic_check));
        mList.add(new ScreenItem("Performance", "watch your performance", R.drawable.ic_bar_chart));

        // setup viewpager
        screenPager = findViewById(R.id.screen_view_pager);
        introViewPagerAdapter = new IntroViewPagerAdapter(this, mList);
        screenPager.setAdapter(introViewPagerAdapter);

        // setup dot with viewpager
        mDotLayout = findViewById(R.id.dotsLayout);
        addDotsIndicator(0);
        screenPager.addOnPageChangeListener(viewListener);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                position = screenPager.getCurrentItem();

                if (position < mList.size()) {
                    position++;
                    screenPager.setCurrentItem(position);
                }

                if (position == mList.size() - 1) {
                    // TODO : show the get stated button and hide the indicator and the next button
                    loadLastScreen();
                }

            }
        });

        // get started button click listener
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainActivity);
                //also we need to save a boolean value to storage so next time when the user run the app
                // we could know that he is already check the intro screen activity
                // I'm going to use shared preferences to that process
                savePrefsData();
                finish();
            }
        });

    }

    public void addDotsIndicator(int position) {
        mDots = new TextView[4];
        mDotLayout.removeAllViews();

        for (int i = 0; i < mDots.length; i++) {
            mDots[i] = new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.black));
            mDotLayout.addView(mDots[i]);
        }

        if (mDots.length > 0) {
            mDots[position].setTextColor(getResources().getColor(R.color.design_default_color_error));
        }

    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addDotsIndicator(position);

            if (position == mDots.length - 1) {
                // TODO : show the get stated button and hide the indicator and the next button
                loadLastScreen();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    /* show the get stated button and hide the indicator and the next button */
    private void loadLastScreen() {
        btnNext.setVisibility(View.INVISIBLE);
        btnGetStarted.setVisibility(View.VISIBLE);
        mDotLayout.setVisibility(View.INVISIBLE);
        // TODO: add animation to get started button
        btnGetStarted.setAnimation(btnAnimation);

    }

    private void savePrefsData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("my_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("is_intro_opened", true);
        editor.commit();
    }

    private boolean restorePrefData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("my_prefs", MODE_PRIVATE);
        Boolean isIntroActivityOpenedBefore = pref.getBoolean("is_intro_opened", false);
        return isIntroActivityOpenedBefore.booleanValue();
    }

}
