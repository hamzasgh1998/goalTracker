# Publishing Milestone — the checklist

Everything in this folder is store content. None of it ships inside the app, and
nothing here is referenced by the build.

**Upload `app/build/outputs/bundle/release/app-release.aab`.** Play wants the App
Bundle, not the APK. The APK in `app/build/outputs/apk/release/` is only for
sideloading and testing on a device.

---

## Before you open the console

1. **Back up the signing keystore somewhere off this machine.**
   `milestone-release.jks` and `keystore.properties` exist in exactly one place, in
   the project root, and are deliberately not in version control. If that file is
   lost and you have not enrolled in Play App Signing, **you can never update this
   app again** — not under this package name, ever. Copy it to a password manager
   attachment, an encrypted drive, or both.

2. **Enrol in Play App Signing when prompted at the first upload.** Say yes. Google
   then holds the key that signs what users install, and your local keystore becomes
   only an upload key — which Google can reset for you if you lose it. Declining is
   the one decision in this process that cannot be undone later.

3. **Host the privacy policy.** Play will not accept the listing without a public
   URL. The contact address (supportdevtn@gmail.com) is already filled in, and the
   page is a single self-contained HTML file with no external assets.

   This repo is public, and GitHub Pages serves the whole tree from the root, so the
   policy needs no separate repo and no copying — just turn Pages on:

   ```
   github.com → hamzasgh1998/goalTracker → Settings → Pages
       Source: "Deploy from a branch"
       Branch: main    Folder: / (root)    → Save
   ```

   First build takes about a minute. The policy is then live at:

   **https://hamzasgh1998.github.io/goalTracker/play/privacy-policy/**

   That is the URL for the Play Console. Confirm it is really serving before pasting
   it in — a 404 here fails the listing review:

   ```sh
   curl -sI https://hamzasgh1998.github.io/goalTracker/play/privacy-policy/ | head -1   # 200
   ```

   When the policy changes, edit it here, commit and push: Pages rebuilds itself.

   Note that the repo being public means the source is readable by anyone. The
   signing keystore, `keystore.properties` and `local.properties` are gitignored and
   have never been committed — keep it that way.

---

## Filling in the console

| Console field | File |
| --- | --- |
| App name | `listing/title.txt` (29 of 30 characters) |
| Short description | `listing/short-description.txt` (75 of 80) |
| Full description | `listing/full-description.txt` (3,051 of 4,000) |
| App icon | `graphics/icon-512.png` (512×512) |
| Feature graphic | `graphics/feature-graphic-1024x500.png` (1024×500, no alpha) |
| Phone screenshots | `graphics/screenshots/01…08` (1200×2400, in order) |
| Privacy policy URL | the page you hosted above |
| Data safety form | `data-safety.md` — answer by answer |
| Content rating questionnaire | `content-rating.md` — answer by answer |
| Category | Productivity |

The screenshots are padded to 1200×2400 rather than the phone's native 1080×2400 on
purpose: Play rejects an image whose longer side is more than twice the shorter one,
and 1080×2400 is just over that line. The padding is the app's own background
colour, so it is invisible.

## Still missing, and only you can supply it

- A developer account (one-off 25 USD) and, for a personal account created after
  November 2023, Play's testing requirement: **12 testers opted in for 14 continuous
  days** before you may apply for production access. Start that clock early — it is
  usually the longest pole in the tent.
- Contact email for the store listing (shown publicly on the listing page).
- Countries, pricing (free), and the content guidelines / US export declarations.

## Not blocking, worth knowing

- **Room is at schema version 1 with no migration.** The moment a real user installs,
  the schema is frozen: any future change needs a `Migration` or their data is gone.
  `app/schemas/…/1.json` is already exported, so the migration test harness is cheap
  to add before version 2.
- **No tablet or large-screen layouts.** Play's large-screen quality guidelines will
  flag this; it does not block publishing.
- **No themed (monochrome) icon**, so Android 13+ themed icons fall back to the
  colour one.
- **No crash reporting**, by design — you will hear about production crashes from
  reviews rather than from a dashboard.
