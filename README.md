# Wakey ⏰

An alarm clock you have to get out of bed to turn off. Inspired by Alarmy.

## What's different from WakeUpGuard (why the screen will actually pop up)

| Problem last time | What Wakey does |
|---|---|
| Alarm screen didn't appear | Uses **3 layers**: a full-screen alarm notification, a direct launch using "Display over other apps", and a watchdog that brings the screen back every 3 seconds until you finish the mission |
| Phone was unlocked, so only a small banner showed | The "Display over other apps" permission lets it take over the screen anyway |
| Android 14 blocks full-screen alarms by default | Built-in **Settings → Alarm reliability** checklist with one-tap fixes |
| Xiaomi/Oppo/Vivo phones kill the app | Checklist shows Autostart and lock-screen pop-up settings for your brand |
| Press Home or Back to escape | Back does nothing. Home or the power button just brings the alarm back |
| Sound stops if the screen closes | Sound lives in a foreground service. Only finishing the mission stops it |

## Features
- Alarmy-style dark UI: scroll-wheel time picker, repeat days, labels, volume, vibration, snooze (limited to 3)
- **📸 Photo mission**: register a spot (bathroom sink, kitchen) and photograph it again to dismiss. Works offline. Match strictness can be Easy, Normal or Hard.
- **🏷️ Barcode mission**: scan a registered barcode (toothpaste, shampoo...) to dismiss. Works offline.
- The volume drops while you're doing the mission. If you stop for 60 s it goes back to full blast.
- Safety valve: after 3 minutes of ringing, "Can't do the mission?" lets you type a long sentence instead, in case you lost the barcode.
- **Test button**: rings in 10 seconds so you can lock your phone and check it works.

## Build the APK (no Android Studio needed)
1. Create a new repo on github.com. Public is fine.
2. Upload everything in this folder, **including the hidden `.github` folder**.
   - If the web uploader skips `.github`: click **Add file → Create new file**, type the name `.github/workflows/build.yml`, and paste in the contents of `ci-build.yml`.
3. Open the **Actions** tab. The "Build APK" job runs automatically and takes about 5 minutes.
4. Open **Releases** (right side of the repo page) on your phone, tap `Wakey.apk`, and install it. Allow "Install unknown apps" if asked.
5. Open Wakey, go to **Settings**, and make everything green. Then tap **Ring a test alarm in 10 s** and lock your phone.

Every push builds a new release. New versions install as updates because the signing key is fixed.

## Project layout
```
app/src/main/java/com/aditya/wakey/
  alarm/    AlarmScheduler (setAlarmClock), Receivers (fire + boot), RingService (sound, vibration, watchdog)
  ring/     RingActivity (lock-screen takeover) + RingScreens (alarm face, missions)
  mission/  CameraX + ML Kit barcode, PhotoMatcher (offline image similarity)
  ui/       Home list, editor, wheel picker, settings checklist, mission setup
  data/     Alarm model + JSON store
```
