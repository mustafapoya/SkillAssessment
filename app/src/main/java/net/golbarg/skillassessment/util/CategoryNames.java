package net.golbarg.skillassessment.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.models.InterviewPack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Human readable names and a stable accent colour for topic slugs such as "node.js" or "t-sql".
 * Technology names stay as they are; the app's own modes (mixed practice, interview packs, …) are
 * translated once {@link #init} has run.
 */
public final class CategoryNames {
    private static final Map<String, String> OVERRIDES = new HashMap<>();
    private static final Map<String, String> MONOGRAMS = new HashMap<>();
    /** Pseudo topics whose names come from string resources. */
    private static final Map<String, Integer> LOCALIZED = new HashMap<>();
    private static Context appContext;

    static {
        OVERRIDES.put("aws", "AWS");
        OVERRIDES.put("aws-lambda", "AWS Lambda");
        OVERRIDES.put("c", "C");
        OVERRIDES.put("c#", "C#");
        OVERRIDES.put("c++", "C++");
        OVERRIDES.put("css", "CSS");
        OVERRIDES.put("dotnet-framework", ".NET Framework");
        OVERRIDES.put("front-end-development", "Front-End Development");
        OVERRIDES.put("html", "HTML");
        OVERRIDES.put("it-operations", "IT Operations");
        OVERRIDES.put("javascript", "JavaScript");
        OVERRIDES.put("jquery", "jQuery");
        OVERRIDES.put("json", "JSON");
        OVERRIDES.put("matlab", "MATLAB");
        OVERRIDES.put("microsoft-power-bi", "Microsoft Power BI");
        OVERRIDES.put("mongodb", "MongoDB");
        OVERRIDES.put("mysql", "MySQL");
        OVERRIDES.put("node.js", "Node.js");
        OVERRIDES.put("nosql", "NoSQL");
        OVERRIDES.put("object-oriented-programming", "Object-Oriented Programming");
        OVERRIDES.put("objective-c", "Objective-C");
        OVERRIDES.put("php", "PHP");
        OVERRIDES.put("quickbooks", "QuickBooks");
        OVERRIDES.put("r", "R");
        OVERRIDES.put("rest-api", "REST API");
        OVERRIDES.put("ruby-on-rails", "Ruby on Rails");
        OVERRIDES.put("search-engine-optimization", "SEO");
        OVERRIDES.put("sharepoint", "SharePoint");
        OVERRIDES.put("t-sql", "T-SQL");
        OVERRIDES.put("vba", "VBA");
        OVERRIDES.put("wordpress", "WordPress");
        OVERRIDES.put("xml", "XML");
        OVERRIDES.put("mixed-practice", "Mixed practice");
        OVERRIDES.put("daily-challenge", "Daily challenge");
        OVERRIDES.put("mistakes-review", "Mistakes review");
        OVERRIDES.put("bookmarked-questions", "Saved questions");
        OVERRIDES.put("speed-round", "Speed round");
        OVERRIDES.put("interview-android", "Android developer");
        OVERRIDES.put("interview-ios", "iOS developer");
        OVERRIDES.put("interview-frontend", "Frontend developer");
        OVERRIDES.put("interview-backend", "Backend developer");
        OVERRIDES.put("interview-dotnet", ".NET developer");
        OVERRIDES.put("interview-data", "Data scientist");
        OVERRIDES.put("interview-devops", "DevOps & cloud engineer");

        LOCALIZED.put("mixed-practice", R.string.mixed_practice);
        LOCALIZED.put("daily-challenge", R.string.daily_challenge);
        LOCALIZED.put("mistakes-review", R.string.mistakes_review);
        LOCALIZED.put("bookmarked-questions", R.string.saved_title);
        LOCALIZED.put("speed-round", R.string.speed_round);
        for (InterviewPack pack : InterviewPack.values()) LOCALIZED.put(pack.slug, pack.title);

        MONOGRAMS.put("c#", "C#");
        MONOGRAMS.put("c++", "C++");
        MONOGRAMS.put("dotnet-framework", ".NET");
        MONOGRAMS.put("node.js", "JS");
        MONOGRAMS.put("javascript", "JS");
        MONOGRAMS.put("t-sql", "SQL");
        MONOGRAMS.put("mysql", "SQL");
        MONOGRAMS.put("objective-c", "OC");
        MONOGRAMS.put("object-oriented-programming", "OOP");
        MONOGRAMS.put("search-engine-optimization", "SEO");
        MONOGRAMS.put("rest-api", "API");
        MONOGRAMS.put("mixed-practice", "MX");
        MONOGRAMS.put("daily-challenge", "DC");
        MONOGRAMS.put("mistakes-review", "RV");
        MONOGRAMS.put("bookmarked-questions", "SV");
        MONOGRAMS.put("speed-round", "SR");
        MONOGRAMS.put("interview-android", "AN");
        MONOGRAMS.put("interview-ios", "iOS");
        MONOGRAMS.put("interview-frontend", "FE");
        MONOGRAMS.put("interview-backend", "BE");
        MONOGRAMS.put("interview-dotnet", ".NET");
        MONOGRAMS.put("interview-data", "DS");
        MONOGRAMS.put("interview-devops", "OPS");
    }

    /** The groups offered as filter chips on the home screen. */
    public enum Domain {
        ALL(R.string.domain_all),
        LANGUAGES(R.string.domain_languages),
        FRONTEND(R.string.domain_frontend),
        BACKEND(R.string.domain_backend),
        MOBILE(R.string.domain_mobile),
        DATABASES(R.string.domain_databases),
        CLOUD_DEVOPS(R.string.domain_cloud_devops),
        AI_DATA(R.string.domain_ai_data),
        TOOLS_OTHER(R.string.domain_tools);

        public final int titleRes;

        Domain(int titleRes) {
            this.titleRes = titleRes;
        }
    }

    public static Domain getDomain(String slug) {
        if (slug == null) return Domain.TOOLS_OTHER;
        switch (slug) {
            case "c":
            case "c#":
            case "c++":
            case "go":
            case "java":
            case "kotlin":
            case "php":
            case "python":
            case "r":
            case "rust":
            case "scala":
            case "swift":
            case "objective-c":
            case "vba":
                return Domain.LANGUAGES;

            case "html":
            case "css":
            case "javascript":
            case "react":
            case "angular":
            case "jquery":
            case "front-end-development":
                return Domain.FRONTEND;

            case "node.js":
            case "django":
            case "spring-framework":
            case "dotnet-framework":
            case "ruby-on-rails":
            case "rest-api":
                return Domain.BACKEND;

            case "android":
            case "unity":
                return Domain.MOBILE;

            case "mysql":
            case "t-sql":
            case "mongodb":
            case "nosql":
            case "microsoft-access":
                return Domain.DATABASES;

            case "aws":
            case "aws-lambda":
            case "google-cloud-platform":
            case "microsoft-azure":
            case "git":
            case "linux":
            case "bash":
            case "it-operations":
            case "cybersecurity":
                return Domain.CLOUD_DEVOPS;

            case "machine-learning":
            case "matlab":
            case "hadoop":
            case "microsoft-power-bi":
                return Domain.AI_DATA;

            default:
                return Domain.TOOLS_OTHER;
        }
    }

    private CategoryNames() {
    }

    /** Lets pseudo-topic names follow the app language; call once from the Application. */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static String displayName(String slug) {
        if (slug == null || slug.isEmpty()) return "";
        Integer localized = LOCALIZED.get(slug);
        if (localized != null && appContext != null) return appContext.getString(localized);
        String override = OVERRIDES.get(slug);
        if (override != null) return override;
        StringBuilder sb = new StringBuilder();
        for (String word : slug.split("-")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (word.equals("on") || word.equals("of")) {
                sb.append(word);
            } else {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return sb.toString();
    }

    /** Up to three characters shown in the topic badge. */
    public static String monogram(String slug) {
        String preset = MONOGRAMS.get(slug);
        if (preset != null) return preset;
        String name = displayName(slug);
        String[] words = name.split("[\\s-]+");
        if (name.length() <= 3 && !name.contains(" ")) return name;
        if (words.length >= 2) {
            return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase(Locale.ROOT);
        }
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1, 2).toLowerCase(Locale.ROOT);
    }

    /** Fills a topic badge with the topic's monogram on its own tinted background. */
    public static void styleBadge(TextView badge, String slug, float cornerRadiusDp) {
        Context context = badge.getContext();
        boolean dark = UiUtils.isNightMode(context);
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(UiUtils.dp(context, cornerRadiusDp));
        background.setColor(badgeBackground(slug, dark));
        badge.setBackground(background);
        badge.setTextColor(badgeForeground(slug, dark));
        badge.setText(monogram(slug));
    }

    /** A hue derived from the slug, so each topic keeps the same colour everywhere. */
    private static int accentHue(String slug) {
        int hash = slug == null ? 0 : slug.hashCode();
        return Math.abs(hash % 360);
    }

    private static int badgeBackground(String slug, boolean dark) {
        return Color.HSVToColor(new float[]{accentHue(slug), dark ? 0.45f : 0.22f, dark ? 0.32f : 0.96f});
    }

    private static int badgeForeground(String slug, boolean dark) {
        return Color.HSVToColor(new float[]{accentHue(slug), dark ? 0.35f : 0.75f, dark ? 0.95f : 0.45f});
    }
}
