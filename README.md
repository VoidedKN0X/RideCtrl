# RideCtrl

**An offline maintenance, fuel and mod log for your scooter, motorbike or car.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/RideCtrl?label=download)](https://github.com/VoidedKN0X/RideCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) ![Offline](https://img.shields.io/badge/internet-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

RideCtrl keeps track of everything about your vehicles: fuel and consumption, premix, services and when the next one is due, parts, mods, temperature readings and documents. It's made first for 2-stroke scooters, but works for any vehicle, and it never goes online.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/RideCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a>

> **Early development.** RideCtrl 0.1 is the app's foundation: look, app lock, privacy switches and unit settings. Vehicles and logs arrive over the next releases (see [Roadmap](#roadmap)).

## Features

- **Works fully offline.** RideCtrl doesn't have the internet permission and never goes online. Everything stays on your phone
- **Units your way:** km or miles, litres or gallons, °C or °F, and your currency. Everything is stored in metric, so switching never changes your logs
- **App lock** with its own PIN and optional fingerprint unlock
- **Hide in screenshots** and **Hide in recent apps**, both off until you turn them on
- **Ctrl / Signal look**, shared by every Ctrl app: dark and light, following your phone's dark theme setting, high contrast, one violet accent, Space Grotesk and JetBrains Mono; tablet and foldable layout

## Roadmap

- **Garage:** vehicles with photo, make/model/year, engine type, plate, frame number and odometer, each with its own dashboard
- **Fuel log** with consumption between full tanks, cost per km and charts, and a **premix calculator** with big numbers for use at the pump
- **Maintenance:** service items with your own km and time intervals (a 2-stroke scooter template to start from), service records, and an OK / soon / overdue list
- **Reminders** for maintenance (by date, and by km from your riding average) and document expiry, as local notifications
- **Parts inventory, mods log and a specs sheet** per vehicle
- **Temperature log** with charts and min/avg/max per setup, to compare jetting or exhaust changes
- **Costs:** total cost of ownership per vehicle, per category and per year
- **Backup** to one file (optionally encrypted) and CSV export, a home-screen widget and app shortcuts

## Requirements

- Android 13 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- Permissions: none so far. Later releases add notifications only, for reminders. **No internet.**
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google libraries, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. Download the latest `.apk` file from the [Releases page](https://github.com/VoidedKN0X/RideCtrl/releases/latest).
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your data and settings are kept.

**Automatic updates:** install [Obtainium](https://github.com/ImranR98/Obtainium), then tap the button above. It adds RideCtrl and installs every new release for you. If the button doesn't open the app, add `https://github.com/VoidedKN0X/RideCtrl` in Obtainium by hand.

## App lock

Turn it on under **Settings > Privacy and security > App lock** (tap your profile picture at the top right, then **Settings**). It's off until you turn it on.

- **Its own app PIN** of at least 6 digits, separate from your phone's screen lock. Very obvious PINs such as 111111 or 123456 are refused.
- **Unlock with fingerprint** (optional) only accepts strong biometrics, checked by the phone's secure hardware. The PIN is still needed after the phone restarts and at least every 72 hours.
- **The PIN is never stored.** It's stretched with 600,000 rounds of PBKDF2 and mixed with a key held in the phone's secure hardware.
- **Wrong PINs:** after 5 tries you wait 30 seconds, doubling up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't skip the wait.
- **Lock again:** as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts.
- **Forgot your PIN?** It can't be recovered. The lock screen offers to delete everything on the phone and turn the lock off.

**Hide in screenshots** and **Hide in recent apps** (both off by default): the first keeps RideCtrl out of screenshots and screen recordings, the second shows a blank card in the recent-apps view while screenshots still work. With the app lock on, the recent-apps view always shows a blank card.

## Privacy

**Telemetry-free.** RideCtrl has no analytics, crash reporting, usage stats or tracking of any kind. It has **no internet permission**, works fully offline and never connects to the internet. Every build checks that the app has no network permission and no Google libraries.

- Everything stays on your phone. RideCtrl is left out of Android's cloud backup and its phone-to-phone transfer.
- The Support, Report a bug, Source code and Licence links on the You page open in your browser, only when you tap them.

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

## Support the project

RideCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). In the app, tap your profile picture: **About** at the bottom of the You page has **Support RideCtrl** and **Report a bug**, which opens the [issue tracker](https://github.com/VoidedKN0X/RideCtrl/issues). Starring the repo helps too.

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same design.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ChatCtrl](https://github.com/VoidedKN0X/ChatCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| **RideCtrl** (this app) | Offline maintenance, fuel and mod log for your vehicles |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |

## License

RideCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

Fonts, bundled in the app (nothing is downloaded): [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), both under the SIL Open Font License 1.1 ([FONT-LICENSE-SpaceGrotesk.txt](FONT-LICENSE-SpaceGrotesk.txt), [FONT-LICENSE-JetBrainsMono.txt](FONT-LICENSE-JetBrainsMono.txt)).
