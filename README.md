<div align="center">

# Upwake

**The alarm clock you can't snooze your way out of.**

Wake-up missions · Strict mode · Morning gratitude · Sleep-focus blocker

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-black)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-black)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-black)
![Status](https://img.shields.io/badge/status-pre--release-FFB547)

</div>

---

## What it does

| Feature | How it works |
|---|---|
| **Wake-up missions** | Turn the alarm off only by photographing a registered spot (e.g. your sink) or scanning a registered barcode (e.g. toothpaste). Matching runs fully on-device. |
| **Writing backup** | Away from home? Copy four paragraphs exactly instead. Progress is kept if you wander off. |
| **Strict mode** | Until the mission is done the alarm covers every app, closes the power menu and notification shade, ignores the volume keys, and resumes after a reboot. |
| **Morning gratitude** | After the alarm, a daily topic and a 25-word reflection, saved to a private journal. |
| **Sleep focus** | Chosen social apps are blocked from 30 min before bedtime until 30 min after wake-up. Their websites are blocked in browsers too. |
| **Upwake Shield** | Local DNS filter that blocks adult websites (Cloudflare Family DNS). Nothing is routed through any Upwake server. |

## Design

Monochrome: jet black, grey-black surfaces, white for primary actions. One signature accent, **Dawn** (`#FFB547`), reserved for "alive" moments: on, ringing, active, complete. Typeface: Inter / Inter Display.

## Tech

- Kotlin 2.2, Jetpack Compose (Material 3), single-activity UI + dedicated ring activity
- `AlarmManager.setAlarmClock` + foreground service + full-screen intent + overlay guard
- CameraX + ML Kit barcode scanning (bundled, offline)
- `VpnService` (DNS-only) and `UsageStatsManager` for sleep focus
- `AccessibilityService` for strict mode (active only during an alarm)
- minSdk 26 · targetSdk 36

## Project structure

```
app/src/main/java/app/upwake/
├── alarm/       scheduling, RingService, boot resume, strict-mode guard
├── ring/        ringing flow, missions, writing backup
├── mission/     camera, barcode, photo matching
├── focus/       sleep-focus blocker, Upwake Shield (DNS)
├── gratitude/   morning check-in and journal
├── data/        alarm model and storage
└── ui/          screens, onboarding, design system (theme/)
```

## Building

Every push to `main` runs GitHub Actions and publishes a signed **APK** (for direct install) and **AAB** (for Google Play) under **Releases**.

Local build (Android Studio Ladybug or newer, JDK 17):

```bash
gradle assembleRelease bundleRelease
```

## Privacy

Upwake has no accounts, servers or analytics. All data stays on the device. See [PRIVACY.md](PRIVACY.md), [TERMS.md](TERMS.md) and [SECURITY.md](SECURITY.md).

## License

Proprietary. © 2026 Aditya Puri. All rights reserved. See [LICENSE](LICENSE).
Inter typeface © The Inter Project Authors, SIL Open Font License 1.1 ([FONT_LICENSE_Inter.txt](FONT_LICENSE_Inter.txt)).
