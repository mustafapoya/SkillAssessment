package net.golbarg.skillassessment.util;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Human readable names and a stable accent colour for topic slugs such as "node.js" or "t-sql". */
public final class CategoryNames {
    private static final Map<String, String> OVERRIDES = new HashMap<>();
    private static final Map<String, String> MONOGRAMS = new HashMap<>();

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
    }

    public enum Domain {
        ALL(net.golbarg.skillassessment.R.string.domain_all),
        LANGUAGES(net.golbarg.skillassessment.R.string.domain_languages),
        FRONTEND(net.golbarg.skillassessment.R.string.domain_frontend),
        BACKEND(net.golbarg.skillassessment.R.string.domain_backend),
        MOBILE(net.golbarg.skillassessment.R.string.domain_mobile),
        DATABASES(net.golbarg.skillassessment.R.string.domain_databases),
        CLOUD_DEVOPS(net.golbarg.skillassessment.R.string.domain_cloud_devops),
        AI_DATA(net.golbarg.skillassessment.R.string.domain_ai_data),
        TOOLS_OTHER(net.golbarg.skillassessment.R.string.domain_tools);

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

    public static String displayName(String slug) {
        if (slug == null || slug.isEmpty()) return "";
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

    /** A pleasant hue derived from the slug so each topic keeps the same colour. */
    public static int accentHue(String slug) {
        int hash = slug == null ? 0 : slug.hashCode();
        return Math.abs(hash % 360);
    }

    public static int badgeBackground(String slug, boolean dark) {
        return Color.HSVToColor(new float[]{accentHue(slug), dark ? 0.45f : 0.22f, dark ? 0.32f : 0.96f});
    }

    public static int badgeForeground(String slug, boolean dark) {
        return Color.HSVToColor(new float[]{accentHue(slug), dark ? 0.35f : 0.75f, dark ? 0.95f : 0.45f});
    }
}
