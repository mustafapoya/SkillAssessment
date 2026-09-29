# Google Play Store Listing & Store Preferences

This document contains complete store listing metadata, store settings, categorization, content rating, data safety declarations, and store optimization guidelines for **CodeQuiz** on the Google Play Console.

---

## 1. App Store Metadata

### App Details
| Field | Limit | Production Content |
| :--- | :--- | :--- |
| **App Name** | Max 30 chars | `CodeQuiz: Programming Tests` *(alt: `CodeQuiz - Coding Assessment`)* |
| **Short Description** | Max 80 chars | `Master 58+ programming skills with assessment tests, quizzes, and certificates.` |
| **Default Language** | — | `English (United States) – en-US` |
| **Package Name** | — | `net.golbarg.skillassessment` |
| **Version Name / Code**| — | `2.0` (Version Code `6`) |

---

### Full Description (Play Store Compliant & SEO Optimized)

```text
Prepare for technical interviews and assess your coding skills with CodeQuiz! Practice thousands of real-world assessment questions across 58 programming languages, frameworks, cloud platforms, databases, and developer tools.

Whether you are preparing for job interviews, LinkedIn skill assessments, university exams, or want to sharpen your technical edge, CodeQuiz provides structured quizzes and instant feedback to accelerate your learning.

🚀 WHY CODEQUIZ?
• 58 Technical Topics: From Python and JavaScript to AWS, Docker, Git, and Machine Learning.
• Real-World Assessment Style: Multiple-choice questions crafted around practical scenarios, edge cases, and industry standards.
• Instant Explanations & Answers: Reveal answers and learn the logic immediately or simulate real exam conditions.
• Offline Ready: Practice anywhere without internet connectivity.
• Track Your Mastery: Visual progress charts, streak tracking, accuracy breakdowns, and topic mastery levels.
• Earn Certificates: Pass comprehensive exams and unlock verified shareable achievement certificates.

📚 58 TOPICS ACROSS 8 MAJOR DOMAINS:
1. Programming Languages:
   Python, Java, JavaScript, C, C++, C#, Kotlin, Swift, Go, Rust, Ruby, PHP, Scala, Objective-C, Bash, VBA, MATLAB

2. Frontend Development:
   HTML5, CSS3, React, Angular, jQuery, Front-End Development Best Practices

3. Backend & Frameworks:
   Node.js, Django, Spring Framework, Ruby on Rails, .NET Framework, REST APIs, JSON, XML

4. Databases & Data Storage:
   MySQL, MongoDB, NoSQL, T-SQL, Microsoft Access

5. Cloud & DevOps:
   Amazon Web Services (AWS), AWS Lambda, Google Cloud Platform (GCP), Microsoft Azure, Linux, IT Operations

6. AI, Machine Learning & Analytics:
   Machine Learning, R, Google Analytics, Google Ads, Microsoft Power BI

7. Developer Tools & Methodologies:
   Git, Agile Methodologies, Eclipse, SharePoint, QuickBooks, SEO, OOP (Object-Oriented Programming)

8. Mobile & Game Development:
   Android Development, Unity Game Engine

🎯 POWERFUL LEARNING MODES:
• Practice Mode: Instant answer validation, detailed explanations, and 50/50 hints.
• Exam Mode: Timed full-length assessments simulating actual industry skill certification exams.
• Study Mode: Self-paced browsing with interactive tap-to-reveal answers.
• Daily Challenge & Goal: Daily questions to keep your knowledge sharp and build long-lasting habits.
• Bookmark & Mistake Review: Save tricky questions and retake questions you missed to achieve 100% mastery.

🔒 PRIVACY & NO FORCED LOGINS:
• No mandatory account creation or intrusive permissions.
• All stats, bookmarks, and quiz progress are stored securely on your device with local backup & restore options.
• Transparent ad experience with optional Premium upgrade to remove ads completely.

Level up your developer career today. Download CodeQuiz and test your knowledge now!
```

---

## 2. Store Settings & Categorization

### Category & Tags
- **Application Type**: `App`
- **Primary Category**: `Education`
- **Secondary Category**: `Trivia` / `Productivity` *(optional where supported)*
- **Store Tags**:
  - `Education`
  - `Programming`
  - `Developer Tools`
  - `Quiz`
  - `Trivia`
  - `Brain Games`
  - `Job Search / Career Prep`

### Store Listing Contact Details
- **Email Address**: `contact@golbarg.net` *(or official developer email)*
- **Website**: `https://golbarg.net`
- **Privacy Policy URL**: `https://golbarg.net/privacy/skillassessment`

---

## 3. Content Rating (IARC Questionnaire Guidelines)

