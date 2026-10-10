# Publishing Milestone — the checklist

Everything in this folder is store content. None of it ships inside the app, and
nothing here is referenced by the build.

**Upload `app/build/outputs/bundle/release/app-release.aab`.** Play wants the App
Bundle, not the APK. The APK in `app/build/outputs/apk/release/` is only for
sideloading and testing on a device.

| | |
| --- | --- |
| Package | `com.milestone.app` · versionCode 1 · versionName 1.0 |
| Privacy policy | https://hamzasgh1998.github.io/goalTracker/play/privacy-policy/ |
| Public contact | supportdevtn@gmail.com |
| Category | Productivity |

**The long pole:** this is a personal developer account that has never published, so
Play requires **12 testers opted in continuously for 14 days** on a closed track
before you may even apply for production access. Everything else below takes an
afternoon. Start the clock on day one.

---

## Day 0 — back up the keystore before anything else

`milestone-release.jks` and `keystore.properties` exist in exactly one place, in the
project root, and are deliberately not in version control. If that file is lost
*before* you enrol in Play App Signing, **you can never update this app again** — not
under this package name, ever. Copy both to a password manager attachment, an
encrypted drive, or both, and store the password separately from the file.

The repo being public means the source is readable by anyone. The keystore,
`keystore.properties` and `local.properties` are gitignored and have never been
committed — keep it that way.

---

## 1. Create the app

**Play Console → All apps → Create app**

| Field | Value |
| --- | --- |
| App name | `Milestone: Goals & To-Do List` |
| Default language | English (United States) |
| App or game | App |
| Free or paid | **Free** |

Tick the declarations (developer programme policies, US export laws).
Free → paid is impossible afterwards; free is correct here and stays correct.

---

## 2. First upload, and the one irreversible decision

**Test and release → Testing → Internal testing → Create new release**

1. Upload `app-release.aab`.
2. Play offers **Play App Signing** — **accept it.** Google then holds the key that
   signs what users install, and your `.jks` becomes only an *upload* key, which
   Google can reset if it is ever lost. This is the only choice in the whole process
   that can never be undone.
3. Release name `1 (1.0)`, release notes `First release.`
4. **Save** — the App content forms come before reviewing the release.

Then check **App bundle explorer**: versionCode 1, and exactly four permissions
(`POST_NOTIFICATIONS`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`).

---

## 3. App content — every declaration

**Monitor and improve → Policy and programmes → App content.** Every row must go
green or no release can be reviewed.

| Section | Answer |
| --- | --- |
| Privacy policy | `https://hamzasgh1998.github.io/goalTracker/play/privacy-policy/` |
| App access | All functionality is available without special access |
| Ads | No, my app does not contain ads |
| Content rating | Questionnaire → supportdevtn@gmail.com → **Utility, Productivity, Communication or Other** → every content question **No** → "allows users to interact" **No**. Expect Everyone / PEGI 3. Details in `content-rating.md` |
| Target audience and content | **18 and over** — avoids the extra children's-design questions. Do **not** opt into Designed for Families |
| News app | No |
| COVID-19 contact tracing or status | No |
| Data safety | *Does your app collect or share user data?* → **No**. Reasoning in `data-safety.md` |
| Advertising ID | No, my app does not use advertising ID |
| Government apps | No |
| Financial features | My app doesn't provide any financial features |
| Health | No health features — the "Health" life-area label is a user category, not health data |

A **Foreground service** section should not appear: that permission was removed and
the rebuilt AAB no longer declares it. If it shows up, you uploaded a stale bundle —
rebuild with `./gradlew clean :app:bundleRelease`.

---

## 4. Store listing and settings

**Grow users → Store presence → Main store listing**

| Console field | File |
| --- | --- |
| App name | `listing/title.txt` (29 of 30 characters) |
| Short description | `listing/short-description.txt` (75 of 80) |
| Full description | `listing/full-description.txt` (3,079 of 4,000) |
| App icon | `graphics/icon-512.png` (512×512) |
| Feature graphic | `graphics/feature-graphic-1024x500.png` (1024×500, no alpha) |
| Phone screenshots | `graphics/screenshots/01…08` — upload **in filename order**, which is deliberate |

**Store settings:** category Productivity · tags: goal setting, habits, planner,
to-do list · contact email supportdevtn@gmail.com (shown publicly on the listing).

The screenshots are padded to 1200×2400 rather than the phone's native 1080×2400 on
purpose: Play rejects an image whose longer side is more than twice the shorter one,
and 1080×2400 is just over that line. The padding is the app's own background colour,
so it is invisible.

---

## 5. Internal testing — same day, ~30 minutes

No review, so it is the fastest way to see the real Play-delivered app.

1. **Internal testing → Testers** → email list with your own account → copy the
   **opt-in URL**.
2. Open it on your phone, accept, install from Play.
3. Use it for ten minutes: a goal with 3–4 milestones, tick one, add a step, write a
   reflection, export a backup, turn the weekly reminder on.
4. Read the **pre-launch report** — Google runs the app on real devices and reports
   crashes, ANRs, accessibility and policy problems. Fix anything red before
   strangers see it.

---

## 6. Closed testing — start the 14-day clock the same day

**Test and release → Testing → Closed testing → Create track** (call it `Beta`).

1. **Promote** the internal release into it rather than re-uploading.
2. Make a **Google Group** (e.g. `milestone-testers@googlegroups.com`) and use the
   group as the tester list, so people can be added or removed without editing the
   track.
3. Recruit **12 real Google accounts**. Each must open the opt-in link, accept, and
   stay opted in for the full 14 days. Play counts testers opted in *continuously*,
   so recruit **14–15**, not exactly 12 — one person leaving on day 9 breaks the run.
4. **Keep a short feedback log** — who said what, what you changed. Step 7 asks
   exactly this, and a vague answer is the usual reason an application is bounced.

Day 0 is the day the **12th** tester opts in, not the day you created the track.
Watch the count under **Closed testing → Testers**.

---

## 7. After 14 days — apply for production access

**Test and release → Production → Apply for production access.** A short form: how
you recruited testers, what feedback you got, what you changed. Answer concretely
from the log. Reviewed manually; allow up to about a week.

---

## 8. Production release

1. **Production → Create new release** → promote the same bundle. If you rebuild, you
   must bump `versionCode` to 2 in `app/build.gradle.kts`.
2. **Countries/regions → all.** Nothing in the app is region-specific.
3. Rollout **100%** — staged rollout protects existing users, of which there are none
   yet.
4. **Send for review.** A first review on a new account usually takes a few days.

---

## 9. After it is live

- **Android vitals** is your only crash signal; the app ships no crash reporting by
  choice. Check it weekly for the first month.
- Reply to reviews from the Console. supportdevtn@gmail.com is what users see.
- **Every future update:** bump `versionCode`, rebuild, sign with the same upload
  key. Play rejects a duplicate versionCode.

---

## Not blocking, worth knowing

- **Room is at schema version 1 with no migration.** The moment a tester installs, the
  schema is frozen: any future change needs a `Migration` or their data is gone.
  `app/schemas/…/1.json` is already exported, so the migration test harness is cheap
  to add — and much cheaper now than after version 2 exists.
- **No tablet or large-screen layouts.** Play's large-screen quality guidelines will
  flag this; it does not block publishing.
- **No themed (monochrome) icon**, so Android 13+ themed icons fall back to the
  colour one.
- **No crash reporting**, by design.
