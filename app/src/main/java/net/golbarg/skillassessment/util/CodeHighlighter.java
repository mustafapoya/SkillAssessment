package net.golbarg.skillassessment.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import androidx.core.content.ContextCompat;

import net.golbarg.skillassessment.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small, dependency-free syntax highlighter. It is not a full lexer, but it recognises comments,
 * strings, numbers, keywords and markup tags for the language families found in the question bank.
 */
public final class CodeHighlighter {

    private enum Family { C_LIKE, HASH, SQL, MARKUP, CSS }

    private static final String KEYWORDS = "abstract|and|as|assert|async|await|base|bool|boolean|break|byte|case|catch|char|class|const|constexpr|continue|def|default|defer|del|delegate|do|double|elif|else|enum|except|export|extends|extern|false|final|finally|float|fn|for|foreach|from|fun|func|function|go|goto|guard|if|impl|implements|import|in|include|inline|instanceof|int|interface|internal|is|lambda|let|long|loop|match|mod|module|mut|namespace|native|new|nil|None|not|null|nullptr|object|operator|or|override|package|pass|private|protected|pub|public|raise|readonly|ref|return|self|Self|short|signed|sizeof|static|string|String|struct|super|switch|template|then|this|throw|throws|trait|True|False|true|try|type|typedef|typeof|union|unsigned|use|using|val|var|virtual|void|volatile|when|where|while|with|yield|echo|fi|done|esac|elsif|end|unless|require|begin|rescue|puts|print|println|printf|std|cout|endl";

    private static final String SQL_KEYWORDS = "(?i:select|from|where|insert|into|values|update|set|delete|create|table|alter|drop|index|view|join|inner|left|right|outer|full|cross|on|group|by|order|having|limit|offset|union|all|distinct|as|and|or|not|null|is|in|between|like|exists|case|when|then|else|end|primary|key|foreign|references|default|constraint|unique|check|begin|commit|rollback|transaction|declare|procedure|function|return|returns|top|with|count|sum|avg|min|max|asc|desc|int|varchar|nvarchar|char|text|date|datetime|decimal|bit|if|exec|go)";

    private static final Map<Family, Pattern> PATTERNS = new HashMap<>();

    /** Android's ICU regex engine is stricter than the JVM's; never let a bad pattern crash rendering. */
    private static void compile(Family family, String regex) {
        try {
            PATTERNS.put(family, Pattern.compile(regex));
        } catch (RuntimeException e) {
            Log.e("CodeHighlighter", "Invalid pattern for " + family, e);
        }
    }

    static {
        String strings = "\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'";
        String number = "\\b(?:0x[0-9a-fA-F]+|\\d+(?:\\.\\d+)?)[fFlLdDuU]?\\b";

        compile(Family.C_LIKE, (
                "(?<comment>//[^\\n]*|/\\*[\\s\\S]*?\\*/)|(?<string>" + strings + "|`(?:\\\\.|[^`\\\\])*`)|(?<keyword>\\b(?:" + KEYWORDS + ")\\b|#\\s*\\w+|@\\w+)|(?<number>" + number + ")"));
        compile(Family.HASH, (
                "(?<comment>#[^\\n]*)|(?<string>\"\"\"[\\s\\S]*?\"\"\"|" + strings + ")|(?<keyword>\\b(?:" + KEYWORDS + ")\\b|@\\w+|\\$\\{?\\w+\\}?)|(?<number>" + number + ")"));
        compile(Family.SQL, (
                "(?<comment>--[^\\n]*|/\\*[\\s\\S]*?\\*/)|(?<string>" + strings + ")|(?<keyword>\\b" + SQL_KEYWORDS + "\\b)|(?<number>" + number + ")"));
        compile(Family.MARKUP, (
                "(?<comment><!--[\\s\\S]*?-->)|(?<string>" + strings + ")|(?<tag></?[A-Za-z][\\w:.-]*|/?>)|(?<attr>\\b[\\w:@\\[\\]()*#-]+(?=\\s*=))"));
        compile(Family.CSS, (
                "(?<comment>/\\*[\\s\\S]*?\\*/)|(?<string>" + strings + ")|(?<keyword>@[\\w-]+|!important)|(?<attr>[\\w-]+(?=\\s*:[^:]))|(?<number>#[0-9a-fA-F]{3,8}\\b|-?\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|s|ms|deg|fr)?)"));
    }

    private CodeHighlighter() {
    }

    /** True when every language pattern compiled on this device's regex engine. */
    public static boolean allPatternsCompiled() {
        return PATTERNS.size() == Family.values().length;
    }

    public static CharSequence highlight(Context context, String code, String language, String categorySlug) {
        Family family = familyFor(language, categorySlug);
        Pattern pattern = PATTERNS.get(family);
        SpannableString spannable = new SpannableString(code);
        if (pattern == null) return spannable;

        int comment = ContextCompat.getColor(context, R.color.code_comment);
        int string = ContextCompat.getColor(context, R.color.code_string);
        int keyword = ContextCompat.getColor(context, R.color.code_keyword);
        int number = ContextCompat.getColor(context, R.color.code_number);
        int tag = ContextCompat.getColor(context, R.color.code_tag);
        int attr = ContextCompat.getColor(context, R.color.code_attr);

        Matcher m = pattern.matcher(code);
        while (m.find()) {
            if (m.start() == m.end()) continue;
            if (hasGroup(family, "comment") && m.group("comment") != null) {
                span(spannable, m, comment);
                spannable.setSpan(new StyleSpan(Typeface.ITALIC), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (hasGroup(family, "string") && m.group("string") != null) {
                span(spannable, m, string);
            } else if (hasGroup(family, "keyword") && m.group("keyword") != null) {
                span(spannable, m, keyword);
            } else if (hasGroup(family, "tag") && m.group("tag") != null) {
                span(spannable, m, tag);
            } else if (hasGroup(family, "attr") && m.group("attr") != null) {
                span(spannable, m, attr);
            } else if (hasGroup(family, "number") && m.group("number") != null) {
                span(spannable, m, number);
            }
        }
        return spannable;
    }

    /** Which named groups a family's pattern defines; asking a pattern for a missing group throws. */
    private static boolean hasGroup(Family family, String group) {
        switch (group) {
            case "tag": return family == Family.MARKUP;
            case "attr": return family == Family.MARKUP || family == Family.CSS;
            case "keyword": return family != Family.MARKUP;
            case "number": return family != Family.MARKUP;
            default: return true;
        }
    }

    private static void span(SpannableString s, Matcher m, int color) {
        s.setSpan(new ForegroundColorSpan(color), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static Family familyFor(String language, String slug) {
        String lang = language == null ? "" : language.toLowerCase(Locale.ROOT);
        if (lang.isEmpty()) lang = slug == null ? "" : slug.toLowerCase(Locale.ROOT);
        switch (lang) {
            case "python": case "py": case "bash": case "sh": case "shell": case "shall": case "linux":
            case "ruby": case "rb": case "ruby-on-rails": case "r": case "yaml": case "yml": case "perl":
            case "powershell": case "django": case "machine-learning": case "hadoop":
                return Family.HASH;
            case "sql": case "mysql": case "t-sql": case "tsql": case "microsoft-access": case "nosql":
                return Family.SQL;
            case "html": case "xml": case "angular2html": case "xquery": case "svg": case "vue":
            case "front-end-development": case "wordpress":
                return Family.MARKUP;
            case "css": case "scss": case "less":
                return Family.CSS;
            default:
                return Family.C_LIKE;
        }
    }
}
