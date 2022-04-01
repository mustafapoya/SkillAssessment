package net.golbarg.skillassessment.ui.question;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.golbarg.skillassessment.CustomView.AnswerView;
import net.golbarg.skillassessment.CustomView.QuestionView;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableBookmark;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableQuestionResult;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.db.TableQuestionAnswer;
import net.golbarg.skillassessment.models.AnswerResponseType;
import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionAnswer;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;
import java.util.Locale;

public class QuestionActivity extends AppCompatActivity {
    public static final String TAG = QuestionActivity.class.getName();
    private Context context;
    public static String[] AnswerOptions = {"A", "B", "C", "D", "E", "F", "G"};

    public int currentPosition = -1;
    Category selectedCategory;
    ArrayList<Question> questions = new ArrayList<>();
    QuestionResult questionResult;

    DatabaseHandler databaseHandler;
    TableCategory tableCategory;
    TableQuestion tableQuestion;
    TableQuestionAnswer tableQuestionAnswer;
    TableBookmark tableBookmark;
    TableQuestionResult tableQuestionResult;

    ConstraintLayout mainLayout;
    ImageView imgClose;
    ProgressBar progressBarStep;
    ImageButton btnBookmark;

    QuestionView questionView;
    ArrayList<AnswerView> answerViewsList = new ArrayList<>();

    Button btnNextQuestion;
    TextView txtTimer;
    TextView txtQuestionTrack;

    CountDownTimer countDownTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        context = getApplicationContext();

        Intent intent = getIntent();
        int category_id = intent.getIntExtra("category_id", 2);

        initDB();
        selectedCategory = tableCategory.get(category_id);
        questions = tableQuestion.getQuestionsOf(selectedCategory.getId());
        questionResult = new QuestionResult(selectedCategory.getId());

        getSupportActionBar().setTitle(selectedCategory.getTitle().replace("-", " "));

        mainLayout = findViewById(R.id.layout_main_layout);
        imgClose = findViewById(R.id.img_close);
        progressBarStep = findViewById(R.id.progress_step);
        progressBarStep.setMax(questions.size());
        btnBookmark = findViewById(R.id.btn_bookmark);

        questionView = findViewById(R.id.question_view_title);
        initAnswerViews();

        btnNextQuestion = findViewById(R.id.btn_next_question);
        txtTimer = findViewById(R.id.txt_timer);
        txtQuestionTrack = findViewById(R.id.txt_question_track);

        btnNextQuestion.setOnClickListener(v -> {
            questionResult.incrementNoAnswer();
            handleNextQuestion();
        });
        btnNextQuestion.performClick();

        imgClose.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("End Test?");
            builder.setMessage("Are you Sure you want to end Test \nThe Result with not be saved?");
            builder.setPositiveButton("END", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(context, "Exam End", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
            builder.setIcon(R.drawable.ic_info);
            builder.setNegativeButton("CANCEL", null);
            builder.show();
        });

