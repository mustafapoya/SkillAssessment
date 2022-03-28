package net.golbarg.skillassessment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.golbarg.skillassessment.controller.JsonController;
import net.golbarg.skillassessment.db.DatabaseHandler;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableConfig;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.db.TableQuestionAnswer;
import net.golbarg.skillassessment.models.Config;
import net.golbarg.skillassessment.util.CryptUtil;
import net.golbarg.skillassessment.util.UtilController;

public class SplashScreenActivity extends AppCompatActivity {
    public static final String TAG = SplashScreenActivity.class.getName();
    SharedPreferences pref;

    boolean isActive = true;

    ProgressBar progressLoading;
    TextView txtStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = UtilController.getSharedPref(getApplicationContext(), "db_content");
        setContentView(R.layout.activity_splash);
        getSupportActionBar().hide();

        progressLoading = findViewById(R.id.progress_loading);
        txtStatus = findViewById(R.id.txt_status);
        txtStatus.setText("");

        Thread splashThread = new Thread(){
            @Override
            public void run() {
                try {
//                    String dbStatus = pref.getString(UtilController.KEY_DB_STATUS, null);
//
//                    if("success".equals(dbStatus)) {
//                        showWaiting();
//                    } else {
//                        addDataToDatabase();
//                    }
                    checkUserCredit(getApplicationContext());
                    showWaiting();

                    finish();
                    startActivity(new Intent(getBaseContext(), MainActivity.class));
                } catch (Exception e) {
                    finish();
                    e.printStackTrace();
                }
            }

            private void showWaiting() {
                int waitTime = 0;

                while(waitTime <= 2) {
                    try{
                        sleep(1000);
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    waitTime += 1;
                }
            }
        };
        splashThread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            isActive = false;
        }
        return true;
    }

    private void checkUserCredit(Context context) {
        TableConfig tableConfig = new TableConfig(new DatabaseHandler(context));

        if(tableConfig.getByKey(UtilController.KEY_CREDIT) == null) {
            try {
                Config config = new Config(UtilController.KEY_CREDIT, CryptUtil.encrypt(String.valueOf(UtilController.DEFAULT_CREDIT)));
                tableConfig.create(config);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addDataToDatabase() {
        String categoryAdded = pref.getString(TableCategory.TABLE_NAME, null);
        if(!"added".equals(categoryAdded)) {
            txtStatus.setText("reading Categories");
            JsonController.insertCategoriesToDB(getApplicationContext());
            txtStatus.setText("loading Categories");
            UtilController.insertSharedPref(pref, TableCategory.TABLE_NAME, "added");
        }

        String questionAdded = pref.getString(TableQuestion.TABLE_NAME, null);
        if(!"added".equals(questionAdded)) {
            txtStatus.setText("reading questions");
            JsonController.insertQuestionsToDB(getApplicationContext());
            txtStatus.setText("loading questions");
            UtilController.insertSharedPref(pref, TableQuestion.TABLE_NAME, "added");
        }

        String answerAdded = pref.getString(TableQuestionAnswer.TABLE_NAME, null);
        if(!"added".equals(answerAdded)) {
            txtStatus.setText("reading Answers");
            JsonController.insertAnswersToDB(getApplicationContext());
            txtStatus.setText("loading Answers");
            UtilController.insertSharedPref(pref, TableQuestion.TABLE_NAME, "added");
        }

        // After Successful All Data Insertion
        txtStatus.setText("Done.");
        UtilController.insertSharedPref(pref, UtilController.KEY_DB_STATUS, "success");

    }
}
