# RideCtrl

**An offline maintenance, fuel and mod log for your scooter, motorbike or car.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/RideCtrl?label=download)](https://github.com/VoidedKN0X/RideCtrl/releases/latest) ![Android 15+](https://img.shields.io/badge/Android-15%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) ![Offline](https://img.shields.io/badge/internet-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

RideCtrl keeps track of everything about your vehicles: fuel and consumption, premix, services and when the next one is due, parts, mods, temperature readings and documents. It's made first for 2-stroke scooters, but works for any vehicle, and it never goes online.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/RideCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Works fully offline.** RideCtrl doesn't have the internet permission and never goes online. Everything stays on your phone
- **Garage** with every vehicle (photo, make/model/year, engine type, plate, hidden VIN/frame number, purchase), each with its own **dashboard**: odometer, next maintenance, consumption, cost per km, last temperature, and quick-add for fuel, odometer, service, temperature and notes
- **Fuel log** with consumption measured only between full tanks (L/100 km and km/L, or mpg), best and worst full-to-full, cost per km, monthly and yearly spend, and charts- **Fuel economy charts** over time with the average as a dashed line, per vehicle and for all your vehicles side by side, drawn by the app itself (no chart library)
- **Premix calculator** made for the pump: 25:1, 32:1, 40:1, 50:1 or your own ratio, fuel to oil in ml and the reverse, remembering each vehicle's ratio and oil
- **Maintenance** with your own intervals by km and/or time (a 2-stroke scooter list to start from, every interval blank, so nothing is passed off as a manufacturer figure), service records with parts, costs, who did it and photos, and an OK / soon / overdue list
- **Reminders** as local notifications for maintenance (by date, and by km: as soon as a logged odometer value reaches the limit, and ahead of time from your average riding) and document expiry
- **Parts inventory** shared by every vehicle, with stock that follows the parts you use and a low-stock warning
- **Mods log** with before and after photos and tuning results, and a **specs sheet** per vehicle where every value is yours, with its source
- **Temperature log** for your gauge, with conditions and setup, a chart, and min/avg/max per setup to compare jetting or exhaust changes
- **Odometer history and trips** (no GPS), with gauge swaps handled
- **Documents** (insurance, registration, inspection…) with expiry dates and photos
- **Costs:** total cost of ownership per vehicle, per category and per year, and cost per km
- **Backup** to one file with every photo, optionally **encrypted** with a password; restore from it; **CSV export** for spreadsheets
- **Home-screen widget** (odometer, next maintenance, Add fuel) and **app shortcuts** (Add fuel, Premix calculator, Add temp reading)
- **Units your way:** km or miles, litres or gallons, °C or °F, and your currency, stored in metric so switching never changes your logs. Number fields take a comma or a period
- **App lock** with its own PIN and optional fingerprint unlock, and **Hide in screenshots** / **Hide in recent apps** (both off until you turn them on)
- **Ctrl / Signal look**, shared by every Ctrl app: dark and light, following your phone's dark theme setting, high contrast, one violet accent, Space Grotesk and JetBrains Mono; tablet and foldable layout

## Requirements

- Android 15 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- Permissions: notifications (for reminders), run at startup (to re-arm the daily reminder check after a restart) and biometrics (only for the optional fingerprint unlock). **No internet**, no camera and no storage permission: photos come through Android's photo picker and your camera app
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google libraries, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. Open the [latest release](https://github.com/VoidedKN0X/RideCtrl/releases/latest) on your phone, scroll down to **Assets** and tap the file that ends in `.apk` (for example `RideCtrl-v0.2.2.apk`). The "Source code" zip and tar.gz files are not the app; you don't need them.
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your data and settings are kept.

**Automatic updates:** on your phone, tap one of the buttons above.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds RideCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/RideCtrl` in Obtainium by hand.

## Getting started

1. On the **Garage** tab, tap **+** and add your vehicle. For a 2-stroke, keep **Start with the scooter service list** on, then fill in your own intervals under **Maintenance**.
2. Log fills with **Fuel** on the dashboard. Fill up to the top and mark **Filled up**: consumption shows from the second full fill.
3. Turn on reminders under **Settings > Reminders** (tap your profile picture, then **Settings**) and allow notifications.

Settings, Backup and About are behind your profile picture at the top right of the Garage and Tools tabs.

## Is it safe to install?

- **Open source.** All of RideCtrl's code is in this repo, under the GPL-3.0 licence. Anyone can read it.
- **Built by GitHub, not on a personal computer.** Every release on the Releases page is built by [the build workflow](.github/workflows/build.yml) on GitHub Actions, straight from the code in this repo. Each build also checks that the app has no network permission and no Google libraries.
- **Always signed with the same key.** Android refuses to install an update signed with a different key over RideCtrl, and the CtrlF-Droid repo only accepts APKs signed with this key. The SHA-256 fingerprint of RideCtrl's signing certificate is:

  ```
  D5:45:BF:B0:15:26:B1:B6:46:E5:64:97:FC:11:5D:42:EA:90:D0:9C:11:86:D6:C2:69:5A:96:0B:1C:45:73:B8
  ```

  Advanced users can check it with [AppVerifier](https://github.com/soupslurpr/AppVerifier).

**Check a download.** Every release is built and published by GitHub Actions, never on a personal computer:

- The release notes end with the APK's **SHA-256** checksum. Compare it with `sha256sum RideCtrl-vX.Y.Z.apk` (or `Get-FileHash` on Windows).
- Each release APK has a signed **build provenance attestation**. With the [GitHub CLI](https://cli.github.com/), `gh attestation verify RideCtrl-vX.Y.Z.apk -R VoidedKN0X/RideCtrl` proves it was built by this repo's workflow from the tagged commit.
- The signing-certificate fingerprint above stays the same for every release.

**Permissions.** RideCtrl has **no internet permission**: it can't go online at all. It asks for only these:

| Permission | What RideCtrl uses it for |
| --- | --- |
| Notifications | Reminders for maintenance that's due and documents about to expire. Android asks you first. |
| Run at startup | Sets the once-a-day reminder check again after your phone restarts. |
| Use biometric hardware | Optional fingerprint unlock for the app lock. |
| App-internal broadcasts (`com.vknxworks.ridectrl.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) | Added by AndroidX so that only RideCtrl itself can send to its own internal receivers. It isn't something you grant and gives no access to anything. |
| Hide other apps' floating windows over the PIN screen | While the lock or PIN screen, a backup password, or a delete, restore or reset confirmation is showing, other apps' floating windows (chat heads, overlays) are hidden over it, so nothing can cover or imitate it. No prompt; overlays work as usual everywhere else. |

No camera, storage, location or contacts permission: photos come through Android's photo picker or your own camera app.

See [Privacy](#privacy) for what happens to your data.

## Reminders on OPPO (ColorOS) and similar phones

RideCtrl checks once a day for maintenance and documents that are due, with a single inexact alarm (no exact-alarm permission). Phones that stop background apps aggressively can block that check. Under **Settings > Reminders**, tap **Allow background activity** and set RideCtrl's battery use to **Unrestricted** (on ColorOS: *Battery > Allow background activity*, and allow auto-launch if your phone asks). Km-based reminders are estimated from your average km per day over the last 90 days.

## Backup and moving to a new phone

Tap your profile picture, then **Backup and export** (also on the Tools tab):

- **Back up to a file** saves every vehicle, log and photo plus your units in one `.zip`, wherever you choose with Android's file picker. Sync it to your own server (with Syncthing, for example) if you like.
- **Encrypted backup** does the same with a password: AES-256-GCM, with the key made from your password with PBKDF2-HMAC-SHA256 (600,000 rounds). Recommended, since backups hold frame numbers, plates and photos of your documents. The password can't be recovered.
- **Restore from a file** checks the whole file first and asks before it **replaces** everything on the phone. The app lock stays as it is.
- **CSV export** writes the fuel log, services, parts or temperature readings for spreadsheets.

## App lock

Turn it on under **Settings > Privacy and security > App lock** (tap your profile picture at the top right, then **Settings**). It's off until you turn it on.

- **Its own app PIN** of at least 6 digits, separate from your phone's screen lock. Very obvious PINs such as 111111 or 123456 are refused.
- **Unlock with fingerprint** (optional) only accepts strong biometrics, checked by the phone's secure hardware. The PIN is still needed after the phone restarts and at least every 72 hours.
- **The PIN is never stored.** It's stretched with 600,000 rounds of PBKDF2 and mixed with a key held in the phone's secure hardware.
- **Wrong PINs:** after 5 tries you wait 30 seconds, doubling up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't skip the wait.
- **Lock again:** as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts.
- **Forgot your PIN?** It can't be recovered. The lock screen offers to delete everything on the phone and turn the lock off; restore from a backup file afterwards.

**Hide in screenshots** and **Hide in recent apps** (both off by default): the first keeps RideCtrl out of screenshots and screen recordings, the second shows a blank card in the recent-apps view while screenshots still work. With the app lock on, the recent-apps view always shows a blank card.

## Privacy

**Telemetry-free.** RideCtrl has no analytics, crash reporting, usage stats or tracking of any kind. It has **no internet permission**, works fully offline and never connects to the internet. Every build checks that the app has no network permission and no Google libraries.

- Everything stays on your phone. RideCtrl is left out of Android's cloud backup and its phone-to-phone transfer, so nothing leaves the phone unless you make a backup or CSV file and choose where it goes.
- Reminders are local notifications; on the lock screen they only say "RideCtrl reminder".
- With the app lock on, the home-screen widget only says "Locked", reminders don't say what's due, and shortcuts, the widget and reminders open the lock screen first.
- The Support, Report a bug, Source code and Licence links on the You page open in your browser, only when you tap them.

## How RideCtrl is made

RideCtrl is a one-person project by VoidedKN0X (VKNXWorks). Much of the code is written with the help of AI (Claude, by Anthropic). I decide what the app does, review the changes and use the app myself. Every change is listed in the [changelog](CHANGELOG.md), and you can read every line of code here.

## Build from source

You need [Android Studio](https://developer.android.com/studio), or JDK 17 and the Android SDK.

1. Clone the repo:
   ```bash
   git clone https://github.com/VoidedKN0X/RideCtrl.git
   cd RideCtrl
   ```
2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
   On Windows, use `gradlew.bat assembleDebug`.
3. The APK is in `app/build/outputs/apk/debug/`. Copy it to your phone and install it.

Or open the folder in Android Studio and press **Run** with your phone connected.

**Dependency verification.** Every library and build plugin download is checked against the sha256 checksums in `gradle/verification-metadata.xml`, and the build fails on any mismatch. When you change a dependency, a plugin or the Gradle version, run the **Dependency verification metadata** workflow (Actions tab, *Run workflow*), check the diff of the file it prints and commit it in the same push.

## Support the project

RideCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). In the app, tap your profile picture: **About** at the bottom of the You page has **Support RideCtrl** and **Report a bug**, which opens the [issue tracker](https://github.com/VoidedKN0X/RideCtrl/issues). Please report bugs and ideas with the [bug report and feature request forms](https://github.com/VoidedKN0X/RideCtrl/issues/new/choose). Starring the repo helps too.

Found a security problem? Please don't open a public issue; see [SECURITY.md](SECURITY.md).

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same design.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CallCtrl](https://github.com/VoidedKN0X/CallCtrl) | Phone app: dialer, call history and in-call screen |
| [CameraCtrl](https://github.com/VoidedKN0X/CameraCtrl) | Private camera with RAW (DNG) on every lens |
| [ChatCtrl](https://github.com/VoidedKN0X/ChatCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [ClockCtrl](https://github.com/VoidedKN0X/ClockCtrl) | Alarms, timers, stopwatch and world clock |
| [ContactCtrl](https://github.com/VoidedKN0X/ContactCtrl) | Contacts: browse, edit, import and export |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [DateCtrl](https://github.com/VoidedKN0X/DateCtrl) | Calendar for the calendars on your phone and your accounts |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [GalleryCtrl](https://github.com/VoidedKN0X/GalleryCtrl) | Private, offline gallery with an encrypted vault |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [LijnCtrl](https://github.com/VoidedKN0X/LijnCtrl) | Live De Lijn bus and tram departures |
| [LumenCtrl](https://github.com/VoidedKN0X/LumenCtrl) | Private, offline photo and video editor |
| [MailCtrl](https://github.com/VoidedKN0X/MailCtrl) | Email for Gmail and Proton Mail |
| [MapsCtrl](https://github.com/VoidedKN0X/MapsCtrl) | Private, offline maps and navigation |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [RecorderCtrl](https://github.com/VoidedKN0X/RecorderCtrl) | Private, offline voice recorder |
| **RideCtrl** (this app) | Offline maintenance, fuel and mod log for your vehicles |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [ShutterCtrl](https://github.com/VoidedKN0X/ShutterCtrl) | Remote control and photo transfer for Canon cameras |
| [SoundCtrl](https://github.com/VoidedKN0X/SoundCtrl) | JBL headphones and speakers, without the account |
| [SumCtrl](https://github.com/VoidedKN0X/SumCtrl) | Offline calculator, unit converter and work tools |
| [SynapseCtrl](https://github.com/VoidedKN0X/SynapseCtrl) | Administer your Matrix Synapse server |
| [TalkCtrl](https://github.com/VoidedKN0X/TalkCtrl) | Cardo intercom settings, radio and updates |
| [TapoCtrl](https://github.com/VoidedKN0X/TapoCtrl) | Tapo plugs and bulbs on your home network |
| [TextCtrl](https://github.com/VoidedKN0X/TextCtrl) | Private SMS and MMS messaging |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |
| [WakeCtrl](https://github.com/VoidedKN0X/WakeCtrl) | Keep the screen awake, per app or while charging |
| [WrtCtrl](https://github.com/VoidedKN0X/WrtCtrl) | Manage your OpenWrt router |

## License

RideCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

Fonts, bundled in the app (nothing is downloaded): [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), both under the SIL Open Font License 1.1 ([FONT-LICENSE-SpaceGrotesk.txt](FONT-LICENSE-SpaceGrotesk.txt), [FONT-LICENSE-JetBrainsMono.txt](FONT-LICENSE-JetBrainsMono.txt)).
