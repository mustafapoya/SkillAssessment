package net.golbarg.skillassessment.ui.profile;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableConfig;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.db.TableQuestionAnswer;
import net.golbarg.skillassessment.db.TableQuestionResult;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.ui.home.CategoryListAdapter;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class QuestionResultListAdapter extends ArrayAdapter<QuestionResult> {
    public static final String TAG = CategoryListAdapter.class.getName();

    private final Activity context;
    private final ArrayList<QuestionResult> questionResults;
    DatabaseHandler dbHandler;
    TableCategory tableCategory;
    TableQuestionResult tableQuestionResult;

    public QuestionResultListAdapter(Activity context, ArrayList<QuestionResult> questionResults) {
        super(context, R.layout.custom_list_question_result, questionResults);
        this.context = context;
        this.questionResults = questionResults;
        this.dbHandler = new DatabaseHandler(context);
        this.tableCategory = new TableCategory(dbHandler);
        this.tableQuestionResult = new TableQuestionResult(dbHandler);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.custom_list_question_result, null, true);

        QuestionResult selected = questionResults.get(position);

        TextView txtCategoryTitle = rowView.findViewById(R.id.txt_category_title);
        txtCategoryTitle.setText(tableCategory.get(selected.getCategoryId()).getTitle());

        TextView txtCorrectAnswer = rowView.findViewById(R.id.txt_correct);
        TextView txtWrongAnswer   = rowView.findViewById(R.id.txt_wrong);
        TextView txtNoAnswer      = rowView.findViewById(R.id.txt_no_answer);
        TextView txtSuccessRate   = rowView.findViewById(R.id.txt_success_rate);

        txtCorrectAnswer.setText("Correct: " + selected.getCorrectAnswer());
        txtWrongAnswer.setText("Wrong: " + selected.getWrongAnswer());
        txtNoAnswer.setText("No response: " + selected.getNoAnswer());

        int successRate = (selected.getCorrectAnswer() * 100) / (selected.getCorrectAnswer() + selected.getWrongAnswer() + selected.getNoAnswer());

        txtSuccessRate.setText(successRate + "%");

        return rowView;
    }
}
