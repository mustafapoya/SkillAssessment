package net.golbarg.skillassessment.CustomView;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import net.golbarg.skillassessment.R;

public class AnswerView extends ConstraintLayout {
    private OnClickListener listener;
    private TextView txtAnswerText;
    private TextView txtAnswerCode;

    public AnswerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        inflate(context, R.layout.view_answer, this);

        txtAnswerText = findViewById(R.id.txt_answer_text);
        txtAnswerCode = findViewById(R.id.txt_answer_code);

        TypedArray attributes = context.obtainStyledAttributes(attributeSet, R.styleable.AnswerView);
        txtAnswerText.setText(attributes.getString(R.styleable.AnswerView_answer_text));
        txtAnswerCode.setText(attributes.getString(R.styleable.AnswerView_answer_code));

        attributes.recycle();
    }

    public TextView getTxtAnswerText() {
        return txtAnswerText;
    }

    public TextView getTxtAnswerCode() {
        return txtAnswerCode;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_UP) {
            if(listener != null) {
                listener.onClick(this);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_UP && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            if(listener != null) {
                listener.onClick(this);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void setListener(OnClickListener listener) {
        this.listener = listener;
    }
}
