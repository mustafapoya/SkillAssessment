package net.golbarg.skillassessment.ui.bookmark;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableBookmark;
import net.golbarg.skillassessment.models.Bookmark;

import java.util.ArrayList;

public class BookmarkFragment extends Fragment {

    Context context;
    ProgressBar progressLoading;
    BookmarkQuestionListAdapter bookmarkQuestionListAdapter;
    ArrayList<Bookmark> bookmarksArrayList = new ArrayList<>();
    DatabaseHandler dbHandler;
    TableBookmark tableBookmark;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_bookmark, container, false);
        context = root.getContext();
        dbHandler = new DatabaseHandler(context);
        tableBookmark = new TableBookmark(dbHandler);

        AdView mAdViewBookmarkScreenBanner = root.findViewById(R.id.adViewBookmarkScreenBanner);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdViewBookmarkScreenBanner.loadAd(adRequest);

        progressLoading = root.findViewById(R.id.progress_loading);
        ListView listViewContents = root.findViewById(R.id.list_view_questions);

        bookmarkQuestionListAdapter = new BookmarkQuestionListAdapter(getActivity(), bookmarksArrayList);
        listViewContents.setAdapter(bookmarkQuestionListAdapter);

        new BookmarkFragment.FetchBookmarkContentDataTask().execute();

        return root;
    }

    private class FetchBookmarkContentDataTask extends AsyncTask<String, String, ArrayList<Bookmark>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<Bookmark> doInBackground(String... params) {
            ArrayList<Bookmark> result = new ArrayList<>();

            try {
                result = tableBookmark.getAll();
                return result;
            }catch(Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<Bookmark> bookmarks) {
            super.onPostExecute(bookmarks);
            progressLoading.setVisibility(View.GONE);
            bookmarksArrayList.clear();
            bookmarksArrayList.addAll(bookmarks);
            bookmarkQuestionListAdapter.notifyDataSetChanged();

        }
    }
}