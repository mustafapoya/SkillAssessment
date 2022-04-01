package net.golbarg.skillassessment.ui.home;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.ui.dialog.CreditDialog;
import net.golbarg.skillassessment.util.JsonUtil;
import net.golbarg.skillassessment.util.UtilController;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    public static final String TAG = HomeFragment.class.getName();

    Context context;
    AdView mAdViewHomeScreenBanner;
    ProgressBar progressLoading;
    private ListView listViewCategory;
    CategoryListAdapter categoryListAdapter;
    ArrayList<Category> categoryArrayList = new ArrayList<>();

    ConstraintLayout layoutNoInternet;
    ConstraintLayout layoutMainLayout;
    Button btnTryAgainConnection;
    DatabaseHandler dbHandler;
    TableCategory tableCategory;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        setHasOptionsMenu(true);
        context = root.getContext();
        dbHandler = new DatabaseHandler(context);
        tableCategory = new TableCategory(dbHandler);

        mAdViewHomeScreenBanner = root.findViewById(R.id.adViewHomeScreenBanner);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdViewHomeScreenBanner.loadAd(adRequest);

        progressLoading = root.findViewById(R.id.progress_loading);
        progressLoading.setVisibility(View.GONE);
        listViewCategory = root.findViewById(R.id.list_view_category);
        layoutMainLayout = root.findViewById(R.id.layout_main_layout);
        layoutNoInternet = root.findViewById(R.id.layout_no_internet);
        btnTryAgainConnection = root.findViewById(R.id.btn_try_again);

        categoryArrayList = tableCategory.getAll();
        categoryListAdapter = new CategoryListAdapter(getActivity(), categoryArrayList, getParentFragmentManager(), listViewCategory);
        listViewCategory.setAdapter(categoryListAdapter);

        btnTryAgainConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDataLoading();
            }
        });

        if(!UtilController.isNetworkConnected(context) && categoryArrayList.size() > 0) {
            categoryListAdapter.notifyDataSetChanged();
            Log.d(TAG, "read data from database" + categoryArrayList.size());
        } else {
            Log.d(TAG, "connect to online server" + categoryArrayList.size());
            new FetchCategoryDataTask().execute();
        }

        return root;
    }

    private void handleDataLoading() {
        if(UtilController.isNetworkConnected(context)) {
            layoutMainLayout.setVisibility(View.VISIBLE);
            layoutNoInternet.setVisibility(View.GONE);
            new FetchCategoryDataTask().execute();
        } else {
            layoutMainLayout.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.refresh_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.refresh:
                handleDataLoading();
                return true;
            case R.id.credit:
                CreditDialog creditDialog = new CreditDialog();
                creditDialog.show(getChildFragmentManager(), CreditDialog.TAG);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

            if(!successful && categoryArrayList.size() > 0) {
                layoutMainLayout.setVisibility(View.GONE);
                layoutNoInternet.setVisibility(View.VISIBLE);
            } else {
                categoryArrayList.clear();
                categoryArrayList.addAll(result);
                categoryListAdapter.notifyDataSetChanged();
            }
        }
    }

}