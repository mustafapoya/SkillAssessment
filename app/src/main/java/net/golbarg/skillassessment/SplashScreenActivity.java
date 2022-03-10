package net.golbarg.skillassessment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.golbarg.skillassessment.controller.JsonController;
import net.golbarg.skillassessment.db.TableCategory;
import net.golbarg.skillassessment.db.TableQuestion;
import net.golbarg.skillassessment.util.UtilController;

public class SplashScreenActivity extends AppCompatActivity {
    public static final String TAG = SplashScreenActivity.class.getName();
    SharedPreferences pref = UtilController.getSharedPref(getApplicationContext(), "db_content");

    boolean isActive = true;

    ProgressBar progressLoading;
    TextView txtStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getSupportActionBar().hide();

        Thread splashThread = new Thread(){
            @Override
            public void run() {
                try {
                    String dbStatus = pref.getString(UtilController.KEY_DB_STATUS, null);

                    if("success".equals(dbStatus)) {
                        showWaiting();
                    } else {
                        addDataToDatabase();
                    }

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

    private void addDataToDatabase() {
        String categoryAdded = pref.getString(TableCategory.TABLE_NAME, null);
        if(!"added".equals(categoryAdded)) {
            JsonController.insertCategoriesToDB(getApplicationContext());
            UtilController.insertSharedPref(pref, TableCategory.TABLE_NAME, "added");
        }

        String questionAdded = pref.getString(TableQuestion.TABLE_NAME, null);
        if(!"added".equals(questionAdded)) {
            JsonController.insertQuestionsToDB(getApplicationContext());
            UtilController.insertSharedPref(pref, TableQuestion.TABLE_NAME, "added");
        }

        // After Successful All Data Insertion
        UtilController.insertSharedPref(pref, UtilController.KEY_DB_STATUS, "success");

    }
}
