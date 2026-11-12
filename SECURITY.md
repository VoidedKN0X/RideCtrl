# Security policy

Thank you for helping keep RideCtrl and its users safe.

## Reporting a problem

**Please don't report security problems in a public issue.** Report them privately instead:

1. Go to the [Security tab](https://github.com/VoidedKN0X/RideCtrl/security) of this repo.
2. Click **Report a vulnerability** and describe what you found, how to reproduce it, and which version of RideCtrl you used.

Only the maintainer can see the report. I'll reply as soon as I can, work on a fix, and credit you in the changelog if you'd like.

## Supported versions

Only the latest release gets security fixes. Please update before reporting, and check the [changelog](CHANGELOG.md) to see whether it's already fixed.

## What counts

RideCtrl has no server and no internet permission: everything stays on your phone. So these count:

- Any way for RideCtrl to send your data off the phone, or for your logs, photos or documents to end up somewhere you didn't choose
- Ways around the app lock (including the widget, launcher shortcuts and reminders), or protected screens showing up in screenshots when they shouldn't
- Another app on the phone being able to read RideCtrl's data or make RideCtrl do something you didn't ask for
- A backup file that can make RideCtrl write files outside its own folders, crash it, or change your data without asking you first
- Weaknesses in the encrypted backup or the app lock PIN
- Anything that lets someone run code or read files they shouldn't

## How releases are protected

Every release is built from this repo by GitHub Actions and signed with the same release key. Its SHA-256 certificate fingerprint is:

```
D5:45:BF:B0:15:26:B1:B6:46:E5:64:97:FC:11:5D:42:EA:90:D0:9C:11:86:D6:C2:69:5A:96:0B:1C:45:73:B8
```

The [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this key.
