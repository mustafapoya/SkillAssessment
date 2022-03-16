package net.golbarg.skillassessment.ui.home.detail;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.nevidelia.library.highlight.Highlight;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.models.Question;

import java.util.ArrayList;

public class QuestionListAdapter extends ArrayAdapter<Question> {
    public static final String TAG = QuestionListAdapter.class.getName();

    private final Activity context;
    private final ArrayList<Question> questions;

    DatabaseHandler handler;
    FragmentManager fragmentManager;

    public QuestionListAdapter(Activity context, ArrayList<Question> questions) {
        super(context, R.layout.custom_list_question, questions);
        this.context = context;
        this.questions = questions;
        this.handler = new DatabaseHandler(context);
    }

    public QuestionListAdapter(Activity context, ArrayList<Question> questions, FragmentManager fragmentManager) {
        super(context, R.layout.custom_list_question, questions);
        this.context = context;
        this.questions = questions;
        this.handler = new DatabaseHandler(context);
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.custom_list_question, null,true);

        TextView txtNumber = rowView.findViewById(R.id.txt_number);
        txtNumber.setText(String.valueOf(questions.get(position).getId()));

        TextView txtTitle = rowView.findViewById(R.id.txt_title);
        txtTitle.setText(questions.get(position).getTitle());

        Highlight highlight = new Highlight();
        txtTitle.setText(highlight.java(txtTitle.getText().toString()));
//        txtCategoryTitle.setText(highlight.code(Highlight.C,"#include<stdio.h>\nvoid main()\n{\n\b\b\b\bprintf(\"Hello\bworld\");\n}"));

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Question Item Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        return rowView;
    }
}