package net.golbarg.skillassessment.ui.home;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.util.JsonUtil;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    public static final String TAG = HomeFragment.class.getName();

    Context context;
    ProgressBar progressLoading;
    private ListView listViewCategory;
    CategoryListAdapter categoryListAdapter;
    ArrayList<Category> categoryArrayList = new ArrayList<>();
    private ConstraintLayout layoutMainLayout;
    DatabaseHandler dbHandler;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        context = root.getContext();
        dbHandler = new DatabaseHandler(context);

        progressLoading = root.findViewById(R.id.progress_loading);
        listViewCategory = root.findViewById(R.id.list_view_category);
        layoutMainLayout = root.findViewById(R.id.layout_main_layout);

        categoryListAdapter = new CategoryListAdapter(getActivity(), categoryArrayList, getParentFragmentManager());
        listViewCategory.setAdapter(categoryListAdapter);

        try {
            new FetchCategoryDataTask().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }

    private class FetchCategoryDataTask extends AsyncTask<String, String, ArrayList<Category>> {
        boolean successful = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<Category> doInBackground(String... params) {
            ArrayList<Category> result = new ArrayList<>();

            try {
                result = JsonUtil.mapCategoriesFromJson(JsonUtil.getJsonCategories(context));
                successful = true;
                return result;
            }catch(Exception e) {
                successful = false;
                e.printStackTrace();
            }
            successful = false;
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<Category> result) {
            super.onPostExecute(result);
            progressLoading.setVisibility(View.GONE);

            categoryArrayList.clear();
            categoryArrayList.addAll(result);
            categoryListAdapter.notifyDataSetChanged();
        }
    }

}