# Google Play Store Listing & Console Answers

Everything to enter in Google Play Console for **CodeQuiz** (`net.golbarg.skillassessment`): the store listing, the App content declarations (privacy policy, ads, Data safety, content rating, target audience) and the release notes. Every statement here matches what version 2.0 actually does; keep it that way when features change, because Google reviews the listing and the declarations against the app.

> **Open items before submitting**
> - ⚠️ **Privacy policy URL**: publish [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) (or [`privacy_policy.json`](privacy_policy.json) on the website) and enter its address below. The address used before, `https://golbarg.net/privacy/skillassessment`, returns 404, and Play rejects apps with ads whose policy link is broken.
> - ⚠️ **Ads switch**: `AdConfig.ADS_ENABLED` (in `ads/AdConfig.java`) decides whether ads ship; check it before every release. The answers below assume ads are on (`true`). If you release with ads off, answer **No** to "Contains ads", and in Data safety declare only what Google Play itself needs (nothing from the app).
> - ⚠️ **GDPR consent message**: publish one in AdMob → Privacy & messaging. Without it the app can't ask users in the EEA, UK and Switzerland for consent, and they get few or no ads.

---

## 1. Store listing

| Field | Limit | Content |
| :--- | :--- | :--- |
| **App name** | 30 chars | `CodeQuiz: Programming Tests` (27) |
| **Short description** | 80 chars | `Practice 3,800+ coding quiz questions on 58 topics. Offline, no sign-up.` (72) |
| **Default language** | — | English (United States) – en-US |
| **Package name** | — | `net.golbarg.skillassessment` |
| **Version** | — | `2.0` (version code `6`) |

### Full description (4000 chars max, about 2,600 used)

```text
Get ready for coding interviews, exams and skill tests with CodeQuiz. Practice more than 3,800 multiple-choice questions across 58 programming languages, frameworks, cloud platforms, databases and developer tools, and see exactly where you stand.

No account, no sign-up: open the app and start practicing. Everything works offline, and your progress stays on your phone.

WAYS TO PRACTICE
• Practice mode: see right away whether you were right, and use a 50/50 hint when you are stuck.
• Exam mode: a timed test that shows your answers at the end, like the real thing.
• Study mode: go through a topic at your own pace and tap to reveal each answer.
• Daily challenge: a short mixed quiz every day to keep your skills sharp.
• Speed round: answer as many questions as you can against the clock.
• Interview prep: exams that mix the topics a job interview for your role tends to cover, such as Android, iOS or frontend developer.
• Learning paths: work through a topic level by level.
• Mistakes review: the questions you got wrong come back until you master them.

TRACK YOUR PROGRESS
• Accuracy, weekly activity and mastery for every topic
• Streaks, a daily goal and 16 achievements
• Earn a certificate when you score 80% or more on a test of 10+ questions, and share it
• Save tricky questions and search across all of them

58 TOPICS
Languages: Python, Java, JavaScript, Kotlin, Swift, C, C++, C#, Go, Rust, PHP, Scala, Objective-C, R, MATLAB, VBA, Bash
Web: HTML, CSS, React, Angular, jQuery, WordPress, Front-End Development
Backend and APIs: Node.js, Django, Spring Framework, Ruby on Rails, .NET Framework, REST API, JSON, XML
Databases and big data: MySQL, MongoDB, NoSQL, T-SQL, Microsoft Access, Hadoop
Cloud and operations: AWS, AWS Lambda, Google Cloud Platform, Microsoft Azure, Linux, IT Operations, Cybersecurity
Data, AI and marketing: Machine Learning, Microsoft Power BI, Google Analytics, Google Ads, SEO
Tools and practices: Git, Agile Methodologies, Object-Oriented Programming, Eclipse, SharePoint, QuickBooks
Mobile and games: Android, Unity

MADE FOR DAILY PRACTICE
• Code shown with syntax highlighting, and adjustable code text size
• Dark mode, and layouts for tablets and landscape
• A daily reminder and a "Question of the day" home-screen widget
• Back up your progress to a file and restore it on a new phone

You start with coins to unlock topics of your choice, and you can earn more by watching a short optional video. CodeQuiz is free and supported by ads.

Download CodeQuiz and find out how well you really know your stack.
```

### Contact details
- **Email:** `contact@golbarg.net`
- **Website:** `https://golbarg.net`
- **Privacy policy:** ⚠️ the published address of [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md)

### Category and tags
- **App or game:** App
- **Category:** Education
- **Tags** (pick up to 5 in Play Console → Store settings): Education, Programming, Quiz, Exam prep, Career

Don't name other companies' certifications or products as endorsements (for example "LinkedIn skill assessment"): Play's metadata policy rejects listings that imply an affiliation. Naming the technologies a topic covers, as above, is fine.

---

## 2. Graphics

