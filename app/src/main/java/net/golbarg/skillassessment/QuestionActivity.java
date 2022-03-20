package net.golbarg.skillassessment;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.golbarg.skillassessment.CustomView.AnswerView;
import net.golbarg.skillassessment.CustomView.QuestionView;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.db.TableQuestionAnswer;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;
import java.util.Locale;

public class QuestionActivity extends AppCompatActivity {
    public static final String TAG = QuestionActivity.class.getName();
    public static int counter = 0;

    ArrayList<Question> questions = new ArrayList<>();

    TableQuestion tableQuestion;
    TableQuestionAnswer tableQuestionAnswer;

    ProgressBar progressBarStep;
    QuestionView questionViewTitle;
    AnswerView answerView1;
    AnswerView answerView2;
    AnswerView answerView3;
    AnswerView answerView4;

    ImageView imgClose;
    Button btnNextQuestion;
    TextView txtTimer;

    CountDownTimer countDownTimer;
    private Context context;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        context = getApplicationContext();
        DatabaseHandler databaseHandler = new DatabaseHandler(getApplicationContext());
        tableQuestion = new TableQuestion(databaseHandler);
        tableQuestionAnswer = new TableQuestionAnswer(databaseHandler);

        questions = tableQuestion.getQuestionsOf(2);

        imgClose = findViewById(R.id.img_close);
        progressBarStep = findViewById(R.id.progress_step);
        questionViewTitle = findViewById(R.id.question_view_title);

        answerView1 = findViewById(R.id.answer_view_1);
        answerView2 = findViewById(R.id.answer_view_2);
        answerView3 = findViewById(R.id.answer_view_3);
        answerView4 = findViewById(R.id.answer_view_4);

        btnNextQuestion = findViewById(R.id.btn_next_question);
        txtTimer = findViewById(R.id.txt_timer);

        btnNextQuestion.setOnClickListener(v -> {
            loadQuestion(counter++);
//            btnNextQuestion.setEnabled(false);
            countDown();
        });

        imgClose.setOnClickListener(v -> Toast.makeText(getApplicationContext(), "Closing Test", Toast.LENGTH_SHORT).show());
    }

    private void loadQuestion(int position) {
        if (position >= 0 && position < questions.size()) {
            progressBarStep.setProgress(position);
//            questionViewTitle.getTxtQuestionText().setText(UtilController.highlightQuestionText(questions.get(position).getTitle()));
            UtilController.highlightQuestionText(questionViewTitle, questions.get(position).getTitle());

            questions.get(position).setAnswers(tableQuestionAnswer.getAnswersOf(questions.get(position).getId()));
            
            if (questions.get(position).getAnswers().size() >= 1) {
                UtilController.highlightAnswerText(answerView1, questions.get(position).getAnswers().get(0).getTitle());
                answerView1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(questions.get(position).getAnswers().get(0).isCorrect()) {
                            Log.d(TAG, "Answer 1 Success");
                            Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Answer 1 Wrong");
                            Toast.makeText(getApplicationContext(), "Wrong", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            if (questions.get(position).getAnswers().size() >= 2) {
                UtilController.highlightAnswerText(answerView2, questions.get(position).getAnswers().get(1).getTitle());

                answerView2.setOnClickListener(v -> {
                    if(questions.get(position).getAnswers().get(1).isCorrect()) {
                        Log.d(TAG, "Answer 2 Success");
                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Answer 2 Wrong");
                        Toast.makeText(getApplicationContext(), "Wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (questions.get(position).getAnswers().size() >= 3) {
//                answerView3.setText(UtilController.highlightQuestionText(questions.get(position).getAnswers().get(2).getTitle()));
                UtilController.highlightAnswerText(answerView3, questions.get(position).getAnswers().get(2).getTitle());

                answerView3.setOnClickListener(v -> {
                    if(questions.get(position).getAnswers().get(2).isCorrect()) {
                        Log.d(TAG, "Answer 3 Success");
                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Answer 3 Wrong");
                        Toast.makeText(getApplicationContext(), "Wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (questions.get(position).getAnswers().size() >= 4) {
                UtilController.highlightAnswerText(answerView4, questions.get(position).getAnswers().get(3).getTitle());

                answerView4.setOnClickListener(v -> {
                    if(questions.get(position).getAnswers().get(3).isCorrect()) {
                        Log.d(TAG, "Answer 4 Success");
                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Answer 4 Wrong");
                        Toast.makeText(getApplicationContext(), "Wrong", Toast.LENGTH_SHORT).show();
                    }
                });
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
}
