package net.golbarg.skillassessment;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.golbarg.skillassessment.models.ContentBlock;
import net.golbarg.skillassessment.util.CodeHighlighter;
import net.golbarg.skillassessment.util.ContentParser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Parses and highlights every question and answer in the bundled question bank on a real Android
 * regex engine (ICU), which is stricter than the JVM one used by unit tests.
 */
@RunWith(AndroidJUnit4.class)
public class ContentRenderingTest {

    @Test
    public void everyQuestionParsesAndHighlights() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        JSONArray categories = new JSONObject(read(context.getAssets().open("questions.json"))).getJSONArray("questions");
        int codeBlocks = 0;
        for (int c = 0; c < categories.length(); c++) {
            JSONObject category = categories.getJSONObject(c);
            String slug = category.getString("title");
            JSONArray questions = category.getJSONArray("questions");
            for (int q = 0; q < questions.length(); q++) {
                JSONObject question = questions.getJSONObject(q);
                codeBlocks += highlightAll(context, question.getString("title"), slug);
                JSONArray answers = question.getJSONArray("answers");
                for (int a = 0; a < answers.length(); a++) {
                    codeBlocks += highlightAll(context, answers.getJSONObject(a).getString("title"), slug);
                }
            }
        }
        assertTrue("All highlighter patterns must compile on Android", CodeHighlighter.allPatternsCompiled());
        assertTrue("Expected the bank to contain code blocks", codeBlocks > 500);
    }

    private static int highlightAll(Context context, String raw, String slug) {
        int count = 0;
        for (ContentBlock block : ContentParser.parse(raw)) {
            if (block.getType() == ContentBlock.Type.CODE) {
                CodeHighlighter.highlight(context, block.getContent(), block.getLanguage(), slug);
                count++;
            }
        }
        return count;
    }

    private static String read(InputStream in) throws Exception {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
