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

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableBookmark;
import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;

public class BookmarkQuestionListAdapter extends ArrayAdapter<Bookmark> {
    private final Activity context;
    private final ArrayList<Bookmark> bookmarks;
    DatabaseHandler dbHandler;
    TableBookmark tableBookmark;

    public BookmarkQuestionListAdapter(Activity context, ArrayList<Bookmark> bookmarks) {
        super(context, R.layout.custom_list_bookmark_question, bookmarks);
        this.context = context;
        this.bookmarks = bookmarks;
        dbHandler = new DatabaseHandler(context);
        tableBookmark = new TableBookmark(dbHandler);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.custom_list_bookmark_question, null,true);

        TextView txtMessage = rowView.findViewById(R.id.txt_message);
        txtMessage.setText(String.valueOf(bookmarks.get(position).getQuestionId()));

        Button btnDelete = rowView.findViewById(R.id.btn_delete);
        Button btnShare  = rowView.findViewById(R.id.btn_share);

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, bookmarks.get(position).getQuestionId() + "\n\n" + UtilController.appLink());
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_using)));
            }
        });

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