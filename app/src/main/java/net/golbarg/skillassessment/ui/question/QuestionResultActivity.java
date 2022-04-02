package net.golbarg.skillassessment.ui.question;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableQuestionResult;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.PieChartData;
import net.golbarg.skillassessment.models.QuestionResult;

import java.util.ArrayList;
import java.util.List;

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
    PieChart pieChartProgress;

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
        pieChartProgress = findViewById(R.id.pie_chart_progress);

        Intent intent = getIntent();
        long question_result_id = intent.getLongExtra("question_result_id", 1);
        Log.d(TAG, "question_result_id: " + question_result_id);

        if(question_result_id != -1) {
            selectedQuestionResult = tableQuestionResult.get((int)question_result_id);
            selectedCategory = tableCategory.get(selectedQuestionResult.getCategoryId());

            txtCategoryTitle.setText(selectedCategory.getTitle().replace("-", " ").toUpperCase());
            txtQuestionCount.setText(selectedCategory.getNumberOfQuestion() + " Questions");
            txtQuestionCorrect.setText(selectedQuestionResult.getCorrectAnswer() + " Correct Answers");
            txtQuestionWrong.setText(selectedQuestionResult.getWrongAnswer() + " Wrong Answers");
            txtQuestionNoAnswer.setText(selectedQuestionResult.getNoAnswer() + " No Answers");

            int successPercent = (selectedQuestionResult.getCorrectAnswer() * 100) / selectedCategory.getNumberOfQuestion();
            txtProgressStatus.setText("Success Rate: " + String.valueOf(successPercent) + "%");

            ArrayList<PieChartData> pieChartData = new ArrayList<>();
            pieChartData.add(new PieChartData("Correct", selectedQuestionResult.getCorrectAnswer(), context.getResources().getColor(R.color.green_500)));
            pieChartData.add(new PieChartData("Wrong", selectedQuestionResult.getWrongAnswer(), context.getResources().getColor(R.color.red_500)));
            pieChartData.add(new PieChartData("No Response", selectedQuestionResult.getNoAnswer(), context.getResources().getColor(R.color.gray)));


            List<PieEntry> entries = new ArrayList<>();
            for (PieChartData data: pieChartData) {
                PieEntry pie = new PieEntry(data.getValue(), data.getLabel());
                entries.add(pie);
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setValueTextSize(12);
            dataSet.setValueTextColor(context.getResources().getColor(R.color.white));
            dataSet.setColor(context.getResources().getColor(R.color.black));

            dataSet.resetColors();
            for (PieChartData data: pieChartData) {
                dataSet.addColor(data.getColor());
            }
            PieData pieData = new PieData(dataSet);
            pieChartProgress.setData(pieData);
            Description d = new Description();
            d.setText("");
            pieChartProgress.setDescription(d);
            pieChartProgress.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            pieChartProgress.setCenterTextColor(context.getResources().getColor(R.color.blue_700));
            pieChartProgress.setCenterText("Result");
            pieChartProgress.setCenterTextSize(18);
            pieChartProgress.invalidate();

        }

    }
}
