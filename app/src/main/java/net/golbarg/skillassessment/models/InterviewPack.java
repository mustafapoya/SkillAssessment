package net.golbarg.skillassessment.models;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.golbarg.skillassessment.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A curated exam mixing the topics a job interview for one role tends to cover. Questions are drawn
 * only from the pack's topics the user has unlocked. Ids are stored as a result's topic, so they must
 * never change.
 */
public enum InterviewPack {
    ANDROID(-101, "interview-android", R.string.pack_android, "android", "kotlin", "java"),
    IOS(-102, "interview-ios", R.string.pack_ios, "swift", "objective-c"),
    FRONTEND(-103, "interview-frontend", R.string.pack_frontend, "javascript", "html", "css", "react", "angular", "front-end-development"),
    BACKEND(-104, "interview-backend", R.string.pack_backend, "node.js", "rest-api", "spring-framework", "django", "mysql", "nosql"),
    DOTNET(-105, "interview-dotnet", R.string.pack_dotnet, "c#", "dotnet-framework", "t-sql"),
    DATA(-106, "interview-data", R.string.pack_data, "python", "machine-learning", "r", "t-sql", "microsoft-power-bi"),
    DEVOPS(-107, "interview-devops", R.string.pack_devops, "linux", "bash", "git", "aws", "microsoft-azure", "google-cloud-platform");

    /** Questions in every interview exam. */
    public static final int LENGTH = 20;

    public final int id;
    public final String slug;
    @StringRes public final int title;
    public final List<String> topics;

    InterviewPack(int id, String slug, @StringRes int title, String... topics) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.topics = Collections.unmodifiableList(Arrays.asList(topics));
    }

    @Nullable
    public static InterviewPack fromId(int id) {
        for (InterviewPack pack : values()) if (pack.id == id) return pack;
        return null;
    }

    @Nullable
    public static InterviewPack fromSlug(String slug) {
        for (InterviewPack pack : values()) if (pack.slug.equals(slug)) return pack;
        return null;
    }
}
