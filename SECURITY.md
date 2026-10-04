# Security Policy

## Reporting a vulnerability

Please email **adityapuri2001@gmail.com** with the subject "Upwake security". Include the app version (Settings → About), your Android version and phone model, and steps to reproduce. Do not open a public issue.

You'll get an acknowledgement within 7 days. Please give us a reasonable time to ship a fix before disclosing anything publicly.

## Supported versions

Only the latest release on Google Play (and the latest `build-N` GitHub release) receives fixes.

## How Upwake is hardened

- **No server, no accounts, no analytics.** All user data stays in the app's private storage.
- **No backups or device transfer** of app data (`allowBackup=false`, data-extraction rules exclude everything).
- **HTTPS only.** Cleartext traffic is disabled and only system certificate authorities are trusted.
- **Minimal exported surface.** Only the launcher activity, the boot receiver (accepts system broadcasts only) and the accessibility and VPN services (bound only by the system through `BIND_ACCESSIBILITY_SERVICE` / `BIND_VPN_SERVICE`) are exported.
- **Immutable PendingIntents** everywhere.
- **Accessibility service** cannot read window content (`canRetrieveWindowContent=false`) and acts only while an alarm is in progress.
- **Release builds** are minified and obfuscated with R8; debug, verbose and info logging is stripped.
- **Upload key** is generated inside GitHub Actions and stored only in this private repository; it is never sent anywhere else. Google Play App Signing holds the real app-signing key, so the upload key can be reset from Play Console if it is ever exposed. Keep this repository private.
- **Dependencies** come only from Google Maven and Maven Central, pinned to exact versions.
