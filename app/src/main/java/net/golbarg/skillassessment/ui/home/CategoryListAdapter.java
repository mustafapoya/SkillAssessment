package net.golbarg.skillassessment.ui.home;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.models.Category;

import java.util.ArrayList;

public class CategoryListAdapter extends ArrayAdapter<Category> {
    public static final String TAG = CategoryListAdapter.class.getName();

    private final Activity context;
    private final ArrayList<Category> categories;
    DatabaseHandler handler;
    TableQuestion tableQuestion;
    FragmentManager fragmentManager;

    public CategoryListAdapter(Activity context, ArrayList<Category> categories) {
        super(context, R.layout.custom_list_category, categories);
        this.context = context;
        this.categories = categories;
        this.handler = new DatabaseHandler(context);
        this.tableQuestion = new TableQuestion(handler);
    }

    public CategoryListAdapter(Activity context, ArrayList<Category> categories, FragmentManager fragmentManager) {
        super(context, R.layout.custom_list_category, categories);
        this.context = context;
        this.categories = categories;
        this.handler = new DatabaseHandler(context);
        this.tableQuestion = new TableQuestion(handler);
        this.fragmentManager = fragmentManager;

    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.custom_list_category, null,true);

        TextView txtNumber = rowView.findViewById(R.id.txt_number);
        txtNumber.setText(String.valueOf(categories.get(position).getId()));

        TextView txtTitle = rowView.findViewById(R.id.txt_title);
        txtTitle.setText(categories.get(position).getTitle());

        TextView txtContentCount = rowView.findViewById(R.id.txt_content_count);
        txtContentCount.setText(tableQuestion.getQuestionsOf(categories.get(position).getId()).size() + " questions");

        return rowView;

    }


}