| Question Category | Selection | Rationale |
| :--- | :--- | :--- |
| **Violence** | No | Educational technical quiz content only. |
| **Sexuality / Nudity** | No | None. |
| **Language / Profanity** | No | Professional technical content. |
| **Controlled Substances** | No | None. |
| **User Interaction & Location Sharing** | No | App does not share location or host user chat. |
| **Purchases** | Yes | Optional in-app purchase for "Premium" lifetime unlock. |
| **Resulting Rating** | **Everyone (PEGI 3, USK 0, ESRB Everyone)** | Safe for all developer audiences. |

---

## 4. Target Audience & Content Declaration

- **Target Age Group**: `13 and older` (13–15, 16–17, 18+)
- **Families Policy Requirement**: `No` (App is aimed at students, job seekers, and software professionals).
- **Ads Declaration**: `Yes, this app contains ads` (AdMob banners, non-intrusive post-test interstitials, rewarded videos).
- **Financial Features**: `None` (Does not provide financial/loan services).
- **Health / Medical Features**: `None`.
- **Government Apps**: `No`.

---

## 5. Data Safety Declaration (Play Console)

### Summary
The app stores quiz progress, bookmarks, and statistics **locally on the device**. Network communication is strictly utilized for:
1. Google Mobile Ads (AdMob) & Consent (Google UMP)
2. Google Play In-App Billing (Google Play Core)
3. Remote Question update checks (if online)

### Detailed Data Safety Form Mapping

| Data Type | Collected | Shared | Purpose | Ephemeral / Optional |
| :--- | :--- | :--- | :--- | :--- |
| **App info and performance** (Crash logs, diagnostics) | Yes (via Google Play Services) | Yes (Google) | Analytics, App performance | Handled automatically by Google Play / AdMob |
| **Device or other IDs** (Advertising ID) | Yes (AdMob) | Yes (Google Ads) | Advertising, Fraud prevention, Personalization (with consent) | Optional based on UMP user consent (can be reset by user) |
| **Personal Info** (Name, email, phone) | No | No | N/A | User name in profile is stored 100% locally on device |
| **Financial Info** | No | No | N/A | In-app purchases handled directly by Google Play Billing |
| **Location** | No | No | N/A | Not collected |
| **Photos / Media / Files** | No | No | N/A | Local backups are created through Android Storage Access Framework (user-picked location) |

- **Security Practices**:
  - Data encrypted in transit: `Yes` (All network calls use HTTPS/TLS).
  - Request data deletion: `Yes` (User can clear all history/data locally inside settings anytime).

---

## 6. Graphic Assets Specifications

| Asset | Size / Format | Description / Design Recommendation |
| :--- | :--- | :--- |
| **App Icon** | 512 × 512 px (PNG 32-bit, max 1MB) | High-contrast modern icon featuring a clean code terminal symbol `</>` with a subtle checkmark / achievement accent in indigo & teal gradient. |
| **Feature Graphic** | 1024 × 500 px (JPEG or PNG, max 15MB) | Sleek modern dark backdrop with floating code cards (Python, React, AWS, Java) and title *"58+ Technologies • Master Coding Assessments"*. |
| **Phone Screenshots** (Min 4, Max 8) | Min 1080 × 1920 px (16:9 or 9:16) | 1. **Explore 58+ Topics**: Showing language grid & domain filters.<br>2. **Interactive Quiz**: Code question view with syntax highlight & 50/50 hint.<br>3. **Instant Answers & Explanations**: Detailed feedback screen.<br>4. **Exam & Study Modes**: Timed certification test mode.<br>5. **Track Mastery & Streaks**: Analytics, graphs, daily goals, accuracy.<br>6. **Earn Certificates**: Verified Certificate of Achievement preview. |
| **Tablet Screenshots** (7" & 10") | 1200 × 1920 px or 2048 × 1536 px | Tablet layout showing the responsive grid and study mode. |

---

## 7. Release Notes (What's New in v2.0)

```text
What's new in version 2.0:
• Added Study Mode: Browse questions at your own pace with tap-to-reveal answers.
• 50/50 Hints: Use hints during practice tests to eliminate wrong answers.
• Certificates of Achievement: Complete exams with 70%+ score to earn shareable certificates.
• Daily Goals & Streak Recovery: Set daily question goals and protect your hard-earned streak.
• Question Search: Instantly search across all unlocked questions.
• Enhanced Explanations & Content: Updated questions and fixes across 58 topics.
• Performance & UI Improvements: Smoother navigation, dark mode refinement, and code font sizing.
```

---

## 8. App Store Optimization (ASO) Strategy & Keywords

### Primary Keywords:
`programming quiz`, `coding interview preparation`, `python assessment`, `javascript test`, `developer quiz`, `skill assessment`, `aws certification practice`, `coding test`, `tech interview questions`, `learn programming`.

### Secondary Keywords:
`java quiz`, `sql test`, `react practice questions`, `git commands quiz`, `docker devops quiz`, `machine learning test`, `c++ practice`, `html css test`, `daily coding challenges`.
