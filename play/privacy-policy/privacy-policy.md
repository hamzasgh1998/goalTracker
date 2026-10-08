# Privacy Policy — Milestone: Goals & To-Do List

**Last updated: 8 October 2026**

Milestone is an offline app. It has no servers, no accounts and no analytics. This
policy is short because there is very little to describe.

## What Milestone collects

**Nothing.** The developer of Milestone receives no data about you or your use of
the app — not your goals, not your reflections, not usage statistics, not crash
reports, not an advertising identifier.

## What Milestone stores, and where

Everything you type into Milestone — goals, milestones, steps, life areas, seasons,
weekly reflections and your display name — is written to a database that lives
inside the app's private storage on your own device. It is not transmitted anywhere.

That data is removed when you uninstall the app or clear its data from Android
Settings. Because it is only on your device, the developer cannot recover it for you.

## Does the app connect to the internet?

No. Milestone does not request the `INTERNET` permission, so the operating system
will not allow it to open a network connection at all. The app works identically in
flight mode.

The permissions it does request are:

| Permission | Why |
| --- | --- |
| `POST_NOTIFICATIONS` | To show the optional weekly reflection reminder. Nothing else is ever posted. |
| `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE` | Required by Android's WorkManager library, which schedules that one weekly reminder so it survives a reboot. Despite its name, `ACCESS_NETWORK_STATE` only lets the library read whether a network exists; it grants no network access. |

## Backups you create yourself

Milestone can export your data as a single file. This only ever happens when you tap
"Export a backup". The app then hands that file to Android's share sheet, and you
choose where it goes — a cloud drive, a messaging app, a folder on your phone.

Once you hand the file to another app, that app's own privacy policy governs it.
Milestone has no involvement in, and no knowledge of, where it ends up. The backup
file is plain, unencrypted JSON, so treat it as you would any personal document.

## Children

Milestone is not directed at children and collects no data from anyone, including
children.

## Third parties

Milestone contains no advertising, analytics, attribution, crash-reporting or
tracking libraries of any kind. No data is shared with anyone, because none is
collected.

## Changes to this policy

If a future version of the app changes what it does with your data, this policy will
be updated before that version is published, and the "last updated" date above will
change.

## Contact

Questions about this policy: {{CONTACT_EMAIL}}
