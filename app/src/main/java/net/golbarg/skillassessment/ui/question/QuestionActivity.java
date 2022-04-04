package net.golbarg.skillassessment.ui.question;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
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
import net.golbarg.skillassessment.ui.dialog.LifeDialog;
import net.golbarg.skillassessment.util.CountDownTimerWithPause;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;
import java.util.Locale;

public class QuestionActivity extends AppCompatActivity {
    public static final String TAG = QuestionActivity.class.getName();
    public static String[] AnswerOptions = {"A", "B", "C", "D", "E", "F", "G", "H", "J"};
    private Context context;
    private int currentQuestionIndex = 0;
    private Category selectedCategory;
    private ArrayList<Question> questions = new ArrayList<>();
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
    private ImageView imgLife;
    private TextView txtLife;
    private ImageButton btnBookmark;
    private QuestionView questionView;
    private ArrayList<AnswerView> answerViewsList = new ArrayList<>();
    private TextView txtTimer;
    private TextView txtQuestionTrack;
    private Button btnHandleQuestion;

    private CountDownTimerWithPause countDownTimer;
    private int numberOfLife = 9;
    private int selectedAnswerIndex = -1;
    private int correctAnswerIndex = -1;

    LifeDialog lifeDialog;

    private InterstitialAd mInterstitialAd;

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
        imgLife = findViewById(R.id.img_life);
        txtLife = findViewById(R.id.txt_life);
        updateLifeText(numberOfLife);
        btnBookmark = findViewById(R.id.btn_bookmark);

        questionView = findViewById(R.id.question_view_title);
        initAnswerViews();

        txtTimer = findViewById(R.id.txt_timer);
        txtQuestionTrack = findViewById(R.id.txt_question_track);
        btnHandleQuestion = findViewById(R.id.btn_handle_question);

        imgClose.setOnClickListener(view -> showConfirmDialog());

        txtLife.setOnClickListener(view -> {
            handleLifeDialog();
        });

        imgLife.setOnClickListener(view -> {
            handleLifeDialog();
        });

        btnBookmark.setOnClickListener(view -> handleQuestionBookmark());

        btnHandleQuestion.setOnClickListener(view -> {
            if(btnHandleQuestion.getText().equals(context.getResources().getString(R.string.check))) {
                if(selectedAnswerIndex == correctAnswerIndex) {
                    handleAnswerClick(AnswerResponseType.CORRECT);
                } else {
                    handleAnswerClick(AnswerResponseType.WRONG);
                }
            } else {
                handleAnswerClick(AnswerResponseType.NO_ANSWER);
            }
        });

