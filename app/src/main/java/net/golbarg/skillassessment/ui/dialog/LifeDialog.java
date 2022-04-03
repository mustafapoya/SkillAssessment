package net.golbarg.skillassessment.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableConfig;
import net.golbarg.skillassessment.models.Config;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.util.CryptUtil;
import net.golbarg.skillassessment.util.UtilController;

import org.jetbrains.annotations.NotNull;

public class LifeDialog extends DialogFragment {
    public static String TAG = CreditDialog.class.getName();
    Context context;
    private RewardedAd mRewardedAd;
    DatabaseHandler databaseHandler;
    TableConfig tableConfig;

    TextView txtLifeCount;
    Button btnViewAd;
    Button btnEndTest;

    QuestionActivity questionActivity;
    public LifeDialog(QuestionActivity questionActivity) {
        this.questionActivity = questionActivity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_life, container, false);
        context = root.getContext();
        databaseHandler = new DatabaseHandler(context);
        tableConfig = new TableConfig(databaseHandler);

        txtLifeCount = root.findViewById(R.id.txt_life_count);
        txtLifeCount.setText(context.getResources().getString(R.string.number_of_life) + questionActivity.getCurrentLife());
        btnViewAd = root.findViewById(R.id.btn_view_ad);

        btnEndTest = root.findViewById(R.id.btn_end_test);
        btnEndTest.setOnClickListener(v -> {
            questionActivity.gotoFinishActivity();
        });

        if(this.questionActivity.getCurrentLife() <=0) {
            this.setCancelable(false);
        }

        btnViewAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdRequest adRequest = new AdRequest.Builder().build();
                // TODO: on publish add real ad unit
                // real ad Unit: ca-app-pub-1361000594268534/8491545563
                // test ad Unit: ca-app-pub-3940256099942544/5224354917
                RewardedAd.load(context, "ca-app-pub-3940256099942544/5224354917", adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull @NotNull RewardedAd rewardedAd) {
                        super.onAdLoaded(rewardedAd);
                        mRewardedAd = rewardedAd;
                        Log.d(TAG, "Ad was loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull @NotNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.d(TAG, loadAdError.getMessage());
                        mRewardedAd = null;
                    }
                });

                if(mRewardedAd != null) {
                    mRewardedAd.show(getActivity(), new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(@NonNull @NotNull RewardItem rewardItem) {
                            // Handle the reward.
                            Log.d(TAG, "The user earned the reward.");
                            try {
                                questionActivity.setNumberOfLife(questionActivity.getCurrentLife() + 1);
                                txtLifeCount.setText(context.getResources().getString(R.string.number_of_life) + questionActivity.getCurrentLife());
                                setCancelable(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    Log.d(TAG, "The rewarded ad wasn't ready yet.");
                }
            }
        });

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(this.questionActivity.getCountDownTimer() != null) {
            if(this.questionActivity.getCountDownTimer().isPaused()) {
                this.questionActivity.getCountDownTimer().resume();
            }
        }
        Log.d("QuestionActivity", "the dialog closed");
    }
}
