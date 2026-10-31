# Changelog

What changed in each RideCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/RideCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.1.0 (2026-10-31)

The first RideCtrl release: the app's foundation. Vehicles and logs arrive in the next releases.

### New

- **Garage and Tools tabs**, with the shared Ctrl bottom bar (a side rail on tablets and unfolded foldables). Both show what's coming until vehicles and the premix calculator land.
- **Profile page and Settings**: tap the profile picture at the top right to open the You page, then **Settings**. There is no settings tab or gear icon, as in every Ctrl app.
- **Units and currency** under **Settings > Units**: distance in kilometres or miles, fuel in litres, US gallons or imperial gallons, temperature in °C or °F, and the currency used for every amount (euro by default). RideCtrl stores everything in metric, so switching units never changes your logs.
- **App lock**, the same as in every Ctrl app: its own app PIN of at least 6 digits, optional fingerprint unlock (strong biometrics only), a waiting time after 5 wrong PINs, and a choice of when it locks again. Forgetting the PIN deletes everything on the phone and turns the lock off.
- **Hide in screenshots** and **Hide in recent apps**: two separate switches under **Settings > Privacy and security**, both off by default.
- **Ctrl / Signal look**: dark and light, following your phone's dark theme setting, with the violet accent and bundled Space Grotesk and JetBrains Mono fonts. New gauge app icon, with a themed version for Android's themed icons.

### Security

- **Fully offline:** RideCtrl has no internet permission, and every build now checks the APK has no network permission and that no Google library is included.
- **Telemetry-free:** no analytics, crash reporting or tracking of any kind.
- RideCtrl is left out of Android's cloud backup and phone-to-phone transfer. Only 64-bit ARM phones (arm64-v8a) are supported.
