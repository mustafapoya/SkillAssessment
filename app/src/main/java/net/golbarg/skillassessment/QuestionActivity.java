package net.golbarg.skillassessment;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.db.TableQuestionAnswer;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;

public class QuestionActivity extends AppCompatActivity {
    public static final String TAG = QuestionActivity.class.getName();
    public static int counter = 0;

    private float x1, x2;
    static final int MIN_DISTANCE = 150;
    ArrayList<Question> questions = new ArrayList<>();

    TableQuestion tableQuestion;
    TableQuestionAnswer tableQuestionAnswer;

    ProgressBar progressBarStep;
    TextView txtQuestionTitle;
    TextView txtAnswer1;
    TextView txtAnswer2;
    TextView txtAnswer3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        DatabaseHandler databaseHandler = new DatabaseHandler(getApplicationContext());
        tableQuestion = new TableQuestion(databaseHandler);
        tableQuestionAnswer = new TableQuestionAnswer(databaseHandler);

        questions = tableQuestion.getQuestionsOf(2);

        progressBarStep = findViewById(R.id.progress_step);
        txtQuestionTitle = findViewById(R.id.txt_question_title);
        txtAnswer1 = findViewById(R.id.txt_answer_1);
        txtAnswer2 = findViewById(R.id.txt_answer_2);
        txtAnswer3 = findViewById(R.id.txt_answer_3);

        Button btnNextQuestion = findViewById(R.id.btn_next_question);

        btnNextQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadQuestion(counter++);
            }
        });

    }

    private void loadQuestion(int position) {
        if (position > 0 && position < questions.size()) {
            progressBarStep.setProgress(position);
            txtQuestionTitle.setText(UtilController.highlightQuestionText(questions.get(counter).getTitle()));

            questions.get(counter).setAnswers(tableQuestionAnswer.getAnswersOf(questions.get(counter).getId()));

            if (questions.get(counter).getAnswers().size() >= 1) {
                txtAnswer1.setText(UtilController.highlightQuestionText(questions.get(counter).getAnswers().get(0).getTitle()));
            }

            if (questions.get(counter).getAnswers().size() >= 2) {
                txtAnswer2.setText(UtilController.highlightQuestionText(questions.get(counter).getAnswers().get(1).getTitle()));
            }

            if (questions.get(counter).getAnswers().size() >= 3) {
                txtAnswer3.setText(UtilController.highlightQuestionText(questions.get(counter).getAnswers().get(2).getTitle()));
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    if (x2 > x1)
                    {
//                        Toast.makeText(this, "Left to Right swipe [Next]", Toast.LENGTH_SHORT).show ();
                        loadQuestion(counter--);
                    }
                    // Right to left swipe action
                    else
                    {
//                        Toast.makeText(this, "Right to Left swipe [Previous]", Toast.LENGTH_SHORT).show ();
                        loadQuestion(counter++);
                    }
                } else {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
