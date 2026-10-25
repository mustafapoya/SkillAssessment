package net.golbarg.skillassessment.ads;

import com.google.android.gms.ads.RequestConfiguration;

/**
 * The only file to edit to control ads. Change a value and rebuild.
 * See docs/ads/README.md (MobileApps workspace) for the quick card.
 */
public final class AdConfig {

    // ── Master switches ─────────────────────────────────────────────────────
    // false = no ads anywhere: no SDK start, no consent form, no ad requests,
    // and rewards (coins, 50/50 hint, streak restore) are given without a video
    public static final boolean ADS_ENABLED = true;
    // true = Google's demo ad units (while developing), false = the real units below
    public static final boolean USE_TEST_ADS = false;

    // ── Rules ───────────────────────────────────────────────────────────────
    // The store listing is rated Everyone (3+), so ads are kept to PG content
    static final String MAX_CONTENT_RATING = RequestConfiguration.MAX_AD_CONTENT_RATING_PG;
    // Shortest time between two full-screen ads (interstitial or rewarded), any placement
    static final long FULLSCREEN_MIN_GAP_MS = 3 * 60 * 1000L;

    // ── Placements: every spot in the app that can show an ad ───────────────
    // ON / OFF turns one spot on or off. Real unit IDs come from the AdMob console.
    // An empty real ID keeps that spot off while USE_TEST_ADS is false.
    private static final boolean ON = true;
    private static final boolean OFF = false;

    public enum Placement {
        HOME_BANNER             (AdFormat.BANNER,       ON, "ca-app-pub-8976959600358837/8503467396"),
        BOOKMARK_BANNER         (AdFormat.BANNER,       ON, "ca-app-pub-8976959600358837/8078632423"),
        PROFILE_BANNER          (AdFormat.BANNER,       ON, "ca-app-pub-8976959600358837/7190385727"),
        RESULT_BANNER           (AdFormat.BANNER,       ON, "ca-app-pub-8976959600358837/6765550752"),
        // On the test screen: under the top bar, away from the answers and the Check button
        QUESTION_BANNER         (AdFormat.BANNER,       ON, "ca-app-pub-8976959600358837/9485430471"),
        // Before a test the user has just chosen
        TEST_START_INTERSTITIAL (AdFormat.INTERSTITIAL, ON, "ca-app-pub-8976959600358837/8499651861"),
        // After a finished test, before its result
        TEST_FINISH_INTERSTITIAL(AdFormat.INTERSTITIAL, ON, "ca-app-pub-8976959600358837/7186570198"),
        // Coins for unlocking topics
        COINS_REWARDED          (AdFormat.REWARDED,     ON, "ca-app-pub-8976959600358837/5452469083"),
        // Second chances: the 50/50 hint in a test and restoring a lost streak
        LIFE_REWARDED           (AdFormat.REWARDED,     ON, "ca-app-pub-8976959600358837/1966033109");

        final AdFormat format;
        final boolean on;
        final String realUnitId;

        Placement(AdFormat format, boolean on, String realUnitId) {
            this.format = format;
            this.on = on;
            this.realUnitId = realUnitId;
        }
    }

    private AdConfig() {
    }
}
