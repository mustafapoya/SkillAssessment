package net.golbarg.skillassessment.ui.home.detail;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.nevidelia.library.highlight.Highlight;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;

import java.util.ArrayList;

public class CategoryDetailFragment extends Fragment {
    public static final String TAG = CategoryDetailFragment.class.getName();

    Context context;
    ProgressBar progressLoading;
    private ListView listViewQuestion;
    QuestionListAdapter questionListAdapter;
    ArrayList<Question> questionArrayList = new ArrayList<>();
    DatabaseHandler dbHandler;
    private int categoryId;

    public CategoryDetailFragment() {

    }

    public CategoryDetailFragment(int categoryId) {
        this.categoryId = categoryId;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_category_detail, container, false);
        context = root.getContext();
        dbHandler = new DatabaseHandler(context);

        progressLoading = root.findViewById(R.id.progress_loading);
        listViewQuestion = root.findViewById(R.id.list_view_questions);

        TableCategory categoryTable = new TableCategory(dbHandler);
        Category selectedCategory = categoryTable.get(categoryId);

        TextView txtCategoryTitle = root.findViewById(R.id.txt_category_title);
        txtCategoryTitle.setText(selectedCategory.getTitle().replace("-", " "));


        questionListAdapter = new QuestionListAdapter(getActivity(), questionArrayList, getParentFragmentManager());
        listViewQuestion.setAdapter(questionListAdapter);

        try {
            new FetchQuestionDataTask().execute(String.valueOf(categoryId));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }

    private class FetchQuestionDataTask extends AsyncTask<String, String, ArrayList<Question>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<Question> doInBackground(String... params) {
            ArrayList<Question> result = new ArrayList<>();

            try {
                int categoryId = Integer.parseInt(params[0]);
                TableQuestion tableContent = new TableQuestion(dbHandler);
                result = tableContent.getQuestionsOf(categoryId);

                return result;
            }catch(Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<Question> result) {
            super.onPostExecute(result);
            progressLoading.setVisibility(View.GONE);
            questionArrayList.clear();
            questionArrayList.addAll(result);
            questionListAdapter.notifyDataSetChanged();
        }
    }
}
