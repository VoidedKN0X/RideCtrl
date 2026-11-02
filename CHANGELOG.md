# Changelog

What changed in each RideCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/RideCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.2.2 (2026-11-02)

A security and safety update for the widget and for restoring backups, plus a new security policy and issue forms on GitHub.

### Security

- **The widget hides your data while the app lock is on.** Before, the home-screen widget kept showing the main vehicle's name, odometer and next maintenance item even with the app lock on. Now it only says "Locked" until you turn the lock off, and its buttons open RideCtrl's lock screen first. It updates as soon as you turn the lock on or off.
- **Reminders don't say what's due while the app lock is on.** With the lock on, a reminder in the notification shade now only says "RideCtrl reminder" and "Something is due" (the lock screen already hid the details). Tap it and unlock to see which item or document it is.
- **Safer restore from a backup file.** RideCtrl now refuses a backup whose contents unpack to far more than the file itself (a file built to fill up your phone's memory), and a backup with duplicate entries or odd photo names that RideCtrl never writes. Nothing on the phone changes when a file is refused. Backups made by RideCtrl restore exactly as before.

### Fixed

- **A huge km interval no longer closes the app.** A service item with an enormous km interval (for example a typo with many extra zeros) made the expected due date run off the end of the calendar, which crashed the Garage, the vehicle dashboard, the widget and the reminder check. The expected date is now capped at 100 years ahead.

### Project

- New [SECURITY.md](SECURITY.md): how to report a security problem privately, and the release key's fingerprint.
- New issue forms on GitHub for bug reports and feature requests, with a private link for security problems.
- README: a beginner-friendly install step (which file to tap on the Releases page), a new "Is it safe to install?" section with the signing fingerprint and every permission explained, "How RideCtrl is made", the full Ctrl series list, and the "Coming soon" screenshots section removed.

## v0.2.1 (2026-11-02)

A fix for bottom sheets.

### Fixed

- **Bottom sheets no longer show a stray grey box/outline.** Every bottom sheet (the pickers and option sheets) drew its 1dp grey border in the wrong place: a grey rectangle partway down the open sheet, cutting across the rows, and a stray rounded outline near the top of the screen. Material's bottom sheet applies an outside border before it moves the sheet into place, so the border stayed behind. The border is now gone: sheets are the plain surface colour with 26dp rounded top corners and no outline, and the dimmed background already sets them apart. Their colour, shape, drag handle, dimmed background and behaviour are unchanged.

### Unchanged

- Dialogs keep their 1dp border; they don't slide, so theirs was always drawn in the right place.
- Checked everything else that slides or moves (the bottom tab bar's slide in/out and the app lock's PIN shake): their borders sit inside the moving content, so they were already correct.

## v0.2.0 (2026-10-31)

RideCtrl is complete: every log, the premix calculator, reminders, costs, backup, the widget and shortcuts. Everything still stays on your phone, with no internet permission.

### New

- **Garage:** add your vehicles with a photo, name, make, model, year, engine type (2-stroke premix, 2-stroke injection, 4-stroke or electric), plate, VIN/frame number, purchase date and price, odometer and notes. The main vehicle is the big card on top; the others are listed below with a warning when something is due.
- **Vehicle dashboard:** the odometer, the next three maintenance items with their state, consumption, running cost per km, total running cost and the last temperature reading, with quick-add buttons for **Fuel**, **Odometer**, **Service**, **Temp** and **Note**. The VIN stays hidden until you tap it.
- **Fuel log:** date, odometer, amount, price per litre *or* total paid (the other one is worked out), station, full tank or not, premix ratio and oil cost. Consumption (L/100 km and km/L, or mpg with miles) is only measured between two full tanks, so partial fills don't throw it off. Also cost per km, total spent, a consumption chart, and spend per month or per year (tap to switch).
- **Premix calculator** (Tools tab, the fuel log and a launcher shortcut): presets 25:1, 32:1, 40:1 and 50:1 or your own ratio, fuel to oil in ml, and **Reverse** (how much fuel the oil you have is good for). Big numbers for the pump. It starts from the vehicle's own ratio and oil, and can save a new one. When you log a fill on a premix vehicle, the oil to add is shown straight away.
- **Maintenance:** service items with your own interval in km and/or months or days, whichever comes first. New 2-stroke vehicles can start with the scooter list (spark plug, drive belt, variator rollers/sliders, clutch shoes and bell, gear oil, air filter, fuel filter, brakes front and rear, brake fluid, tyres front and rear, carburettor, exhaust decoke, cable lubrication, battery) with **every interval blank**: nothing is presented as a manufacturer figure. Each item shows OK, Soon or Overdue in colour, with km left and the due date.
- **Service records:** date, odometer, items done, parts from your inventory, other parts cost, labour, done by you or a shop, photos and notes. Logging a record resets the next due of every item in it.
- **Parts inventory** (Tools tab): name, brand, part number, where bought, price each, quantity, which vehicles it fits and an optional low-stock warning. Parts used in a service or mod come out of stock automatically, and go back in if you remove them again.
- **Mods log:** what changed, date, odometer, description, inventory parts, other cost, before and after photos, and tuning results.
- **Specs sheet** per vehicle: tyre sizes and pressures, plug type and gap, main and pilot jet, needle clip, roller weights, belt part number, bulbs, gear oil, 2-stroke oil, tank capacity, brake fluid, torque values (N·m) and other. Every field is yours to fill in, each with a "source / notes" box.
- **Temperature log** for your gauge: temperature, date and time, odometer, outside temperature, riding (city, highway, uphill, idling, mixed) and your setup (for example the jetting). A chart over time, and min/avg/max per setup to compare before and after a change. A new reading starts with the last setup.
- **Odometer and trips:** odometer readings (also taken from fuel, service, mod and temperature entries), your average km per day and per month, the distance ridden, and simple trips (date, distance, purpose) without any location tracking. A lower odometer is allowed with a warning, for gauge swaps; ridden distance only counts increases.
- **Documents:** insurance, registration/plate, inspection or anything else, with expiry date, how many days ahead to warn, cost and photos of the papers.
- **Notes** per vehicle.
- **Costs:** total cost of ownership (purchase, fuel, oil, maintenance, parts, mods and documents), running cost, cost per km ridden, and charts per category and per year.
- **Reminders:** a local notification when a service item or document becomes due soon or overdue. Km-based items are estimated from your average km per day (last 90 days). RideCtrl checks once a day with one inexact alarm, so it needs no exact-alarm permission, and it re-arms after a restart. Choose how far ahead to warn (days and km) under **Settings > Reminders**, where a row also opens the battery settings: on OPPO (ColorOS) and similar phones, allow RideCtrl to run in the background or the daily check may not run. On the lock screen a reminder only says "RideCtrl reminder".
- **Backup and export** (Tools tab or the You page): back up everything, including photos, to one file through Android's file picker, or as an **encrypted backup** with a password (AES-256-GCM, key from PBKDF2-HMAC-SHA256 with 600,000 rounds). Restore checks the whole file first and asks before replacing anything. **CSV export** of the fuel log, services, parts and temperature readings for spreadsheets.
- **Home-screen widget:** the main vehicle's odometer and next maintenance due, with an **Add fuel** button.
- **App shortcuts** (long-press the app icon): Add fuel, Premix calculator and Add temp reading.
- **Main vehicle:** choose it on a vehicle's dashboard or under Settings > Reminders; the widget and shortcuts use it.
- **Photos** come from Android's photo picker or your camera app, so RideCtrl needs no camera or storage permission. They're kept in RideCtrl's private storage.
- Every number field takes a comma or a period as the decimal separator.

### Changed

- The Garage and Tools tabs replace the placeholders from 0.1.0.
- Settings has a new **Reminders** section.
- The README describes the complete app, with getting started, reminders on OPPO (ColorOS) and backup sections, and the CtrlF-Droid install button.

### Security

- New permissions: **notifications** (Android asks before the first reminder) and **run at startup** (only to re-arm the daily reminder check after a restart). Still **no internet permission**, and no camera or storage permission.
- The backup never contains the app lock's PIN or fingerprint keys.

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
- **No Google code at all:** the small Guava library that two AndroidX components bring along is left out, together with the baseline-profile installer that needed it (it only speeds up the first launches after an update and talks to app-store tooling).
- **Telemetry-free:** no analytics, crash reporting or tracking of any kind.
- RideCtrl is left out of Android's cloud backup and phone-to-phone transfer. Only 64-bit ARM phones (arm64-v8a) are supported.
