package net.golbarg.skillassessment.CustomView;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import net.golbarg.skillassessment.R;

public class QuestionView extends ConstraintLayout {
    private TextView txtQuestionText;
    private TextView txtQuestionCode;
    private ImageView imgQuestionImage;
    
    public QuestionView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        inflate(context, R.layout.view_question, this);

        txtQuestionText = findViewById(R.id.txt_question_text);
        txtQuestionCode = findViewById(R.id.txt_question_code);
        imgQuestionImage = findViewById(R.id.img_question_image);

        TypedArray attributes = context.obtainStyledAttributes(attributeSet, R.styleable.QuestionView);
        txtQuestionText.setText(attributes.getString(R.styleable.QuestionView_question_text));
        txtQuestionCode.setText(attributes.getString(R.styleable.QuestionView_question_code));
        imgQuestionImage.setImageDrawable(attributes.getDrawable(R.styleable.QuestionView_question_image));

        attributes.recycle();

    }

    public TextView getTxtQuestionText() {
        return txtQuestionText;
    }

    public TextView getTxtQuestionCode() {
        return txtQuestionCode;
    }

    public ImageView getImgQuestionImage() {
        return imgQuestionImage;
    }
}
