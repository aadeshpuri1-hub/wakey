# Changelog

## 1.1.0 (unreleased)

### Added
- Stopwatch with laps (best lap green, slowest red)
- World Clock with city search
- Apple Clock style: iOS colors, green switches, round Start / Stop buttons
- Steps mission (motion sensor)
- Squat and push-up missions with on-device AI form coaching (ML Kit pose detection): only full, clean reps count
- Alarm background: Sunrise, Night, Ocean, Forest, Black or your own photo
- Delete gratitude journal entries
- Alarmy-style consent screen and a cleaner Terms / Privacy reader
- New logo: an alarm-clock sun on the horizon

### Changed
- Tabs are now Alarm, Focus, Stopwatch, World Clock; Settings moved to the top-left gear and made compact
- Sleep-focus times can be changed at any time; during an active session the change starts after it ends

## 1.0.0 (unreleased)

First market-ready release (formerly the "Wakey" prototype).

### Added
- Onboarding with a permission wizard and the strict-mode disclosure
- About screen with an in-app privacy policy
- Dawn amber accent, new sunrise app icon, themed (monochrome) icon
- Boot resume through an exact alarm (Android 15+ compliant)
- Signed release APK + App Bundle in CI
- Terms of Use and Privacy Policy, accepted on first launch (re-asked when they change)
- Security hardening: no backups/device transfer, HTTPS-only network config, boot receiver accepts system broadcasts only, R8 obfuscation, release log stripping
- SECURITY.md vulnerability reporting policy

### Changed
- Rebranded to Upwake (`app.upwake`)
- Target Android 16 (API 36), updated AndroidX, Compose, CameraX and Kotlin
- Strict mode now has an Accessibility description and Play-compliant disclosure
