package net.golbarg.skillassessment;

import android.os.Bundle;
import android.os.CountDownTimer;
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

    ImageView imgClose;
    Button btnNextQuestion;
    TextView txtTimer;

    CountDownTimer countDownTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

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
        btnNextQuestion = findViewById(R.id.btn_next_question);
        txtTimer = findViewById(R.id.txt_timer);

        btnNextQuestion.setOnClickListener(v -> {
            loadQuestion(counter++);
            btnNextQuestion.setEnabled(false);
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
            }

            if (questions.get(position).getAnswers().size() >= 2) {
                UtilController.highlightAnswerText(answerView2, questions.get(position).getAnswers().get(1).getTitle());
            }

            if (questions.get(position).getAnswers().size() >= 3) {
//                answerView3.setText(UtilController.highlightQuestionText(questions.get(position).getAnswers().get(2).getTitle()));
                UtilController.highlightAnswerText(answerView3, questions.get(position).getAnswers().get(0).getTitle());
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