        loadQuestion();
        lifeDialog = new LifeDialog(QuestionActivity.this);
    }

    private void handleLifeDialog() {
        if(lifeDialog != null && !lifeDialog.isVisible()) {
            Log.d(TAG, "handleLifeDialog: View the Life Dialog");
            countDownTimer.pause();
            lifeDialog.show(getSupportFragmentManager(), LifeDialog.TAG);
        }
    }

    private void loadQuestion() {
        if(currentQuestionIndex < questions.size()) {
            loadQuestionDetails(currentQuestionIndex);
            startCountDownTimer();
        } else {
            gotoFinishActivity();
        }
    }

    public void gotoFinishActivity() {
        for(int i = currentQuestionIndex; i < questions.size(); i++) {
            questionResult.incrementNoAnswer();
        }
        long question_result_id =  tableQuestionResult.createGetId(questionResult);
        Intent questionResultIntent = new Intent(getBaseContext(), QuestionResultActivity.class);
        questionResultIntent.putExtra("question_result_id", question_result_id);
        if(countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if(lifeDialog != null) {
            lifeDialog.dismiss();
            lifeDialog = null;
        }

        loadInterstitialAd();

        if(mInterstitialAd != null) {
            mInterstitialAd.show(this);
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    startActivity(questionResultIntent);
                    finish();
                }
            });
        } else {
            startActivity(questionResultIntent);
            finish();
        }

    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        /* real ad Unit: ca-app-pub-1361000594268534/6220605444 */
        /* test ad Unit: ca-app-pub-3940256099942544/1033173712 */
        InterstitialAd.load(context,"ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        mInterstitialAd = null;
                    }
                });

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

                if(selectedAnswer.isCorrect()) {
                    correctAnswerIndex = i;
                }
                int finalI = i;
                answerView.setOnClickListener(view -> {
                    selectedAnswerIndex = finalI;
                    // TODO: Firstly Handle Selection of Question Answer, Then Check the Answer, Go to Next Question
                    setSelectedAnswer(selectedAnswerIndex);
                    btnHandleQuestion.setText(context.getResources().getString(R.string.check));
                });
            } else {
                answerViewsList.get(i).setVisibility(View.GONE);
            }
        }
    }

    private void handleAnswerClick(AnswerResponseType responseType) {
        int text;
        int color;

        if(responseType == AnswerResponseType.CORRECT) {
            text = R.string.correct;
            color = context.getResources().getColor(R.color.green_500);
            questionResult.incrementCorrectAnswer();
        } else if(responseType == AnswerResponseType.WRONG) {
            text = R.string.wrong;
            color = context.getResources().getColor(R.color.red_500);
            questionResult.incrementWrongAnswer();
            setNumberOfLife(--numberOfLife);
        } else {
            text = R.string.no_response;
            color = context.getResources().getColor(R.color.gray);
            questionResult.incrementNoAnswer();
            setNumberOfLife(--numberOfLife);
        }

        if(getNumberOfLife() <= 0) {
            handleLifeDialog();
            return;
        }

        Snackbar snackbar = UtilController.createSnackBar(mainLayout, context.getString(text), color, R.id.layout_footer);
        snackbar.setDuration(1200);
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);

        snackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                processQuestionAnswer();
            }
        });

        if(responseType != AnswerResponseType.NO_ANSWER) {
            snackbar.show();
        } else {
            processQuestionAnswer();
        }
    }

    private void processQuestionAnswer() {
        currentQuestionIndex++;
        loadQuestion();
        correctAnswerIndex = -1;
        selectedAnswerIndex = -1;
        setSelectedAnswer(selectedAnswerIndex);
        btnHandleQuestion.setText(context.getResources().getString(R.string.skip));
    }

    private void startCountDownTimer() {
        if(countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        //91000
        countDownTimer = new CountDownTimerWithPause(61000, 500, true) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisUntilFinished = millisUntilFinished / 1000;
                long minutes = millisUntilFinished / 60;
                long seconds = millisUntilFinished % 60;
                txtTimer.setText(String.format(Locale.ENGLISH,"%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {

                if(getNumberOfLife() > 0) {
                    handleAnswerClick(AnswerResponseType.NO_ANSWER);
                } else {
                    if(!lifeDialog.isVisible()) {
                        handleLifeDialog();
                    }
                }

            }

        };

        countDownTimer.create();
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
        builder.setPositiveButton("END", (dialog, which) -> {
            Toast.makeText(context, "Exam End", Toast.LENGTH_SHORT).show();
            if(countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
            finish();
        });
        builder.setIcon(R.drawable.ic_info);
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(countDownTimer != null) {
                    countDownTimer.resume();
                }
            }
        });
        builder.setCancelable(false);
        builder.show();

        if(countDownTimer != null) {
            countDownTimer.pause();
        }

    }

    private void setSelectedAnswer(int index) {
        for(int i = 0; i < answerViewsList.size(); i++) {
            answerViewsList.get(i).setSelected(i == index);
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

    private void updateLifeText(int life) {
        txtLife.setText(String.valueOf(life));
        txtLife.invalidate();
    }

    public int getNumberOfLife() {
        return this.numberOfLife;
    }

    public void setNumberOfLife(int newLife) {
        if(newLife > 0) {
            this.numberOfLife = newLife;
        } else {
            this.numberOfLife = 0;
        }
        updateLifeText(this.numberOfLife);
    }

    public CountDownTimerWithPause getCountDownTimer() {
        return countDownTimer;
    }
}
