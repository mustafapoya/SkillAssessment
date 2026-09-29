# Ad Units Inventory & Monetization Architecture

This document details all advertising placements, ad unit identifiers, formats, trigger locations, business logic, and Google AdMob console configurations for **CodeQuiz** (`net.golbarg.skillassessment`).

---

## 1. AdMob App & Account Overview

| Parameter | Configuration Value |
| :--- | :--- |
| **App Name** | `CodeQuiz` |
| **Package ID** | `net.golbarg.skillassessment` |
| **AdMob App ID** | `ca-app-pub-8976959600358837~4153212745` |
| **Primary Ad Network** | Google AdMob (`com.google.android.gms:play-services-ads`) |
| **Consent Management** | Google User Messaging Platform (UMP SDK) |
| **Max Ad Content Rating** | `MAX_AD_CONTENT_RATING_PG` (Family/Safe content) |
| **Debug Mode Behavior** | Automatically uses Google Official Test Ad Units in debug builds |

---

## 2. Complete Ad Units Inventory Table

| # | Ad Type | Suggested AdMob Unit Name | Production Ad Unit ID | Test Ad Unit ID (Google) | App Location / Screen | Primary Trigger / Placement |
| :- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | **Anchored Adaptive Banner** | `CodeQuiz_Banner_Home` | `ca-app-pub-8976959600358837/8503467396` | `ca-app-pub-3940256099942544/9214589741` | **Explore / Home Screen** (`HomeFragment`) | Bottom anchored banner on main topic list. Collapsed if ad fails or user is Premium. |
| **2** | **Anchored Adaptive Banner** | `CodeQuiz_Banner_Saved` | `ca-app-pub-8976959600358837/8078632423` | `ca-app-pub-3940256099942544/9214589741` | **Saved Questions Screen** (`BookmarkFragment`) | Bottom anchored banner when browsing bookmarked questions. |
| **3** | **Anchored Adaptive Banner** | `CodeQuiz_Banner_Progress` | `ca-app-pub-8976959600358837/7190385727` | `ca-app-pub-3940256099942544/9214589741` | **Progress / Profile Screen** (`ProfileFragment`) | Bottom anchored banner on user statistics & achievements screen. |
| **4** | **Anchored Adaptive Banner** | `CodeQuiz_Banner_Result` | `ca-app-pub-8976959600358837/6765550752` | `ca-app-pub-3940256099942544/9214589741` | **Test Results Screen** (`QuestionResultActivity`) | Bottom anchored banner displayed during score & review summary. |
| **5** | **Interstitial (Full Screen)** | `CodeQuiz_Interstitial_TestFinish` | `ca-app-pub-8976959600358837/7186570198` | `ca-app-pub-3940256099942544/1033173712` | **Post-Quiz Transition** (`QuestionActivity` -> `QuestionResultActivity`) | Natural transition break after submitting a test. Subject to strict frequency caps & grace rules. |
| **6** | **Rewarded Video** | `CodeQuiz_Rewarded_UserAction` | `ca-app-pub-8976959600358837/5452469083` | `ca-app-pub-3940256099942544/5224354917` | **Multi-location (Opt-In)**: <br>• Coin Sheet (`CreditsSheet`)<br>• 50/50 Hint (`QuestionActivity`)<br>• Streak Recovery (`HomeFragment`) | User explicitly opts in to earn in-app rewards: <br>• `+1 Coin` to unlock new topics<br>• `50/50 Hint` to discard 2 incorrect options<br>• `Streak Freeze` to restore missed practice day |

---

## 3. Detailed Placement & Logic Specifications

### 1. Anchored Adaptive Banners
- **Components**: `HomeFragment`, `BookmarkFragment`, `ProfileFragment`, `QuestionResultActivity`.
- **User Experience Safeguard**:
  - Banners are **never placed during active testing/quiz screens** (`QuestionActivity`) to prevent accidental clicks or distracting users.
  - The container stays `View.GONE` and zero-height until the ad is successfully loaded, preventing content shifts.
  - Automatically destroyed on lifecycle `onDestroy` and paused on `onPause`.
  - Instantly hidden and cleaned when the user purchases Premium.

---

### 2. Post-Test Interstitial Ad
- **Component**: `AdManager.java` -> `showInterstitialAfterTest()`
- **Natural Break Guarantee**: Google Play policy strictly mandates interstitials only at natural transition points. The ad triggers only after the test is completed before showing the result page.
- **Rules & Frequency Capping**:
  1. **New User Grace Period**: First **2 tests** are 100% ad-free (`GRACE_TESTS = 2`).
  2. **Test Spacing**: At least **2 finished tests** must occur between any two interstitial displays (`TESTS_BETWEEN_INTERSTITIALS = 2`).
  3. **Meaningful Effort Rule**: Never shown if a user abandons a test early or answered fewer than 3 questions (`MIN_ANSWERED_FOR_INTERSTITIAL = 3`).
  4. **Cooldown Timer**: Enforces a strict minimum **3-minute gap** (`3 * 60 * 1000 ms`) after any full-screen ad (interstitial or rewarded).
  5. **Pre-loading & Expiry**: Interstitials are preloaded in the background and discarded if older than **1 hour** to maintain fresh bids.

---

### 3. Rewarded Video Ads
- **Components**: `CreditsSheet`, `QuestionActivity`, `HomeFragment`
- **Opt-in Transparency**: Clear upfront explanation of the exact reward before initiating playback.
- **Reward Use-Cases**:
  1. **Unlock Topics (+1 Coin)**: Users can watch rewarded videos to earn coins and unlock any of the 58 available topics permanently for free.
  2. **50/50 Hint in Practice**: Users can watch a short video during difficult questions to eliminate half of the wrong options (limited to 1 hint per test).
  3. **Streak Rescue**: If a user missed a practice day, watching a rewarded video restores their active streak.
- **Cooldown Coordination**: Showing a rewarded ad also counts toward the global full-screen cooldown timer, preventing an interstitial from immediately appearing afterwards.

---

## 4. Privacy, Compliance & Consent (GDPR / UMP / CCPA)

- **Consent Framework**: Implemented via Google User Messaging Platform (UMP) SDK in [AdManager.java](file:///d:/work/codes/mine/SkillAssessment/app/src/main/java/net/golbarg/skillassessment/ads/AdManager.java).
- **Consent Gathering Flow**:
  - `AdManager.gatherConsent(activity)` runs upon startup in `MainActivity`.
  - In EEA/UK and applicable US regions, Google's certified CMP consent form is presented automatically.
  - No ads are requested or initialized until UMP `canRequestAds()` returns `true`.
- **Ad Privacy Settings Button**:
  - The `About` screen dynamically checks `isPrivacyOptionsRequired(context)`.
  - If required in the user's jurisdiction, an **"Ad privacy choices"** setting is displayed allowing users to adjust or revoke consent anytime.
- **COPPA / Child Safety**: Max ad content rating is restricted via `RequestConfiguration.MAX_AD_CONTENT_RATING_PG`.

---

## 5. Premium Monetization (Ad-Free Experience)

- **Product ID**: `premium_lifetime` (One-time Google Play In-App Purchase via [BillingManager.java](file:///d:/work/codes/mine/SkillAssessment/app/src/main/java/net/golbarg/skillassessment/billing/BillingManager.java)).
- **Ad Suppression Behavior**:
  - When `BillingManager.isPremium()` is active:
    - Banners are hidden and never requested.
    - Interstitials are bypassed completely.
    - All 58 topics are unlocked for free without needing coins or rewarded videos.