        btnBookmark.setOnClickListener(view -> {
            try {
                Bookmark isBookmarked = tableBookmark.getByQuestionId(questions.get(currentPosition).getId());
                if(isBookmarked != null) {
                    tableBookmark.delete(isBookmarked);
                    UtilController.showSnackMessage(mainLayout, context.getString(R.string.bookmark_deleted),
                            context.getResources().getColor(R.color.red_500), R.id.layout_footer);
                } else {
                    tableBookmark.create(new Bookmark(questions.get(currentPosition).getId()));
                    UtilController.showSnackMessage(mainLayout, context.getString(R.string.added_to_bookmark),
                            context.getResources().getColor(R.color.green_500), R.id.layout_footer);
                }

                validateBookmarkStatus(questions.get(currentPosition));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleNextQuestion() {
        currentPosition++;
        if (currentPosition >= 0 && currentPosition < questions.size()) {
            loadQuestion(currentPosition);
        }

        if(currentPosition >= 0 && currentPosition == questions.size()-1) {
            btnNextQuestion.setText(R.string.finish);
            return;
        }

        if(btnNextQuestion.getText().toString().equals(context.getString(R.string.finish))) {
            tableQuestionResult.create(questionResult);

        }
        countDownTime();
    }

    private void initDB() {
        databaseHandler = new DatabaseHandler(getApplicationContext());
        tableCategory = new TableCategory(databaseHandler);
        tableQuestion = new TableQuestion(databaseHandler);
        tableQuestionAnswer = new TableQuestionAnswer(databaseHandler);
        tableBookmark = new TableBookmark(databaseHandler);
        tableQuestionResult = new TableQuestionResult(databaseHandler);
    }

    private void initAnswerViews() {
        AnswerView  answerView1 = findViewById(R.id.answer_view_1);
        AnswerView  answerView2 = findViewById(R.id.answer_view_2);
        AnswerView  answerView3 = findViewById(R.id.answer_view_3);
        AnswerView  answerView4 = findViewById(R.id.answer_view_4);
        AnswerView  answerView5 = findViewById(R.id.answer_view_5);

        answerViewsList.add(answerView1);
        answerViewsList.add(answerView2);
        answerViewsList.add(answerView3);
        answerViewsList.add(answerView4);
        answerViewsList.add(answerView5);
    }

    private void setAnswersClickable(boolean isEnable) {
        for (int i = 0; i < answerViewsList.size(); i++) {
            answerViewsList.get(i).setClickable(isEnable);
            answerViewsList.get(i).getMainLayout().invalidate();
        }
    }

    private void loadQuestion(int position) {

        validateBookmarkStatus(questions.get(position));

        txtQuestionTrack.setText(String.format("%d/%d", position + 1, questions.size()));
        progressBarStep.setProgress(position);

        UtilController.highlightQuestionText(questionView, questions.get(position).getTitle(), selectedCategory, getApplicationContext());

        questions.get(position).setAnswers(tableQuestionAnswer.getAnswersOf(questions.get(position).getId()));

        //
        for(int i = 0; i < answerViewsList.size(); i++) {
            if(i < questions.get(position).getAnswers().size()) {
                QuestionAnswer selectedAnswer = questions.get(position).getAnswers().get(i);

                AnswerView answerView = answerViewsList.get(i);
                answerView.getTxtAnswerOption().setText(AnswerOptions[i]);
                UtilController.highlightAnswerText(answerView, selectedAnswer.getTitle(), selectedCategory, getApplicationContext());
                answerView.setOnClickListener(view -> {
                    setAnswersClickable(false);
                    if(selectedAnswer.isCorrect()) {
                        handleAnswerClick(AnswerResponseType.CORRECT);
                    } else {
                        handleAnswerClick(AnswerResponseType.WRONG);
                    }
                });
            } else {
                answerViewsList.get(i).setVisibility(View.GONE);
            }
        }
    }

    private void handleAnswerClick(AnswerResponseType responseType) {
        int text = 0;
        int color = 0;

        if(responseType == AnswerResponseType.CORRECT) {
            text = R.string.correct;
            color = context.getResources().getColor(R.color.green_500);
            questionResult.incrementCorrectAnswer();
        } else if(responseType == AnswerResponseType.WRONG) {
            text = R.string.wrong;
            color = context.getResources().getColor(R.color.red_500);
            questionResult.incrementWrongAnswer();
        } else {
            text = R.string.no_response;
            color = context.getResources().getColor(R.color.gray);
            questionResult.incrementNoAnswer();
        }

        Snackbar snackbar = UtilController.createSnackBar(mainLayout, context.getString(text), color, R.id.layout_footer);
        snackbar.setDuration(1200);
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);

        snackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                handleNextQuestion();
                setAnswersClickable(true);
            }
        });
        if(responseType != AnswerResponseType.NO_ANSWER) {
            snackbar.show();
        }
    }

    private void countDownTime() {
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
                handleAnswerClick(AnswerResponseType.NO_ANSWER);
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
