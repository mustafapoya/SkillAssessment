package net.golbarg.skillassessment.ui.profile;

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
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableQuestionResult;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.PieChartData;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.ui.home.CategoryListAdapter;
import net.golbarg.skillassessment.util.JsonUtil;
import net.golbarg.skillassessment.util.UtilController;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    Context context;
    AdView mAdViewScreenBanner;
    ProgressBar progressLoading;
    private ListView listViewQuestionResult;
    QuestionResultListAdapter questionResultListAdapter;
    ArrayList<QuestionResult> questionResultArrayList = new ArrayList<>();

    DatabaseHandler dbHandler;
    TableQuestionResult tableQuestionResult;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        context = root.getContext();

        context = root.getContext();
        dbHandler = new DatabaseHandler(context);
        tableQuestionResult = new TableQuestionResult(dbHandler);

        mAdViewScreenBanner = root.findViewById(R.id.adViewScreenBanner);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdViewScreenBanner.loadAd(adRequest);

        progressLoading = root.findViewById(R.id.progress_loading);
        progressLoading.setVisibility(View.GONE);

        TextView txtTotalTest = root.findViewById(R.id.txt_total_test);
        txtTotalTest.setText(tableQuestionResult.getCount() + " Tests tried!");

        PieChart pieChartResult = root.findViewById(R.id.pie_chart_result);

        QuestionResult totalResult = tableQuestionResult.getTotalResult();

        ArrayList<PieChartData> pieChartData = new ArrayList<>();
        pieChartData.add(new PieChartData("Correct", totalResult.getCorrectAnswer(), context.getResources().getColor(R.color.green_500)));
        pieChartData.add(new PieChartData("Wrong", totalResult.getWrongAnswer(), context.getResources().getColor(R.color.red_500)));
        pieChartData.add(new PieChartData("No Response", totalResult.getNoAnswer(), context.getResources().getColor(R.color.gray)));

        List<PieEntry> entries = new ArrayList<>();
        for (PieChartData data: pieChartData) {
            PieEntry pie = new PieEntry(data.getValue(), data.getLabel());
            entries.add(pie);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setValueTextSize(12);

        if(UtilController.isNightMode(context)) {
            dataSet.setColor(context.getResources().getColor(R.color.white));
        } else {
            dataSet.setColor(context.getResources().getColor(R.color.black));
        }

        dataSet.resetColors();
        for (PieChartData data: pieChartData) {
            dataSet.addColor(data.getColor());
        }
        PieData pieData = new PieData(dataSet);
        pieData.setValueTextColor(context.getResources().getColor(R.color.white));

        pieChartResult.setData(pieData);
        Description d = new Description();
        d.setText("");
        pieChartResult.setDescription(d);
        pieChartResult.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        pieChartResult.setCenterTextColor(context.getResources().getColor(R.color.blue_700));
        pieChartResult.setCenterText("Result");
        pieChartResult.setCenterTextSize(18);
        pieChartResult.getLegend().setTextColor(context.getResources().getColor(UtilController.isNightMode(context) ? R.color.white : R.color.black));
        pieChartResult.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieChartResult.setEntryLabelColor(context.getResources().getColor(UtilController.isNightMode(context) ? R.color.white : R.color.black));

        pieChartResult.invalidate();


        listViewQuestionResult = root.findViewById(R.id.list_view_question_result);

        questionResultListAdapter = new QuestionResultListAdapter(getActivity(), questionResultArrayList);
        listViewQuestionResult.setAdapter(questionResultListAdapter);

        new FetchQuestionResultDataTask().execute();


        return root;
    }

    private class FetchQuestionResultDataTask extends AsyncTask<String, String, ArrayList<QuestionResult>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<QuestionResult> doInBackground(String... params) {
            ArrayList<QuestionResult> result = new ArrayList<>();

            try {
                result = tableQuestionResult.getAll();
                return result;
            }catch(Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<QuestionResult> result) {
            super.onPostExecute(result);
            progressLoading.setVisibility(View.GONE);
            questionResultArrayList.clear();
            questionResultArrayList.addAll(result);
            questionResultListAdapter.notifyDataSetChanged();
        }
    }
}