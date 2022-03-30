package net.golbarg.skillassessment.ui.bookmark;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.golbarg.skillassessment.CustomView.AnswerView;
import net.golbarg.skillassessment.CustomView.QuestionView;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableBookmark;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;

public class BookmarkQuestionListAdapter extends ArrayAdapter<Bookmark> {
    private final Activity context;
    private final ArrayList<Bookmark> bookmarks;
    DatabaseHandler dbHandler;
    TableBookmark tableBookmark;
    TableQuestion tableQuestion;
    TableCategory tableCategory;

    public BookmarkQuestionListAdapter(Activity context, ArrayList<Bookmark> bookmarks) {
        super(context, R.layout.custom_list_bookmark_question, bookmarks);
        this.context = context;
        this.bookmarks = bookmarks;
        dbHandler = new DatabaseHandler(context);
        tableBookmark = new TableBookmark(dbHandler);
        tableQuestion = new TableQuestion(dbHandler);
        tableCategory = new TableCategory(dbHandler);

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.custom_list_bookmark_question, null,true);

        Question currentQuestion = tableQuestion.getWithCorrectAnswer(bookmarks.get(position).getQuestionId());

        QuestionView questionView = rowView.findViewById(R.id.question_view);
        UtilController.highlightQuestionText(questionView, currentQuestion.getTitle(), tableCategory.get(currentQuestion.getCategoryId()), context);

        AnswerView answerView = rowView.findViewById(R.id.question_answer_view);
        if(currentQuestion.getAnswers().size() > 0) {
            answerView.setVisibility(View.VISIBLE);
            UtilController.highlightAnswerText(answerView, currentQuestion.getAnswers().get(0).getTitle(), tableCategory.get(currentQuestion.getCategoryId()), context);
        } else {
            answerView.setVisibility(View.GONE);
        }

        Button btnDelete = rowView.findViewById(R.id.btn_delete);

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    tableBookmark.delete((bookmarks.get(position)));
                    bookmarks.remove(position);
                    notifyDataSetChanged();
                    UtilController.showSnackMessage(rowView, context.getString(R.string.bookmark_deleted));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return rowView;
    }
}
