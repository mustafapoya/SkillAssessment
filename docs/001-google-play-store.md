# CodeQuiz — Google Play Store Kit

Everything to set or answer in Google Play Console for **CodeQuiz: Programming Tests** (`net.golbarg.skillassessment`). The store text is in [002-store-listing.md](002-store-listing.md) and the privacy policy in [003-privacy-policy.md](003-privacy-policy.md). Google reviews the listing and the declarations against the app, so keep every statement here true when features change.

| | |
| --- | --- |
| **Application ID** | `net.golbarg.skillassessment` |
| **Current version** | `versionName 2.0` / `versionCode 6` — `app/build.gradle` (re-check before every upload) |
| **Store text** | [002-store-listing.md](002-store-listing.md): `en-US` (default) |
| **Privacy policy** | [003-privacy-policy.md](003-privacy-policy.md), published at `https://golbarg.net/codequiz_app/privacy_policy/` |
| **Support email** | `contact@golbarg.net` |
| **Website** | `https://golbarg.net` |
| **Target audience** | 13+ |
| **Ads** | Google AdMob, account `pub-8976959600358837` — see [§5](#5-ads--monetization) |

**Open items**
- ⚠️ Publish [003](003-privacy-policy.md) at `https://golbarg.net/codequiz_app/privacy_policy/` and enter it in Play Console. The app's About → Privacy policy row opens that page. The old address `https://golbarg.net/privacy/skillassessment` returns 404, and Play rejects apps with ads whose policy link is broken.
- ⚠️ Create and publish a GDPR message in AdMob → Privacy & messaging. Without it the app can't ask users in the EEA, UK and Switzerland for consent, and they get few or no ads.
- ⚠️ Publish `google.com, pub-8976959600358837, DIRECT, f08c47fec0942fa0` in `https://golbarg.net/app-ads.txt`.

---

## 1. App facts

CodeQuiz is an offline quiz app for programming and IT skills: 3,800+ multiple-choice questions on 58 topics, with practice, exam, study and interview modes, progress tracking and certificates.

### Technical profile

| | |
| --- | --- |
| **SDK** | `minSdk 28` (Android 9) · `compileSdk 36` · `targetSdk 36` |
| **Content** | The questions are built into the app; it works without a connection |
| **Network** | Only Google services: AdMob, the UMP consent SDK and Google Play (billing, in-app review) |
| **Storage** | The user's name (optional), test history and answers, bookmarks, unlocked topics and coins, streaks and achievements, an unfinished test, settings, ad and rating counters — all on the device. Backup / restore goes to a file the user picks |
| **Accounts** | None |
| **Notifications & widgets** | A daily reminder (scheduled on the phone) and a **Question of the day** home-screen widget |
| **Play libraries** | Billing (`BillingManager`; Premium `premium_lifetime`, not on sale: `FOR_SALE = false`) and in-app review |
| **Monetization** | Free with Google AdMob ads (banners, interstitials, rewarded); coins unlock topics; Premium hides every ad |
| **Languages** | English only |

### Features

- **Ways to practice:**
  - Practice mode with instant feedback and a 50/50 hint; timed Exam mode; Study mode.
  - Daily challenge, speed round, interview prep packs, learning paths.
  - Mistakes review.
- **Progress:** accuracy, weekly activity and mastery per topic; streaks, daily goal, 16 achievements; certificates for 80 %+ on tests of 10+ questions; saved questions with search.
- **58 topics** across languages, web, backend, databases, cloud, data/AI and tools; each costs 1 coin to unlock.
- **Daily use:** syntax-highlighted code with adjustable size, dark mode, tablet and landscape layouts, reminder, widget, backup & restore.

---

## 2. Store settings

| Setting | Value | Notes |
| --- | --- | --- |
| **App or game** | App | |
| **App category** | Education | |
| **Tags** | Education, Programming, Quiz, Exam prep, Career | Up to 5, in Store settings |
| **Contact email** | `contact@golbarg.net` | |
| **Website** | `https://golbarg.net` | |
| **Privacy policy URL** | `https://golbarg.net/codequiz_app/privacy_policy/` | Text from [003](003-privacy-policy.md) |
| **Account deletion URL** | Not applicable | The app has no accounts |
| **Contains ads** | Yes | Banners, interstitials, rewarded videos |
| **In-app purchases** | No, while `BillingManager.FOR_SALE = false` | Change this answer if Premium goes on sale |
| **Price** | Free | |

---

## 3. Target audience & content rating

- **Target age groups:** 13–15, 16–17, 18+. Don't select an under-13 group: that puts the app under the Families policy, which needs a different ad setup.
- **Appeals to children?** No: the app is for students, job seekers and software professionals.
- **Content rating (IARC):**
  - Category *Reference, News, or Educational*.
  - Violence, sexuality, language, controlled substances, gambling: No.
  - User interaction or content exchange: No. Sharing a result through Android's share sheet doesn't count.
  - Shares location: No.
  - Digital purchases: No while `FOR_SALE = false`.
  - Expected result: Everyone / PEGI 3 / USK 0.

---

## 4. Graphics

| Asset | Size | Notes |
| --- | --- | --- |
| App icon | 512 × 512 PNG, 32-bit, max 1 MB | The white "101" on the lime-green square (`brand_lime_dark` `#57CC02`); must match the launcher icon |
| Feature graphic | 1024 × 500 JPEG or 24-bit PNG | Dark background (`#0A0C0B`), the icon on the left, the text from [002](002-store-listing.md) on the right, a few topic badges (Python, Java, React, AWS). Keep text out of the outer edges |
| Phone screenshots | 2–8, 1080 × 1920 or larger, 9:16 | Storyboard below; captions in [002](002-store-listing.md) |
| Tablet screenshots | 7" and 10", e.g. 1200 × 1920 or 2560 × 1600 | Explore with the multi-column topic grid, and a test screen in landscape |

### Screenshot storyboard

Use a real-looking name and some progress, not an empty first-run state.

| # | Screen | What to show | Caption |
| --- | --- | --- | --- |
| 1 | Explore | Daily goal, practice tiles and topics | 002 caption 1 |
| 2 | Test | A question with a code snippet | 002 caption 2 |
| 3 | Test | Right/wrong feedback after Check | 002 caption 3 |
| 4 | Result | The score screen | 002 caption 4 |
| 5 | Progress | Accuracy ring, week chart, achievements | 002 caption 5 |
| 6 | Interview prep or a learning path | | 002 caption 6 |
| 7 | Certificate | | 002 caption 7 |
| 8 | Explore in dark mode | | 002 caption 8 |

Take screenshots with `AdConfig.ADS_ENABLED = false` so no ad appears.

---

## 5. Ads & monetization

Ads are controlled from `ads/AdConfig` (standard ads package, see `docs/ads` in the MobileApps workspace). Content is capped at PG. Premium users never see or load ads (`AdManager.userIsAdFree` → `BillingManager.isPremium`). With `ADS_ENABLED = false`, rewards (coins, 50/50 hint, streak restore) are given without a video.

| Placement | Format | Where | Rule |
| --- | --- | --- | --- |
| `HOME_BANNER` | Banner | Explore (`HomeFragment`) | Hidden until an ad loads |
| `BOOKMARK_BANNER` | Banner | Saved questions | |
| `PROFILE_BANNER` | Banner | Progress | |
| `RESULT_BANNER` | Banner | Test result | |
| `QUESTION_BANNER` | Banner | Test screen, under the top bar | Never next to the answers or the Check button |
| `TEST_START_INTERSTITIAL` | Interstitial | After choosing a test, before its first question | See the test rules below |
| `TEST_FINISH_INTERSTITIAL` | Interstitial | After a finished test, before its result | See the test rules below; never after fewer than 3 answers |
| `COINS_REWARDED` | Rewarded | Coins sheet; "Watch & unlock" | Opt-in; +1 coin |
| `LIFE_REWARDED` | Rewarded | 50/50 hint (once per test); streak restore | Opt-in |

**Test rules** (`ui/question/TestAds`, shared by both interstitials):
- The first 2 tests are ad-free.
- At least 2 finished tests between two interstitials.
- At most one full-screen ad (interstitial or rewarded) every 3 minutes.

**Pre-test triggers:**
- Test setup sheet (topics, mixed practice, saved questions), daily challenge, review, sprint, interview packs, learning-path levels, focus practice, and Retry / Practice mistakes on the result screen.
- Never when resuming a saved test, previewing a locked topic, or opening a test from the widget or a reminder.

### AdMob console

| Item | Status |
| --- | --- |
| App ID `ca-app-pub-8976959600358837~4153212745` | ✅ in `AndroidManifest.xml` |
| Nine ad units | ✅ in `AdConfig` |
| GDPR message | ⚠️ create and publish |
| `app-ads.txt` | ⚠️ publish |

---

## 6. Play Console declarations

### App access
All functionality is available without special access. There is no login.

### Ads
Contains ads: Yes. Banners, interstitials before and after some tests, and optional rewarded videos, all from Google AdMob. If a release ships with `ADS_ENABLED = false`, answer No and declare no AdMob data.

### Advertising ID
Yes, through the Google Mobile Ads SDK (`com.google.android.gms.permission.AD_ID`). Purposes: Advertising or marketing, Analytics, Fraud prevention, security and compliance.

### Data safety

The app itself collects nothing: names, answers, history, bookmarks and settings stay on the device, and backups go only to a file the user picks. The only collection comes from the Google Mobile Ads SDK; see [Google's data disclosure for the Mobile Ads SDK](https://developers.google.com/admob/android/privacy/play-data-disclosure) when updating the SDK.

| Question / data type | Answer | Purpose | Optional? |
| --- | --- | --- | --- |
| Does the app collect or share user data? | Yes, through the AdMob SDK | | |
| Is data encrypted in transit? | Yes | | |
| Can users request deletion? | No | No account; clearing app data or uninstalling deletes everything on the phone; ad data is managed through the user's Google account | |
| Approximate location (from IP) | Collected and shared | Advertising or marketing, analytics, fraud prevention, security and compliance | No |
| App interactions (ad views and taps) | Collected and shared | Same | No |
| Crash logs, diagnostics (ad diagnostics) | Collected and shared | Same | No |
| Device or other IDs (advertising ID, app set ID) | Collected and shared | Same | No |
| Everything else | Not collected | The name and quiz history never leave the phone | |

### Permissions

| Permission | Why | Play Console form needed? |
| --- | --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Ads and Google Play services | No |
| `POST_NOTIFICATIONS` | Daily reminder, Android 13+ | No |
| `RECEIVE_BOOT_COMPLETED` | Re-set the reminder after a restart | No |
| `com.google.android.gms.permission.AD_ID` | AdMob | Advertising ID declaration (above) |
| `FOREGROUND_SERVICE`, `WAKE_LOCK` (added by Google's libraries) | Short background tasks of the libraries | If asked: the app itself starts no foreground service |

### Other declarations
Government, financial, health, news, COVID-19, data deletion: Not applicable.

---

## 7. Names & content rights

The listing names the technologies each topic covers, which is fine. It never names other companies' certifications or products as endorsements, such as "LinkedIn skill assessment": Play's metadata policy rejects listings that imply an affiliation.

---

## 8. Release checklist

- [ ] `versionCode` raised and `versionName` set in `app/build.gradle`; header of this file updated
- [ ] Release notes written in [002](002-store-listing.md)
- [ ] Ads: `AdConfig.ADS_ENABLED = true`, `USE_TEST_ADS = false`; Logcat shows `Ads: on, REAL units`. Never tap your own live ads
- [ ] [003](003-privacy-policy.md) still matches the app; if it changed, **Last updated** set and the web page republished
- [ ] Data safety and permissions in §6 still match the merged release manifest
- [ ] If Premium goes on sale (`FOR_SALE = true`): In-app purchases → Yes, IARC digital purchases → Yes, policy §6 still accurate
- [ ] Screenshots and captions still match the app
- [ ] Signed release bundle built (R8 is on) and smoke-tested from Internal testing
- [ ] Release history below updated

---

## 9. Release history

| Version | versionCode | Date | Highlights |
| --- | --- | --- | --- |
| 2.0 | 6 | Not released yet | New name (CodeQuiz) and design; practice/exam/study modes; daily challenge, speed round, interview prep, learning paths; progress, achievements, certificates; reminder, widget, backup |