| Asset | Size / format | What to show |
| :--- | :--- | :--- |
| **App icon** | 512 × 512 PNG, 32-bit, max 1 MB | The app's icon: the white "101" on the lime-green square (`brand_lime_dark` `#57CC02`), exported at 512 px. It must match the launcher icon. |
| **Feature graphic** | 1024 × 500 JPEG or 24-bit PNG | Dark background (`#0A0C0B`), the icon on the left, "CodeQuiz" and "3,800+ questions · 58 topics" on the right, and a few topic badges (Python, Java, React, AWS). Keep text out of the outer edges, which some layouts crop. |
| **Phone screenshots** | 2–8, 1080 × 1920 or larger, 9:16 | 1. Explore: daily goal, practice tiles and topics.<br>2. A question with a code snippet.<br>3. Right/wrong feedback after Check.<br>4. Result screen with the score.<br>5. Progress: accuracy ring, week chart, achievements.<br>6. Interview prep or a learning path.<br>7. Certificate.<br>8. The same Explore screen in dark mode. |
| **Tablet screenshots** | 7" and 10", e.g. 1200 × 1920 or 2560 × 1600 | Explore with the multi-column topic grid, and a test screen in landscape. |

Take screenshots with a real-looking name and some progress, not an empty first-run state, and keep test ads out of them.

---

## 3. App content declarations (Policy → App content)

### Privacy policy
⚠️ The published address of [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md).

### App access
**All functionality is available without special access.** There is no login.

### Ads
**Yes, my app contains ads.** Banners, interstitials before and after some tests, and optional rewarded videos, all from Google AdMob. See [`ADS_CONFIGURATION.md`](ADS_CONFIGURATION.md).

### Advertising ID
**Yes**, the app uses the advertising ID (through the Google Mobile Ads SDK, which adds `com.google.android.gms.permission.AD_ID`).
Purposes: **Advertising or marketing**, **Analytics**, **Fraud prevention, security and compliance**.

### Content rating (IARC questionnaire)
- **Category:** Reference, News, or Educational
- Violence, sexuality, language, controlled substances, gambling: **No** to all
- Users can interact or exchange content: **No** (sharing a result through Android's share sheet doesn't count; the app has no chat or user content)
- Shares the user's location: **No**
- Allows purchases of digital goods: **No** (`BillingManager.FOR_SALE` is `false`; change this answer if Premium goes on sale)
- Expected rating: **Everyone / PEGI 3 / USK 0**

### Target audience and content
- **Target age groups:** 13–15, 16–17, 18 and over. Don't select an under-13 group: that puts the app under the Families policy, which needs a different ad setup.
- **Appeals to children:** No. The app is for students, job seekers and software professionals.

### Other declarations
- News app: **No**
- Health, financial features, government app: **No**
- Uses the `FOREGROUND_SERVICE` permission only through Google libraries (added by the ads SDK). If Play Console asks for a foreground service declaration, the app itself starts none.

---

## 4. Data safety form

The app itself collects nothing: names, answers, history, bookmarks and settings stay on the device, and data that never leaves the device is not "collected" in Play's terms. Backups go only to a file the user picks. The only collection comes from the **Google Mobile Ads SDK**; Google lists what it collects in [Data disclosure for the Google Mobile Ads SDK](https://developers.google.com/admob/android/privacy/play-data-disclosure). Check that page when updating the SDK.

### Overview questions
| Question | Answer |
| :--- | :--- |
| Does your app collect or share any of the required user data types? | **Yes** (through the AdMob SDK) |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Do you provide a way for users to request that their data is deleted? | **No**. There is no account; data on the phone is deleted by clearing the app's data or uninstalling. Ad data is held by Google and managed through the user's Google account. |

### Data types
| Data type | Collected | Shared | Ephemeral | Required / optional | Purposes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Location → Approximate location** (from IP address) | Yes | Yes | No | Required | Advertising or marketing, Analytics, Fraud prevention, security and compliance |
| **App activity → App interactions** (ad views and taps) | Yes | Yes | No | Required | Advertising or marketing, Analytics, Fraud prevention, security and compliance |
| **App info and performance → Crash logs, Diagnostics** (ad diagnostics) | Yes | Yes | No | Required | Advertising or marketing, Analytics, Fraud prevention, security and compliance |
| **Device or other IDs** (advertising ID, app set ID) | Yes | Yes | No | Required | Advertising or marketing, Analytics, Fraud prevention, security and compliance |

Everything else (personal info, financial info, health, messages, photos and videos, audio, files and docs, calendar, contacts, web browsing, search history) is **not collected**. The name the user types and their quiz history never leave the phone.

---

## 5. Release notes for version 2.0 (500 chars max, about 460 used)

```text
CodeQuiz 2.0 is a whole new app:
• New name and design, with dark mode and tablet layouts
• Practice and timed exam modes, 50/50 hints and Study mode
• Daily challenge, speed round, interview prep and learning paths
• Mistakes review brings back the questions you missed
• Progress charts, streaks, achievements and shareable certificates
• Daily reminder, home-screen widget, and backup & restore
• Unlock any topic with one coin, earned from a short video
```

---

## 6. Store search (ASO)

Put the most important words in the app name and short description, then use them naturally in the full description. Play ignores keyword lists and penalises keyword stuffing.

- **Primary:** programming quiz, coding quiz, coding interview, programming test, developer quiz, learn programming
- **Topic searches:** python quiz, java quiz, javascript test, sql quiz, react questions, aws practice, kotlin quiz, c++ quiz, git quiz, machine learning quiz
- **Intent:** interview prep, tech interview questions, skill assessment, exam practice
