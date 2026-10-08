# Data Safety form — the answers, in form order

Play Console → **Policy → App content → Data safety**. The form is mandatory even
for an app that collects nothing; the listing cannot be published without it.

Answer exactly this. Every answer below is verifiable from the app: it declares no
`INTERNET` permission, so it cannot transmit anything anywhere.

---

### Section 1 — Data collection and security

**Does your app collect or share any of the required user data types?**
→ **No**

> Why this is the right answer, and not a judgement call: the form asks about data
> *the developer or a third party receives*. Milestone has no backend, no SDKs and
> no network permission. Data typed into the app never leaves the device, so nothing
> is collected or shared in Play's sense of either word.
>
> The export feature is not collection. It runs only when the user taps it, it hands
> a file to an app the user picks, and nothing about it reaches the developer. Play's
> own guidance excludes data that only moves because the user explicitly asked for it
> to move to a destination they chose.

Answering **No** ends the data-collection branch. The remaining questions below are
the ones Play still asks.

**Is all of the user data collected by your app encrypted in transit?**
→ Not shown once you answer No above. If it appears, answer **No data is collected**
(there is nothing in transit; the app has no network access).

**Do you provide a way for users to request that their data is deleted?**
→ Not shown once you answer No above. If it appears: **No** — data lives only on the
device and is removed by uninstalling the app or clearing its data in Android
Settings. There is no developer-held copy to request the deletion of.

---

### Section 2 — Store listing fields that sit next to this form

| Field | Answer |
| --- | --- |
| Privacy policy URL | the page you host from `play/privacy-policy/` |
| Does your app contain ads? | **No** |
| In-app purchases | **None** |
| App access (does part of the app require login?) | **All functionality available without special access** |
| Target audience | **18+** (or 13+). The app is not designed for children; keep it out of the "designed for families" programme. |
| Government app | **No** |
| Financial features | **None** |
| Health apps declaration | **None.** Milestone has a "Health" life-area label, but it stores no health data and makes no health claims. Do not tick any health category. |
| Category | **Productivity** |
| Tags | goal setting, habits, planner, to-do list |

---

### Section 3 — What the form will look wrong about

If a reviewer questions the "no data collected" answer, the three facts that settle
it:

1. `AndroidManifest.xml` declares no `INTERNET` permission. Verify with
   `aapt2 dump badging app-release.apk | grep uses-permission` — the only
   permissions are `POST_NOTIFICATIONS` and four WorkManager ones.
2. There are no analytics, ads, attribution or crash-reporting dependencies in
   `app/build.gradle.kts`.
3. The only data egress is the user-initiated backup, which goes to a destination
   the user picks through the system share sheet.
