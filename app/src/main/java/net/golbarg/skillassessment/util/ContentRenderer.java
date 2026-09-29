package net.golbarg.skillassessment.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.models.ContentBlock;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renders parsed question content into a vertical {@link LinearLayout}. */
public final class ContentRenderer {
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`\\n]+)`");

    public enum Style {
        QUESTION(R.style.TextAppearance_App_Question, 13),
        BODY(R.style.TextAppearance_App_Body, 13),
        COMPACT(R.style.TextAppearance_App_Caption, 12);

        @StyleRes final int textAppearance;
        final int codeSizeSp;

        Style(int textAppearance, int codeSizeSp) {
            this.textAppearance = textAppearance;
            this.codeSizeSp = codeSizeSp;
        }
    }

    private ContentRenderer() {
    }

    /**
     * @param clickTarget if set, taps on scrollable code blocks are forwarded to this view, so a
     *                    code-only answer option can still be selected.
     */
    public static void render(LinearLayout container, String raw, String categorySlug, Style style, @Nullable View clickTarget) {
        container.removeAllViews();
        container.setOrientation(LinearLayout.VERTICAL);
        Context context = container.getContext();
        List<ContentBlock> blocks = ContentParser.parse(raw);
        int gap = UiUtils.dp(context, 10);

        for (int i = 0; i < blocks.size(); i++) {
            ContentBlock block = blocks.get(i);
            View view;
            switch (block.getType()) {
                case CODE:
                    view = codeView(context, block, categorySlug, style, clickTarget);
                    break;
                case IMAGE:
                    view = imageView(context, block, categorySlug);
                    break;
                default:
                    view = textView(context, block.getContent(), style);
                    break;
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = gap;
            container.addView(view, lp);
        }
    }

    private static TextView textView(Context context, String text, Style style) {
        TextView tv = new TextView(context);
        TextViewCompat.setTextAppearance(tv, style.textAppearance);
        tv.setTextColor(UiUtils.color(context, com.google.android.material.R.attr.colorOnSurface));
        tv.setText(withInlineCode(context, text));
        return tv;
    }

    public static CharSequence withInlineCode(Context context, String text) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        Matcher m = INLINE_CODE.matcher(text);
        int last = 0;
        int bg = ContextCompat.getColor(context, R.color.inline_code_background);
        while (m.find()) {
            sb.append(text, last, m.start());
            int start = sb.length();
            sb.append(m.group(1));
            sb.setSpan(new TypefaceSpan("monospace"), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new BackgroundColorSpan(bg), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(0.9f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            last = m.end();
        }
        sb.append(text.substring(last));
        return sb;
    }

    private static View codeView(Context context, ContentBlock block, String slug, Style style, @Nullable View clickTarget) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setBackgroundResource(R.drawable.bg_code_block);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(true);

        TextView code = new TextView(context);
        int pad = UiUtils.dp(context, 14);
        code.setPadding(pad, UiUtils.dp(context, 12), pad, UiUtils.dp(context, 12));
        code.setTypeface(Typeface.MONOSPACE);
        int fontSize = Prefs.isLargeCodeFont(context) ? style.codeSizeSp + 2 : style.codeSizeSp;
        code.setTextSize(fontSize);
        code.setLineSpacing(0, 1.2f);
        code.setTextColor(ContextCompat.getColor(context, R.color.code_text));
        code.setText(CodeHighlighter.highlight(context, block.getContent(), block.getLanguage(), slug));
        code.setTextIsSelectable(false);
        scroll.addView(code, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (clickTarget != null) {
            code.setOnClickListener(v -> clickTarget.performClick());
        }
        return scroll;
    }

    private static View imageView(Context context, ContentBlock block, String slug) {
        ImageView image = new ImageView(context);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(UiUtils.dp(context, 320));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundResource(R.drawable.bg_image_block);
        int pad = UiUtils.dp(context, 8);
        image.setPadding(pad, pad, pad, pad);
        image.setMinimumHeight(UiUtils.dp(context, 48));
        image.setContentDescription(null);

        String src = block.getContent();
        String url = src.startsWith("http") ? src : "file:///android_asset/question_images/" + slug + "/" + src;
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .error(R.drawable.image_placeholder)
                .into(image);
        return image;
    }
}
