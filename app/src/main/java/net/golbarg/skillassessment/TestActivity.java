package net.golbarg.skillassessment;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nevidelia.library.highlight.Highlight;

public class TestActivity extends AppCompatActivity {
    public static final String TAG = SplashScreenActivity.class.getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        TextView txtCode = findViewById(R.id.txtCode);

        Highlight highlight = new Highlight();
        txtCode.setText(highlight.c("#include<stdio.h>\nvoid main()\n{\n\b\b\b\bprintf(\"Hello\bworld\");\n}"));
        txtCode.setText(highlight.code(Highlight.C,"#include<stdio.h>\nvoid main()\n{\n\b\b\b\bprintf(\"Hello\bworld\");\n}"));

    }
}
