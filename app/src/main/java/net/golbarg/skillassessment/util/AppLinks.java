package net.golbarg.skillassessment.util;

import net.golbarg.skillassessment.BuildConfig;

/** Where the app points people: its store listing, the website and the support address. */
public final class AppLinks {
    public static final String EMAIL = "contact@golbarg.net";
    public static final String WEBSITE = "https://golbarg.net";
    public static final String PLAY_URL = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
    /** Opens the listing in the Play Store app; fall back to {@link #PLAY_URL} without it. */
    public static final String MARKET_URI = "market://details?id=" + BuildConfig.APPLICATION_ID;

    private AppLinks() {
    }
}
