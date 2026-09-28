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
import net.golbarg.skillassessment.databinding.ViewShareCardBinding;
import net.golbarg.skillassessment.models.QuestionResult;

import java.io.File;
import java.io.FileOutputStream;

/** Renders a branded result card to an image and opens the share sheet. */
public final class ShareCard {
    private static final String PLAY_URL = "https://play.google.com/store/apps/details?id=net.golbarg.skillassessment";

    private ShareCard() {
    }

    public static void share(Activity activity, QuestionResult result, String topic, int bestStreak) {
        try {
            ViewShareCardBinding card = ViewShareCardBinding.inflate(LayoutInflater.from(activity));
            card.txtTopic.setText(topic);
            card.txtScore.setText(result.getScorePercent() + "%");
            card.txtDetail.setText(activity.getString(R.string.share_card_score_label, result.getCorrectAnswer(), result.getTotal()));
            card.txtStreak.setVisibility(bestStreak >= 2 ? View.VISIBLE : View.GONE);
            card.txtStreak.setText(activity.getString(R.string.best_streak, bestStreak));

            View view = card.getRoot();
            int width = UiUtils.dp(activity, 360);
            int height = UiUtils.dp(activity, 450);
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            view.layout(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bitmap));

            File dir = new File(activity.getCacheDir(), "shared");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create " + dir);
            File file = new File(dir, "result.png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            bitmap.recycle();

            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_result_message, result.getScorePercent(), topic, PLAY_URL))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(send, activity.getString(R.string.share_result)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.no_app_to_open, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ShareCard", "Unable to share result", e);
            Toast.makeText(activity, R.string.no_app_to_open, Toast.LENGTH_SHORT).show();
        }
    }
}
