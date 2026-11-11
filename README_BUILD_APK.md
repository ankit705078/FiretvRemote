# FireControl Remote — Zero-Setup Native Android APK Project

This project packages your **FireControl Remote** into a standalone, shareable **Android `.apk`** that includes:
1. **Embedded Pure-Kotlin ADB (`dev.mobile:dadb:1.2.7`)** — Connects directly to any Fire TV Stick over Wi-Fi (`port 5555`) and generates its own RSA keys internally (**zero Python, zero Termux, and zero PC required**).
2. **Embedded Offline Bluetooth HID (`AndroidBluetoothHidHelper.kt`)** — Uses Android's native `BluetoothHidDevice` API so any phone running the APK can pair directly as a Bluetooth remote when the Fire TV Stick has no Wi-Fi.
3. **Full Interactive UI (`app/src/main/assets/firetv_remote.html`)** — Bound directly to Kotlin via `@JavascriptInterface` (`window.FireControlNative`).

---

## Option A: Build the `.apk` in the Cloud (No Android Studio Install Needed!)
1. Create a free repository on [GitHub](https://github.com/new) and upload the contents of this folder.
2. Click the **Actions** tab in your GitHub repository and select **Build Shareable FireControl APK**.
3. In ~2 minutes, GitHub's cloud servers compile the project and produce **`FireControl-Remote-APK` (`app-debug.apk`)** right at the bottom of the workflow summary page—ready to download and share over WhatsApp, Google Drive, or email!

## Option B: Build Locally in Android Studio
1. Open [Android Studio](https://developer.android.com/studio) and select **Open** -> choose this `FireControl_Android_APK_Project` folder.
2. In the top menu, click **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
3. Share the generated `app/build/outputs/apk/debug/app-debug.apk` file with anyone!
