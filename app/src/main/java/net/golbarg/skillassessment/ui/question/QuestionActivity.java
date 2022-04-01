package net.golbarg.skillassessment.ui.question;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
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
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.db.TableQuestionAnswer;
import net.golbarg.skillassessment.db.TableQuestionResult;
import net.golbarg.skillassessment.models.AnswerResponseType;
import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionAnswer;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;
import java.util.Locale;

public class QuestionActivity extends AppCompatActivity {
    public static final String TAG = QuestionActivity.class.getName();
    public static String[] AnswerOptions = {"A", "B", "C", "D", "E", "F", "G"};
    private Context context;
    private int currentQuestionIndex = 0;
    private Category selectedCategory;
    private ArrayList<Question> questions = new ArrayList<Question>();
    private QuestionResult questionResult;

    private DatabaseHandler databaseHandler;
    private TableCategory tableCategory;
    private TableQuestion tableQuestion;
    private TableQuestionAnswer tableQuestionAnswer;
    private TableBookmark tableBookmark;
    private TableQuestionResult tableQuestionResult;

    private ConstraintLayout mainLayout;
    private ImageView imgClose;
    private ProgressBar progressBarStep;
    private ImageButton btnBookmark;
    private QuestionView questionView;
    private ArrayList<AnswerView> answerViewsList = new ArrayList<>();
    private TextView txtTimer;
    private TextView txtQuestionTrack;

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        context = getApplicationContext();

        Intent intent = getIntent();
        int category_id = intent.getIntExtra("category_id", 14);

        initDbTables();
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

        txtTimer = findViewById(R.id.txt_timer);
        txtQuestionTrack = findViewById(R.id.txt_question_track);

        imgClose.setOnClickListener(view -> {
            showConfirmDialog();
        });

        btnBookmark.setOnClickListener(view -> {
            handleQuestionBookmark();
        });

        loadQuestion();
    }

    private void loadQuestion() {
        if(currentQuestionIndex < questions.size()) {
            loadQuestionDetails(currentQuestionIndex);
            startCountDownTimer();
        } else {
            long question_result_id =  tableQuestionResult.createGetId(questionResult);
            Intent questionResultIntent = new Intent(getBaseContext(), QuestionResultActivity.class);
            questionResultIntent.putExtra("question_result_id", question_result_id);
            Log.d("QuestionResult", String.valueOf(question_result_id));
            startActivity(questionResultIntent);
            finish();
        }
    }

    private void loadQuestionDetails(int selectedPosition) {
        validateBookmarkStatus(questions.get(selectedPosition));

        txtQuestionTrack.setText(String.format("%d/%d", selectedPosition + 1, questions.size()));
        progressBarStep.setProgress(selectedPosition);

        UtilController.highlightQuestionText(questionView, questions.get(selectedPosition).getTitle(), selectedCategory, context);
        questions.get(selectedPosition).setAnswers(tableQuestionAnswer.getAnswersOf(questions.get(selectedPosition).getId()));

        for(int i = 0; i < answerViewsList.size(); i++) {
            if (i < questions.get(selectedPosition).getAnswers().size()) {
                QuestionAnswer selectedAnswer = questions.get(selectedPosition).getAnswers().get(i);
                AnswerView answerView = answerViewsList.get(i);
                answerView.getTxtAnswerOption().setText(AnswerOptions[i]);
                UtilController.highlightAnswerText(answerView, selectedAnswer.getTitle(), selectedCategory, getApplicationContext());

                answerView.setOnClickListener(view -> {
                    setAnswersClickable(false);
                    Log.d(TAG, "question_result: " + questionResult.toString());
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
                currentQuestionIndex++;
                loadQuestion();
                setAnswersClickable(true);
            }
        });
        if(responseType != AnswerResponseType.NO_ANSWER) {
            snackbar.show();
        }
    }

    private void startCountDownTimer() {
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


    private void handleQuestionBookmark() {
        try {
            if(currentQuestionIndex < questions.size()) {
                Bookmark isBookmarked = tableBookmark.getByQuestionId(questions.get(currentQuestionIndex).getId());
                if(isBookmarked != null) {
                    tableBookmark.delete(isBookmarked);
                    int color = context.getResources().getColor(R.color.red_500);
                    UtilController.showSnackMessage(mainLayout, context.getString(R.string.bookmark_deleted), color, R.id.layout_footer);
                } else {
                    tableBookmark.create(new Bookmark(questions.get(currentQuestionIndex).getId()));
                    int color = context.getResources().getColor(R.color.green_500);
                    UtilController.showSnackMessage(mainLayout, context.getString(R.string.added_to_bookmark), color, R.id.layout_footer);
                }

                validateBookmarkStatus(questions.get(currentQuestionIndex));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validateBookmarkStatus(Question question) {
        Bookmark isBookmarked = tableBookmark.getByQuestionId(question.getId());
        if(isBookmarked != null) {
            btnBookmark.setImageDrawable(context.getDrawable(R.drawable.ic_bookmark_yes));
        } else {
            btnBookmark.setImageDrawable(context.getDrawable(R.drawable.ic_bookmark_no));
        }
    }

    private void showConfirmDialog() {
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
    }

    private void setAnswersClickable(boolean isEnable) {
        for (int i = 0; i < answerViewsList.size(); i++) {
            answerViewsList.get(i).setClickable(isEnable);
            answerViewsList.get(i).getMainLayout().invalidate();
        }
    }

    private void initAnswerViews() {
        AnswerView answerView1 = findViewById(R.id.answer_view_1);
        AnswerView answerView2 = findViewById(R.id.answer_view_2);
        AnswerView answerView3 = findViewById(R.id.answer_view_3);
        AnswerView answerView4 = findViewById(R.id.answer_view_4);
        AnswerView answerView5 = findViewById(R.id.answer_view_5);

        answerViewsList.add(answerView1);
        answerViewsList.add(answerView2);
        answerViewsList.add(answerView3);
        answerViewsList.add(answerView4);
        answerViewsList.add(answerView5);
    }

    private void initDbTables() {
        databaseHandler = new DatabaseHandler(getApplicationContext());
        tableCategory = new TableCategory(databaseHandler);
        tableQuestion = new TableQuestion(databaseHandler);
        tableQuestionAnswer = new TableQuestionAnswer(databaseHandler);
        tableBookmark = new TableBookmark(databaseHandler);
        tableQuestionResult = new TableQuestionResult(databaseHandler);
    }
}
