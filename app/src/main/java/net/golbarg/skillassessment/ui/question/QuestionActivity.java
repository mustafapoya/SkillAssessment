package net.golbarg.skillassessment.ui.question;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.golbarg.skillassessment.CustomView.AnswerView;
import net.golbarg.skillassessment.CustomView.QuestionView;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableBookmark;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.db.TableQuestionAnswer;
import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionAnswer;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;
import java.util.Locale;

public class QuestionActivity extends AppCompatActivity {
    public static final String TAG = QuestionActivity.class.getName();
    public int counter = 0;
    Category selectedCategory;
    ArrayList<Question> questions = new ArrayList<>();

    DatabaseHandler databaseHandler;
    TableCategory tableCategory;
    TableQuestion tableQuestion;
    TableQuestionAnswer tableQuestionAnswer;
    TableBookmark tableBookmark;

    ImageView imgClose;
    ProgressBar progressBarStep;
    ImageButton btnBookmark;

    QuestionView questionViewTitle;

    ArrayList<AnswerView> answerViewsList = new ArrayList<AnswerView>();
    AnswerView answerView1;
    AnswerView answerView2;
    AnswerView answerView3;
    AnswerView answerView4;
    AnswerView answerView5;

    Button btnNextQuestion;
    TextView txtTimer;
    TextView txtQuestionTrack;

    CountDownTimer countDownTimer;
    private Context context;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        context = getApplicationContext();
        databaseHandler = new DatabaseHandler(getApplicationContext());
        tableCategory = new TableCategory(databaseHandler);
        tableQuestion = new TableQuestion(databaseHandler);
        tableQuestionAnswer = new TableQuestionAnswer(databaseHandler);
        tableBookmark = new TableBookmark(databaseHandler);

        Intent intent = getIntent();
        int category_id = intent.getIntExtra("category_id", 2);
        selectedCategory = tableCategory.get(category_id);

        questions = tableQuestion.getQuestionsOf(selectedCategory.getId());

        imgClose = findViewById(R.id.img_close);
        progressBarStep = findViewById(R.id.progress_step);
        progressBarStep.setMax(questions.size());
        btnBookmark = findViewById(R.id.btn_bookmark);

        questionViewTitle = findViewById(R.id.question_view_title);

        answerView1 = findViewById(R.id.answer_view_1);
        answerView2 = findViewById(R.id.answer_view_2);
        answerView3 = findViewById(R.id.answer_view_3);
        answerView4 = findViewById(R.id.answer_view_4);
        answerView5 = findViewById(R.id.answer_view_5);

        answerViewsList.add(answerView1);
        answerViewsList.add(answerView2);
        answerViewsList.add(answerView3);
        answerViewsList.add(answerView4);
        answerViewsList.add(answerView5);

        btnNextQuestion = findViewById(R.id.btn_next_question);
        txtTimer = findViewById(R.id.txt_timer);
        txtQuestionTrack = findViewById(R.id.txt_question_track);

        btnNextQuestion.setOnClickListener(v -> {
            loadQuestion(counter++);
            countDown();
        });
        btnNextQuestion.performClick();
        imgClose.setOnClickListener(v -> Toast.makeText(getApplicationContext(), "Closing Test", Toast.LENGTH_SHORT).show());

        btnBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Bookmark isBookmarked = tableBookmark.getByQuestionId(questions.get(counter).getId());
                    if(isBookmarked != null) {
                        tableBookmark.deleteByQuestionId(questions.get(counter).getId());
                        Toast.makeText(context, R.string.bookmark_deleted, Toast.LENGTH_SHORT).show();
                        btnBookmark.setImageDrawable(context.getDrawable(R.drawable.ic_bookmark_no));
                    } else {
                        Bookmark newBookmark = new Bookmark();
                        newBookmark.setQuestionId(questions.get(counter).getId());
                        tableBookmark.create(newBookmark);
                        Toast.makeText(context, R.string.added_to_bookmark, Toast.LENGTH_SHORT).show();
                        btnBookmark.setImageDrawable(context.getDrawable(R.drawable.ic_bookmark_yes));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadQuestion(int position) {
        if (position >= 0 && position < questions.size()) {
            validateBookmarkStatus(questions.get(position));
            txtQuestionTrack.setText(position + 1 + "/" + questions.size());
            progressBarStep.setProgress(position);

            UtilController.highlightQuestionText(questionViewTitle, questions.get(position).getTitle(), selectedCategory, getApplicationContext());

            questions.get(position).setAnswers(tableQuestionAnswer.getAnswersOf(questions.get(position).getId()));

            //
            for(int i = 0; i < answerViewsList.size(); i++) {
                if(i < questions.get(position).getAnswers().size()) {
                    AnswerView answerView = answerViewsList.get(i);
                    QuestionAnswer selectedAnswer = questions.get(position).getAnswers().get(i);

                    UtilController.highlightAnswerText(answerView, selectedAnswer.getTitle(), selectedCategory, getApplicationContext());
                    answerView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(selectedAnswer.isCorrect()) {
                                Log.d(TAG, "Answer 1 Success");
                                Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "Answer 1 Wrong");
                                Toast.makeText(getApplicationContext(), "Wrong", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    answerViewsList.get(i).setVisibility(View.GONE);
                }
            }
        }
    }

    private void countDown() {
        if(countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(91000, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisUntilFinished = millisUntilFinished / 1000;
                long minutes = millisUntilFinished / 60;
                long seconds = millisUntilFinished % 60;
                txtTimer.setText(String.format(Locale.ENGLISH,"%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                btnNextQuestion.setEnabled(true);
            }
        };

        countDownTimer.start();
    }

    private void validateBookmarkStatus(Question question) {
        Bookmark isBookmarked = tableBookmark.getByQuestionId(question.getId());
        if(isBookmarked != null) {
            btnBookmark.setImageDrawable(context.getDrawable(R.drawable.ic_bookmark_yes));
        } else {
            btnBookmark.setImageDrawable(context.getDrawable(R.drawable.ic_bookmark_no));
        }
    }
}
