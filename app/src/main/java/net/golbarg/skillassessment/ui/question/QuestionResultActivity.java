package net.golbarg.skillassessment.ui.question;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableQuestionResult;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.QuestionResult;

public class QuestionResultActivity extends AppCompatActivity {
    public static final String TAG = QuestionResultActivity.class.getName();
    public Context context;

    DatabaseHandler databaseHandler;
    TableCategory tableCategory;
    TableQuestionResult tableQuestionResult;

    Category selectedCategory;
    QuestionResult selectedQuestionResult;

    TextView txtCategoryTitle;
    TextView txtQuestionCount;
    TextView txtQuestionCorrect;
    TextView txtQuestionWrong;
    TextView txtQuestionNoAnswer;
    TextView txtProgressStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_result);
        context = getApplicationContext();

        databaseHandler = new DatabaseHandler(getApplicationContext());
        tableQuestionResult = new TableQuestionResult(databaseHandler);
        tableCategory = new TableCategory(databaseHandler);

        txtCategoryTitle = findViewById(R.id.txt_category_title);
        txtQuestionCount = findViewById(R.id.txt_question_count);
        txtQuestionCorrect = findViewById(R.id.txt_question_correct);
        txtQuestionWrong = findViewById(R.id.txt_question_wrong);
        txtQuestionNoAnswer = findViewById(R.id.txt_question_no_answer);
        txtProgressStatus = findViewById(R.id.txt_progress_status);

        Intent intent = getIntent();
        int exam_result_id = intent.getIntExtra("exam_result_id", 1);

        if(exam_result_id != -1) {
            selectedQuestionResult = tableQuestionResult.get(exam_result_id);
            selectedCategory = tableCategory.get(selectedQuestionResult.getCategoryId());

            txtCategoryTitle.setText(selectedCategory.getTitle().replace("-", " ").toUpperCase());
            txtQuestionCount.setText(selectedCategory.getNumberOfQuestion() + " Questions");
            txtQuestionCorrect.setText(selectedQuestionResult.getCorrectAnswer() + " Correct Answers");
            txtQuestionWrong.setText(selectedQuestionResult.getWrongAnswer() + " Wrong Answers");
            txtQuestionNoAnswer.setText(selectedQuestionResult.getNoAnswer() + " No Answers");

            int successPercent = (selectedQuestionResult.getCorrectAnswer() * 100) / selectedCategory.getNumberOfQuestion();
            txtProgressStatus.setText("Success Rate: " + String.valueOf(successPercent) + "%");

        }

    }
}
