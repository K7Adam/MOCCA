---
name: testing-mocca-android-ui
description: Test MOCCA's Compose UI/theme end-to-end on a Devin Linux Android emulator. Use when verifying theme, onboarding, or any Android runtime UI change (e.g. the M3 Expressive "Mocha" theme).
---

# Testing MOCCA Android UI on a Linux Emulator

The repo's `mocca-android-agent-workflow` skill is Windows/PowerShell-oriented. On a Linux VM (such as Devin's) use the flow below instead.

## Prerequisites / environment
- Android SDK is at `$HOME/android-sdk` (installed by the blueprint). Export:
  `export ANDROID_HOME=$HOME/android-sdk; export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator`
- AVDs available: `FlayerPhone` (phone), `FlayerTV`. Use `FlayerPhone` for UI.
- No secrets required — the onboarding/theme is reachable offline (no MOCCA CLI bridge / server needed).

## KVM gotcha (root cause of a prior crashed test session)
The emulator needs `/dev/kvm`. If it dies instantly with `This user doesn't have permissions to use KVM (/dev/kvm)` / `x86_64 emulation currently requires hardware acceleration`, the `ubuntu` user isn't in the `kvm` group. Fix:
```
sudo gpasswd -a ubuntu kvm && sudo chmod 666 /dev/kvm
```
(The blueprint should add `ubuntu` to the `kvm` group so future snapshots boot the emulator directly. `chmod 666` is a per-boot top-up since the device node is recreated at boot.)

## Build + install + launch
```
/home/ubuntu/repos/MOCCA/gradlew :androidApp:assembleDebug --no-daemon          # ~3-4 min; APK at /home/ubuntu/repos/MOCCA/androidApp/build/outputs/apk/debug/androidApp-debug.apk
emulator -avd FlayerPhone -no-snapshot -no-audio -gpu swiftshader_indirect -no-boot-anim &   # cold boot avoids stale-snapshot deaths
adb wait-for-device && (until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 3; done)
adb install -r /home/ubuntu/repos/MOCCA/androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell pm grant com.mocca.app android.permission.POST_NOTIFICATIONS   # pre-dismiss the notif permission dialog
adb shell monkey -p com.mocca.app -c android.intent.category.LAUNCHER 1
```
- Package: `com.mocca.app`, launcher `com.mocca.app/.MainActivity`.
- Start screen with no configured server/bridge = `ProgressiveOnboardingScreen` (Welcome → Connect → Connecting).

## Verifying the theme objectively (don't eyeball colors)
The app forces **dark** theme (`ui/App.kt`: `darkTheme = true`), so `MochaDarkColorScheme` (`ui/theme/AppTheme.kt`) applies. Sample real pixels from `adb` screenshots and compare to the scheme constants:
```
adb exec-out screencap -p > shot.png
python3 -c "from PIL import Image; im=Image.open('shot.png').convert('RGB'); print(im.getpixel((540,2050)))"
```
Key expected Mocha-dark values (device 1080x2400): `primary=#FFB784` (warm tan, R−B>0), `background/surface=#19120D`. The OLD theme's primary was blue `#AFC2FF` (B>R) — a warm-vs-blue pixel check cleanly distinguishes working vs reverted/broken theme.

## Desktop window for recording
Emulator warning dialogs ("Nested Virtualization", "Compatibility Warnings") pop up over the window — click OK to dismiss. Maximize with:
`export DISPLAY=:0; wmctrl -i -a <emulator_win_id>; wmctrl -i -r <emulator_win_id> -b add,maximized_vert,maximized_horz`
(`wmctrl -l` lists windows; the app window is titled `Android Emulator - FlayerPhone:5554`.)

## Alive/crash check after navigation
```
adb shell dumpsys activity activities | grep -iE "topResumedActivity|ResumedActivity"
```
Should show `com.mocca.app/.MainActivity`. A composable-context regression (the theme refactor made `AppColors` `@Composable get()`) would crash on launch or on the first navigation, so always navigate at least one screen (tap GET STARTED) as part of the test.

## Devin Secrets Needed
- None for onboarding/theme testing. (Testing an authenticated chat flow would require a running MOCCA CLI bridge / OpenCode server to pair with, which is out of scope for offline UI checks.)
