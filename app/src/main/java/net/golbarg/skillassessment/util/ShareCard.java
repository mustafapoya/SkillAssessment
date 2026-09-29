package net.golbarg.skillassessment.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ViewCertificateBinding;
import net.golbarg.skillassessment.databinding.ViewShareCardBinding;
import net.golbarg.skillassessment.models.InterviewPack;
import net.golbarg.skillassessment.models.QuestionResult;

import java.io.File;
import java.io.FileOutputStream;

/** Renders branded cards (a result, or an exam certificate) to an image and opens the share sheet. */
public final class ShareCard {
    private static final String TAG = "ShareCard";

    /** Exams need at least this many questions and this score to earn a certificate. */
    public static final int CERTIFICATE_MIN_QUESTIONS = 10;
    public static final int CERTIFICATE_MIN_PERCENT = 80;

    private ShareCard() {
    }

    /** Topic exams and interview exams count; mixed and other practice modes don't. */
    public static boolean earnsCertificate(QuestionResult result) {
        boolean certifiable = result.getCategoryId() >= 0 || InterviewPack.fromId(result.getCategoryId()) != null;
        return result.isExam() && certifiable && result.getTotal() >= CERTIFICATE_MIN_QUESTIONS
                && result.getScorePercent() >= CERTIFICATE_MIN_PERCENT;
    }

    public static void share(Activity activity, QuestionResult result, String topic, int bestStreak) {
        ViewShareCardBinding card = ViewShareCardBinding.inflate(LayoutInflater.from(activity));
        card.txtTopic.setText(topic);
        card.txtScore.setText(activity.getString(R.string.percent_value, result.getScorePercent()));
        card.txtDetail.setText(activity.getString(R.string.share_card_score_label, result.getCorrectAnswer(), result.getTotal()));
        card.txtStreak.setVisibility(bestStreak >= 2 ? View.VISIBLE : View.GONE);
        card.txtStreak.setText(activity.getString(R.string.best_streak, bestStreak));
        shareView(activity, card.getRoot(), 360, 450, "result.png",
                activity.getString(R.string.share_result_message, result.getScorePercent(), topic, AppLinks.PLAY_URL),
                activity.getString(R.string.share_result));
    }

    public static void shareCertificate(Activity activity, QuestionResult result, String topic, String name) {
        ViewCertificateBinding card = ViewCertificateBinding.inflate(LayoutInflater.from(activity));
        card.txtName.setText(name);
        card.txtBody.setText(activity.getString(R.string.certificate_body, topic));
        card.txtScore.setText(activity.getString(R.string.certificate_score, result.getScorePercent(), result.getCorrectAnswer(), result.getTotal()));
        long when = result.getCreatedAt() > 0 ? result.getCreatedAt() : System.currentTimeMillis();
        card.txtDate.setText(activity.getString(R.string.certificate_date, UiUtils.formatDate(when)));
        shareView(activity, card.getRoot(), 520, 368, "certificate.png",
                activity.getString(R.string.certificate_share_message, topic, result.getScorePercent(), AppLinks.PLAY_URL),
                activity.getString(R.string.get_certificate));
    }

    private static void shareView(Activity activity, View view, int widthDp, int heightDp, String fileName, String text, String chooserTitle) {
        try {
            int width = UiUtils.dp(activity, widthDp);
            int height = UiUtils.dp(activity, heightDp);
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            view.layout(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bitmap));

            File dir = new File(activity.getCacheDir(), "shared");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create " + dir);
            File file = new File(dir, fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            bitmap.recycle();

            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TEXT, text)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(send, chooserTitle));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.no_app_to_open, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unable to share " + fileName, e);
            Toast.makeText(activity, R.string.no_app_to_open, Toast.LENGTH_SHORT).show();
        }
    }
}
