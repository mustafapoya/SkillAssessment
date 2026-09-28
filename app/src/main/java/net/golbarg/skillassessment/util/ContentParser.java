package net.golbarg.skillassessment.util;

import net.golbarg.skillassessment.models.ContentBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits the markdown-like question text into ordered blocks: prose, fenced code and images.
 * Order is preserved so a question like "Given this code … what is printed?" reads correctly.
 */
public final class ContentParser {
    private static final String FENCE = "```";
    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*]\\(\\s*([^)\\s]+)\\s*\\)");

    private ContentParser() {
    }

    public static List<ContentBlock> parse(String raw) {
        List<ContentBlock> blocks = new ArrayList<>();
        if (raw == null) return blocks;
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');

        int cursor = 0;
        while (cursor < text.length()) {
            int open = text.indexOf(FENCE, cursor);
            if (open < 0) {
                addText(blocks, text.substring(cursor));
                break;
            }
            addText(blocks, text.substring(cursor, open));

            int lineEnd = text.indexOf('\n', open + FENCE.length());
            int close = text.indexOf(FENCE, open + FENCE.length());
            String language;
            int codeStart;
            if (lineEnd >= 0 && (close < 0 || lineEnd < close)) {
                language = text.substring(open + FENCE.length(), lineEnd).trim();
                codeStart = lineEnd + 1;
            } else {
                // Single-line fence such as ```a = 1```
                language = "";
                codeStart = open + FENCE.length();
            }
            int codeEnd = close < 0 ? text.length() : close;
            if (codeEnd < codeStart) codeEnd = codeStart;
            String code = trimBlankLines(text.substring(codeStart, codeEnd));
            if (!code.isEmpty()) blocks.add(new ContentBlock(ContentBlock.Type.CODE, code, language.toLowerCase()));
            cursor = close < 0 ? text.length() : close + FENCE.length();
        }
        return blocks;
    }

    private static void addText(List<ContentBlock> blocks, String segment) {
        Matcher m = IMAGE.matcher(segment);
        int last = 0;
        while (m.find()) {
            addPlain(blocks, segment.substring(last, m.start()));
            blocks.add(new ContentBlock(ContentBlock.Type.IMAGE, m.group(1).replace("\\/", "/"), null));
            last = m.end();
        }
        addPlain(blocks, segment.substring(last));
    }

    private static void addPlain(List<ContentBlock> blocks, String segment) {
        String cleaned = segment.trim();
        if (!cleaned.isEmpty()) blocks.add(new ContentBlock(ContentBlock.Type.TEXT, cleaned, null));
    }

    private static String trimBlankLines(String code) {
        String result = code.replace("\t", "    ");
        while (result.startsWith("\n")) result = result.substring(1);
        int end = result.length();
        while (end > 0 && Character.isWhitespace(result.charAt(end - 1))) end--;
        return result.substring(0, end);
    }

    /** A plain one-line preview with code fences and images removed, for compact lists. */
    public static String preview(String raw) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : parse(raw)) {
            if (block.getType() == ContentBlock.Type.TEXT) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(block.getContent().replace("`", "").replace('\n', ' '));
            }
        }
        if (sb.length() == 0) {
            for (ContentBlock block : parse(raw)) {
                if (block.getType() == ContentBlock.Type.CODE) return block.getContent().replace('\n', ' ');
            }
        }
        return sb.toString();
    }
}
